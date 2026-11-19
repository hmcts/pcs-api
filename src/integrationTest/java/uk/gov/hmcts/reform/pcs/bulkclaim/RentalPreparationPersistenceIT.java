package uk.gov.hmcts.reform.pcs.bulkclaim;

import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
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

    @BeforeEach
    void mockReferenceData() {
        when(eligibilityService.checkEligibility(any(), any())).thenReturn(
            EligibilityResult.builder().status(EligibilityStatus.ELIGIBLE).epimsId(20262)
                .legislativeCountry(LegislativeCountry.ENGLAND).build());
        CourtVenue venue = mock(CourtVenue.class);
        when(venue.regionId()).thenReturn("1");
        when(locationReferenceService.getCourtVenues(any())).thenReturn(List.of(venue));
        when(organisationResolver.isPbaAccountValid(any(), any())).thenReturn(true);
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
