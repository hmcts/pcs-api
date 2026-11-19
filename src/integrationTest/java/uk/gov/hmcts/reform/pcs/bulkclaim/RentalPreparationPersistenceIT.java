package uk.gov.hmcts.reform.pcs.bulkclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Prepared;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.PreparedBulkClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental.DefaultRentalBulkClaimPreparer;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimXmlCodec;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class RentalPreparationPersistenceIT extends AbstractPostgresContainerIT {

    private static final AtomicLong CASE_REFERENCES = new AtomicLong(1781000000010000L);

    @Autowired
    private DefaultRentalBulkClaimPreparer preparer;
    @Autowired
    private BulkClaimXmlCodec codec;
    @Autowired
    private PcsCaseService pcsCaseService;
    @MockitoBean
    private EligibilityService eligibilityService;
    @MockitoBean
    private LocationReferenceService locationReferenceService;
    @MockitoBean
    private BulkClaimantOrganisationResolver organisationResolver;
    @MockitoBean
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void mockReferenceData() {
        when(eligibilityService.checkEligibility(any(), any())).thenReturn(
            EligibilityResult.builder().status(EligibilityStatus.ELIGIBLE).epimsId(20262)
                .legislativeCountry(LegislativeCountry.ENGLAND).build());
        CourtVenue venue = mock(CourtVenue.class);
        when(venue.regionId()).thenReturn("1");
        when(locationReferenceService.getCourtVenues(any())).thenReturn(List.of(venue));
        when(organisationResolver.isPbaAccountValid(any(), any())).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)).thenReturn(true);
    }

    static Stream<String> validFixtures() {
        return Stream.of(
            "rental-england-assured-arrears-valid.xml",
            "rental-england-assured-no-arrears.xml",
            "rental-england-secure.xml",
            "rental-england-flexible.xml",
            "rental-england-introductory.xml",
            "rental-england-demoted-no-grounds.xml",
            "rental-england-other-tenancy.xml"
        );
    }

    @ParameterizedTest
    @MethodSource("validFixtures")
    void preparedClaimSatisfiesPersistence(String fixture) {
        OrganisationDetailsResponse organisation = organisation();
        RentalClaimSet claimSet = codec.parseRental(read(fixture));
        BulkClaimPreparationResult result = preparer.prepare(claimSet, claimSet.getRentalClaim().getFirst(),
                                                             organisation);
        assertThat(result).as(result.toString()).isInstanceOf(Prepared.class);
        PreparedBulkClaim prepared = ((Prepared) result).claim();

        long caseReference = CASE_REFERENCES.incrementAndGet();
        pcsCaseService.createCase(caseReference, prepared.pcsCase().getPropertyAddress(),
                                  prepared.pcsCase().getLegislativeCountry(), organisation);
        pcsCaseService.createMainClaimOnCase(caseReference, prepared.pcsCase(), organisation);

        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = caseEntity.getClaims().getFirst();

        assertThat(caseEntity.getTenancyLicence()).isNotNull();
        assertThat(caseEntity.getTenancyLicence().getHasCopyOfTenancyLicence())
            .isEqualTo(VerticalYesNo.NO);
        assertThat(caseEntity.getRegionId()).isEqualTo(1);
        assertThat(claim.getClaimGrounds()).isNotEmpty();
        assertThat(claim.getNoticeOfPossession().getNoticeServed()).isNotNull();
        assertThat(claim.getRentArrears() != null).isEqualTo(prepared.pcsCase().getRentArrears().getTotal() != null);
        assertThat(claim.getStatementOfTruth().getAccepted()).isEqualTo(YesOrNo.YES);
        assertThat(claim.getLanguageUsed()).isEqualTo(prepared.pcsCase().getLanguageUsed());
        assertThat(claim.getClaimParties().stream().filter(p -> p.getRole() == PartyRole.CLAIMANT)).hasSize(1);
        assertThat(claim.getClaimParties().stream().filter(p -> p.getRole() == PartyRole.DEFENDANT))
            .hasSize(1 + (prepared.pcsCase().getAdditionalDefendants() == null
                ? 0 : prepared.pcsCase().getAdditionalDefendants().size()));
    }

    @ParameterizedTest
    @MethodSource("validFixtures")
    void persistsTheClaimantAndNoticeDetails(String fixture) {
        OrganisationDetailsResponse organisation = organisation();
        RentalClaimSet claimSet = codec.parseRental(read(fixture));
        PreparedBulkClaim prepared = ((Prepared) preparer.prepare(
            claimSet, claimSet.getRentalClaim().getFirst(), organisation)).claim();
        long caseReference = CASE_REFERENCES.incrementAndGet();
        pcsCaseService.createCase(caseReference, prepared.pcsCase().getPropertyAddress(),
                                  prepared.pcsCase().getLegislativeCountry(), organisation);
        pcsCaseService.createMainClaimOnCase(caseReference, prepared.pcsCase(), organisation);

        ClaimEntity claim = pcsCaseService.loadCase(caseReference).getClaims().getFirst();

        assertThat(claim.getNoticeOfPossession().getNoticeServed()).isEqualTo(prepared.pcsCase().getNoticeServed());
        assertThat(claim.getClaimParties().stream()
                       .filter(p -> p.getRole() == PartyRole.CLAIMANT)
                       .findFirst().orElseThrow().getParty().getOrgName())
            .isEqualTo("Example Housing Association");
    }

    static Stream<String> validWalesFixtures() {
        return Stream.of(
            "rental-wales-standard-arrears.xml",
            "rental-wales-secure-asb.xml",
            "rental-wales-estate-management.xml",
            "rental-wales-exempt-landlord.xml",
            "rental-wales-certificates-not-provided.xml"
        );
    }

    private ClaimEntity persistWales(String fixture, PreparedBulkClaim[] preparedOut) {
        when(eligibilityService.checkEligibility(any(), any())).thenReturn(
            EligibilityResult.builder().status(EligibilityStatus.ELIGIBLE).epimsId(20262)
                .legislativeCountry(LegislativeCountry.WALES).build());
        OrganisationDetailsResponse organisation = organisation();
        RentalClaimSet claimSet = codec.parseRental(read(fixture));
        BulkClaimPreparationResult result = preparer.prepare(claimSet, claimSet.getRentalClaim().getFirst(),
                                                             organisation);
        assertThat(result).as(result.toString()).isInstanceOf(Prepared.class);
        PreparedBulkClaim prepared = ((Prepared) result).claim();
        preparedOut[0] = prepared;

        long caseReference = CASE_REFERENCES.incrementAndGet();
        pcsCaseService.createCase(caseReference, prepared.pcsCase().getPropertyAddress(),
                                  prepared.pcsCase().getLegislativeCountry(), organisation);
        pcsCaseService.createMainClaimOnCase(caseReference, prepared.pcsCase(), organisation);
        return pcsCaseService.loadCase(caseReference).getClaims().getFirst();
    }

    private ClaimEntity persistWales(String fixture) {
        return persistWales(fixture, new PreparedBulkClaim[1]);
    }

    private static java.util.Map<String, ClaimGroundEntity> groundsByCode(ClaimEntity claim) {
        return claim.getClaimGrounds().stream()
            .collect(java.util.stream.Collectors.toMap(ClaimGroundEntity::getCode, ground -> ground));
    }

    @ParameterizedTest
    @MethodSource("validWalesFixtures")
    void preparedWalesClaimSatisfiesPersistence(String fixture) {
        PreparedBulkClaim[] prepared = new PreparedBulkClaim[1];
        ClaimEntity claim = persistWales(fixture, prepared);

        assertThat(claim.getPcsCase().getTenancyLicence()).isNotNull();
        assertThat(claim.getPcsCase().getLegislativeCountry()).isEqualTo(LegislativeCountry.WALES);
        assertThat(claim.getClaimGrounds()).isNotEmpty();
        assertThat(claim.getNoticeOfPossession().getNoticeServed()).isNotNull();
        assertThat(claim.getIsExemptLandlord()).isNotNull();
        assertThat(claim.getAsbProhibitedConductEntity()).isNotNull();
        assertThat(claim.getEnergyPerformanceCertificateProvided()).isNotNull();
        assertThat(claim.getGasSafetyReportProvided()).isNotNull();
        assertThat(claim.getElectricalInstallationConditionProvided()).isNotNull();
        assertThat(claim.getStatementOfTruth().getAccepted()).isEqualTo(YesOrNo.YES);
        assertThat(claim.getRentArrears() != null)
            .isEqualTo(prepared[0].pcsCase().getRentArrears().getTotal() != null);
    }

    @Test
    void persistsStandardContractWithRentArrearsGrounds() {
        ClaimEntity claim = persistWales("rental-wales-standard-arrears.xml");

        assertThat(claim.getPcsCase().getTenancyLicence().getType()).isEqualTo(CombinedLicenceType.STANDARD_CONTRACT);
        assertThat(claim.getPcsCase().getTenancyLicence().getStartDate()).isEqualTo(java.time.LocalDate.of(2022, 3, 1));
        assertThat(groundsByCode(claim)).containsOnlyKeys("RENT_ARREARS_S157", "SERIOUS_ARREARS_PERIODIC_S181");
        assertThat(groundsByCode(claim).values()).allSatisfy(ground -> {
            assertThat(ground.getIsRentArrears()).isTrue();
            assertThat(ground.getReason()).isNull();
        });
        assertThat(groundsByCode(claim).get("RENT_ARREARS_S157").getCategory())
            .isEqualTo(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY);
        assertThat(groundsByCode(claim).get("SERIOUS_ARREARS_PERIODIC_S181").getCategory())
            .isEqualTo(ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY);
        assertThat(claim.getNoticeOfPossession().getNoticeServed()).isEqualTo(YesOrNo.YES);
        assertThat(claim.getNoticeOfPossession().getNoticeType()).isEqualTo("RHW20");
        assertThat(claim.getNoticeOfPossession().getServingMethod()).isEqualTo(NoticeServiceMethod.FIRST_CLASS_POST);
        assertThat(claim.getNoticeOfPossession().getNoticeDate()).isEqualTo(java.time.LocalDate.of(2026, 10, 5));
        assertThat(claim.getRentArrears().getTotalRentArrears()).isEqualByComparingTo("2400.50");
        assertThat(claim.getIsExemptLandlord()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getEnergyPerformanceCertificateProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(claim.getAsbProhibitedConductEntity().getClaimingStandardContract()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getAsbProhibitedConductEntity().getAntisocialBehaviour()).isNull();
    }

    @Test
    void persistsSecureContractWithAntisocialBehaviourAndProhibitedConduct() {
        ClaimEntity claim = persistWales("rental-wales-secure-asb.xml");

        assertThat(claim.getPcsCase().getTenancyLicence().getType()).isEqualTo(CombinedLicenceType.SECURE_CONTRACT);
        assertThat(groundsByCode(claim)).containsOnlyKeys("ANTISOCIAL_BEHAVIOUR_S157");
        assertThat(groundsByCode(claim).get("ANTISOCIAL_BEHAVIOUR_S157").getCategory())
            .isEqualTo(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY);
        assertThat(claim.getNoticeOfPossession().getNoticeServed()).isEqualTo(YesOrNo.NO);
        assertThat(claim.getNoticeOfPossession().getNoticeStatement()).startsWith("I am a landlord");

        AsbProhibitedConductEntity conduct = claim.getAsbProhibitedConductEntity();
        assertThat(conduct.getAntisocialBehaviour()).isEqualTo(VerticalYesNo.YES);
        assertThat(conduct.getAntisocialBehaviourDetails()).isEqualTo("Loud parties and threats to neighbours.");
        assertThat(conduct.getIllegalPurposes()).isEqualTo(VerticalYesNo.NO);
        assertThat(conduct.getIllegalPurposesDetails()).isNull();
        assertThat(conduct.getOtherProhibitedConduct()).isEqualTo(VerticalYesNo.YES);
        assertThat(conduct.getOtherProhibitedConductDetails()).isEqualTo("Damage to communal areas.");
        assertThat(conduct.getClaimingStandardContract()).isEqualTo(VerticalYesNo.YES);
        assertThat(conduct.getClaimingStandardContractDetails())
            .isEqualTo("The defendant has repeatedly harassed neighbours.");
        assertThat(conduct.getPeriodicContractAgreed()).isEqualTo(VerticalYesNo.YES);
        assertThat(conduct.getPeriodicContractDetails()).isEqualTo("Pets are not allowed.");
    }

    @Test
    void persistsOtherContractWithEstateManagementGroundsAndReasons() {
        ClaimEntity claim = persistWales("rental-wales-estate-management.xml");

        assertThat(claim.getPcsCase().getTenancyLicence().getType()).isEqualTo(CombinedLicenceType.OTHER);
        assertThat(claim.getPcsCase().getTenancyLicence().getOtherTypeDetails()).isEqualTo("A licence agreement.");
        assertThat(groundsByCode(claim)).containsOnlyKeys(
            "ESTATE_MANAGEMENT_GROUNDS_S160", "OTHER_BREACH_OF_CONTRACT_S157", "LANDLORD_NOTICE_PERIODIC_S178",
            "BUILDING_WORKS", "REDEVELOPMENT_SCHEMES");
        assertThat(groundsByCode(claim).get("BUILDING_WORKS").getCategory())
            .isEqualTo(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT);
        assertThat(groundsByCode(claim).get("BUILDING_WORKS").getReason()).isEqualTo("The block is being redeveloped.");
        assertThat(groundsByCode(claim).get("OTHER_BREACH_OF_CONTRACT_S157").getReason())
            .isEqualTo("Sub-letting without consent.");
        assertThat(groundsByCode(claim).get("LANDLORD_NOTICE_PERIODIC_S178").getReason())
            .isEqualTo("Notice was given and has expired.");
        assertThat(claim.getRentArrears()).isNull();
    }

    @Test
    void persistsExemptLandlordOnASecureContract() {
        ClaimEntity claim = persistWales("rental-wales-exempt-landlord.xml");

        assertThat(claim.getIsExemptLandlord()).isEqualTo(VerticalYesNo.YES);
        assertThat(groundsByCode(claim)).containsOnlyKeys(
            "ESTATE_MANAGEMENT_GROUNDS_S160", "DISABLED_SUITABLE_DWELLING", "FAILURE_TO_GIVE_UP_POSSESSION_S170");
        assertThat(groundsByCode(claim).get("DISABLED_SUITABLE_DWELLING").getCategory())
            .isEqualTo(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT);
        assertThat(claim.getAsbProhibitedConductEntity().getPeriodicContractAgreed()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void persistsCertificatesThatAreNotProvidedWithTheirReasons() {
        ClaimEntity claim = persistWales("rental-wales-certificates-not-provided.xml");

        assertThat(claim.getEnergyPerformanceCertificateProvided()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getNoEnergyPerformanceCertificateReason())
            .isEqualTo("The certificate is held by the previous owner.");
        assertThat(claim.getGasSafetyReportProvided()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getNoGasSafetyReportReason()).isEqualTo("There is no gas supply.");
        assertThat(claim.getElectricalInstallationConditionProvided()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getNoElectricalInstallationConditionReason()).isEqualTo("The inspection is booked.");
    }

    private static OrganisationDetailsResponse organisation() {
        return OrganisationDetailsResponse.builder()
            .name("Example Housing Association")
            .organisationIdentifier("ORG1")
            .organisationProfileIds(List.of("SOLICITOR_PROFILE"))
            .status("ACTIVE")
            .paymentAccount(List.of("PBA1234567"))
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                                            .addressLine1("1 Org Street")
                                            .townCity("London")
                                            .postCode("SW1A 1AA")
                                            .build()))
            .superUser(OrganisationDetailsResponse.SuperUser.builder().email("admin@example.org").build())
            .build();
    }

    private static byte[] read(String fixture) {
        try {
            return Files.readAllBytes(Path.of("src/test/resources/bulkclaim/v2", fixture));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
