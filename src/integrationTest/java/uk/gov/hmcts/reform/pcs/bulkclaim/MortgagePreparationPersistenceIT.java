package uk.gov.hmcts.reform.pcs.bulkclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.mortgage.DefaultMortgageBulkClaimPreparer;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimExtrasService;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimXmlCodec;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageClaimSet;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.SubmissionChannel;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimOccupantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageAdditionalPaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageParticularsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.PostCodeCourtService;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class MortgagePreparationPersistenceIT extends AbstractPostgresContainerIT {

    private static final AtomicLong CASE_REFERENCES = new AtomicLong(1781000000020000L);

    @Autowired
    private DefaultMortgageBulkClaimPreparer preparer;
    @Autowired
    private BulkClaimXmlCodec codec;
    @Autowired
    private PcsCaseService pcsCaseService;
    @Autowired
    private BulkClaimExtrasService extrasService;
    @MockitoBean
    private EligibilityService eligibilityService;
    @MockitoBean
    private LocationReferenceService locationReferenceService;
    @MockitoBean
    private PostCodeCourtService postCodeCourtService;
    @MockitoBean
    private BulkClaimantOrganisationResolver organisationResolver;
    @MockitoBean
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void mockReferenceData() {
        CourtVenue venue = mock(CourtVenue.class);
        when(venue.regionId()).thenReturn("1");
        when(postCodeCourtService.getCourtManagementLocation(any(), any())).thenReturn(20262);
        when(locationReferenceService.getCourtVenues(any())).thenReturn(List.of(venue));
        when(organisationResolver.isPbaAccountValid(any(), any())).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)).thenReturn(true);
    }

    @ParameterizedTest
    @CsvSource({
        "mortgage-england-valid.xml, ENGLAND",
        "mortgage-wales-valid.xml, WALES"
    })
    void preparedMortgageClaimIsPersisted(String fixture, LegislativeCountry country) {
        when(eligibilityService.checkEligibility(any(), any())).thenReturn(
            EligibilityResult.builder().status(EligibilityStatus.ELIGIBLE).epimsId(20262)
                .legislativeCountry(country).build());
        OrganisationDetailsResponse organisation = organisation();
        MortgageClaimSet claimSet = codec.parseMortgage(read(fixture));
        BulkClaimPreparationResult result = preparer.prepare(
            claimSet, claimSet.getMortgageClaim().getFirst(), organisation);
        assertThat(result).as(result.toString()).isInstanceOf(Prepared.class);
        PreparedBulkClaim prepared = ((Prepared) result).claim();

        long caseReference = CASE_REFERENCES.incrementAndGet();
        pcsCaseService.createCase(caseReference, prepared.pcsCase().getPropertyAddress(),
                                  prepared.pcsCase().getLegislativeCountry(), organisation);
        pcsCaseService.createMainClaimOnCase(caseReference, prepared.pcsCase(), organisation);
        extrasService.apply(caseReference, prepared.extras());

        var caseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = caseEntity.getMainClaim();

        assertThat(caseEntity.getLegislativeCountry()).isEqualTo(country);
        assertThat(claim.getClaimantType()).isEqualTo(ClaimantType.MORTGAGE_LENDER);
        assertThat(claim.getClaimantDesignation()).isEqualTo("Commercial");
        assertThat(claim.getSubmissionChannel()).isEqualTo(SubmissionChannel.BULK_API);
        assertThat(claim.getClaimGrounds()).isEmpty();
        assertThat(claim.getNoticeOfPossession()).isNull();
        assertThat(claim.getRentArrears()).isNull();
        assertThat(caseEntity.getTenancyLicence()).isNull();

        MortgageParticularsEntity mortgage = claim.getMortgageParticulars();
        assertThat(mortgage.getMortgageDate()).isEqualTo(LocalDate.of(2015, 6, 1));
        assertThat(mortgage.getDefaultNoticeDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(mortgage.getLoanAmount()).isEqualByComparingTo("200000.00");
        assertThat(mortgage.getCurrentRepaymentTerms()).isEqualTo("Monthly repayments of 1000.00.");
        assertThat(mortgage.getFullRepaymentDate()).isEqualTo(LocalDate.of(2040, 6, 1));
        assertThat(mortgage.getFullRepaymentAmount()).isEqualByComparingTo("180000.00");
        assertThat(mortgage.getCostsAndAdministration()).isEqualByComparingTo("250.00");
        assertThat(mortgage.getOutstandingAmount()).isEqualByComparingTo("175000.00");
        assertThat(mortgage.getStartInterestRate()).isEqualByComparingTo("3.5");
        assertThat(mortgage.getPreArrearsInterestRate()).isEqualByComparingTo("4.25");
        assertThat(mortgage.getClaimStartInterestRate()).isEqualByComparingTo("5");
        assertThat(mortgage.getInterestedParty()).isEqualTo("Example Building Society");
        assertThat(mortgage.getTenancyDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(mortgage.getTenantNoticeServedDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(mortgage.getTotalArrears()).isEqualByComparingTo("1750.00");
        assertThat(mortgage.getRecoverySteps()).isEqualTo("Letters and telephone calls to the borrowers.");
        assertThat(mortgage.getMoneyJudgmentWanted()).isEqualTo(YesOrNo.YES);
        assertThat(mortgage.getOriginalProviderName()).isEqualTo("First Lender Ltd");
        assertThat(mortgage.getOriginalProviderBoughtDate()).isEqualTo(LocalDate.of(2019, 4, 1));
        assertThat(mortgage.getOriginalProviderDefendantNotifiedDate()).isEqualTo(LocalDate.of(2019, 5, 1));

        List<MortgageAdditionalPaymentEntity> payments = mortgage.getAdditionalPayments();
        assertThat(payments).hasSize(2);
        assertThat(payments.getFirst().getAmount()).isEqualByComparingTo("50.00");
        assertThat(payments.getFirst().getReason()).isEqualTo("Insurance premium");
        assertThat(payments.getFirst().getArrears()).isEqualByComparingTo("50.00");
        assertThat(payments.getLast().getArrears()).isNull();

        assertThat(claim.getPaymentHistoryLines()).hasSize(2);
        assertThat(claim.getPaymentHistoryLines().getLast().getCumulativeArrears()).isEqualByComparingTo("1750.00");

        List<ClaimOccupantEntity> occupants = claim.getOccupants();
        assertThat(occupants).hasSize(2);
        assertThat(occupants.getFirst().getFirstName()).isEqualTo("Sam");
        assertThat(occupants.getFirst().getOtherNames()).isEqualTo("Jay");
        assertThat(occupants.getLast().getLastName()).isEqualTo("Lodger");

        List<YesOrNo> defendantOccupantFlags = claim.getClaimParties().stream()
            .filter(party -> party.getRole() == PartyRole.DEFENDANT)
            .sorted(Comparator.comparing(ClaimPartyEntity::getRank))
            .map(party -> party.getParty().getIsOccupant())
            .toList();
        assertThat(defendantOccupantFlags).containsExactly(YesOrNo.NO, YesOrNo.YES);

        assertThat(claim.getStatementOfTruth().getAccepted()).isEqualTo(YesOrNo.YES);
        assertThat(claim.getStatementOfTruth().getPositionHeld()).isEqualTo("Possessions Manager");
        assertThat(claim.getStatementOfTruth().getCompletedDate()).isEqualTo(LocalDate.of(2026, 11, 3).atStartOfDay());
    }

    private static OrganisationDetailsResponse organisation() {
        return OrganisationDetailsResponse.builder()
            .name("Example Bank plc")
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
