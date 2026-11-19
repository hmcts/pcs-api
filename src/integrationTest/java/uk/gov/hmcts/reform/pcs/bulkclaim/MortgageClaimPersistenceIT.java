package uk.gov.hmcts.reform.pcs.bulkclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.CaseViewRequest;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.PCSCaseView;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.SubmissionChannel;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimOccupantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.ClaimPaymentHistoryLineEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.MortgageParticularsEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormPayloadBuilder;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Mortgage claim persistence")
class MortgageClaimPersistenceIT extends AbstractPostgresContainerIT {

    private static final String ORGANISATION_ID = "MORTGAGE-LENDER-ORG";

    @Autowired
    private PcsCaseService pcsCaseService;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private PCSCaseView pcsCaseView;
    @Autowired
    private ClaimFormPayloadBuilder claimFormPayloadBuilder;

    @MockitoBean
    private SecurityContextService securityContextService;
    @MockitoBean
    private OrganisationService organisationService;

    private long caseReference;

    @BeforeEach
    void setUp() {
        caseReference = 1781000000000000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000000000L);
        String userId = UUID.randomUUID().toString();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(securityContextService.getCurrentUserDetails()).thenReturn(
            UserInfo.builder().uid(userId).roles(List.of("caseworker-pcs-solicitor", "claimant-solicitor")).build());
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORGANISATION_ID);
    }

    @Test
    @DisplayName("creates a claim with no tenancy, grounds, notice or rent arrears and reads it back")
    void createsAndReadsMortgageClaim() {
        PcsCaseEntity caseEntity = createMortgageCase();

        ClaimEntity claim = caseEntity.getClaims().getFirst();
        assertThat(claim.getClaimantType()).isEqualTo(ClaimantType.MORTGAGE_LENDER);
        assertThat(claim.getClaimGrounds()).isEmpty();
        assertThat(claim.getNoticeOfPossession()).isNull();
        assertThat(claim.getRentArrears()).isNull();
        assertThat(claim.getPossessionAlternativesEntity()).isNull();
        assertThat(caseEntity.getTenancyLicence()).isNull();
        assertThat(claim.getStatementOfTruth()).isNotNull();

        attachMortgageParticulars(claim);
        pcsCaseRepository.saveAndFlush(caseEntity);

        PCSCase result = pcsCaseView.getCase(new CaseViewRequest<>(caseReference, State.CASE_ISSUED));

        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(result.getClaimGroundSummaries()).isNullOrEmpty();
        assertThat(result.getTenancyLicenceDetails()).isNull();
        assertThat(result.getNoticeServed()).isNull();
        assertThat(result.getRentArrears()).isNull();
    }

    @Test
    @DisplayName("builds the claim form payload for a mortgage claim")
    void buildsClaimFormPayload() {
        PcsCaseEntity caseEntity = createMortgageCase();
        attachMortgageParticulars(caseEntity.getClaims().getFirst());
        pcsCaseRepository.saveAndFlush(caseEntity);

        ClaimFormPayload payload = claimFormPayloadBuilder.build(caseEntity);

        assertThat(payload.getIsMortgageClaim()).isEqualTo("Yes");
        assertThat(payload.isEngland()).isTrue();
        assertThat(payload.getGrounds()).isEmpty();
        assertThat(payload.isShowGroundsList()).isFalse();
        assertThat(payload.getClaimant()).isNotNull();
        assertThat(payload.getDefendants()).hasSize(1);
        assertThatCode(() -> payload.getPropertyAddress().getAddressLine1()).doesNotThrowAnyException();
    }

    private PcsCaseEntity createMortgageCase() {
        OrganisationDetailsResponse orgDetails = OrganisationDetailsResponse.builder()
            .name("Mortgage Lender Ltd")
            .organisationIdentifier(ORGANISATION_ID)
            .organisationProfileIds(List.of("SOLICITOR_PROFILE"))
            .build();

        PCSCase pcsCase = mortgagePcsCase();

        pcsCaseService.createCase(caseReference, pcsCase.getPropertyAddress(), LegislativeCountry.ENGLAND, orgDetails);
        pcsCaseService.createMainClaimOnCase(caseReference, pcsCase, orgDetails);

        PcsCaseEntity caseEntity = pcsCaseService.loadCase(caseReference);
        caseEntity.getClaims().getFirst().setSubmissionChannel(SubmissionChannel.BULK_API);
        return caseEntity;
    }

    private static PCSCase mortgagePcsCase() {
        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("1 Mortgage Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        return PCSCase.builder()
            .propertyAddress(propertyAddress)
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .caseManagementLocationNumber(20262)
            .regionId(1)
            .claimantInformation(ClaimantInformation.builder()
                                     .claimantName("Mortgage Lender Ltd")
                                     .isClaimantNameCorrect(VerticalYesNo.YES)
                                     .build())
            .claimantContactPreferences(ClaimantContactPreferences.builder()
                                            .claimantContactEmail("lender@example.com")
                                            .isCorrectClaimantContactEmail(VerticalYesNo.YES)
                                            .isCorrectClaimantContactAddress(VerticalYesNo.NO)
                                            .overriddenClaimantContactAddress(propertyAddress)
                                            .claimantProvidePhoneNumber(VerticalYesNo.NO)
                                            .build())
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Dana")
                            .lastName("Borrower")
                            .addressKnown(VerticalYesNo.YES)
                            .addressSameAsPossession(VerticalYesNo.YES)
                            .build())
            .claimantType(DynamicStringList.builder()
                              .value(DynamicStringListElement.builder()
                                         .code(ClaimantType.MORTGAGE_LENDER.name())
                                         .label(ClaimantType.MORTGAGE_LENDER.getLabel())
                                         .build())
                              .build())
            .claimAgainstTrespassers(VerticalYesNo.NO)
            .preActionProtocolCompleted(VerticalYesNo.YES)
            .languageUsed(LanguageUsed.ENGLISH)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .statementOfTruth(StatementOfTruthDetails.builder()
                                  .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                                  .agreementClaimant(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE))
                                  .fullNameParty("A Signatory")
                                  .positionParty("Director")
                                  .build())
            .build();
    }

    private static void attachMortgageParticulars(ClaimEntity claim) {
        claim.setMortgageParticulars(MortgageParticularsEntity.builder()
                                         .mortgageDate(LocalDate.of(2015, 3, 1))
                                         .loanAmount(new BigDecimal("150000.00"))
                                         .currentRepaymentTerms("Monthly")
                                         .fullRepaymentDate(LocalDate.of(2045, 3, 1))
                                         .fullRepaymentAmount(new BigDecimal("250000.00"))
                                         .costsAndAdministration(new BigDecimal("500.00"))
                                         .startInterestRate(new BigDecimal("3.5000"))
                                         .preArrearsInterestRate(new BigDecimal("4.2500"))
                                         .claimStartInterestRate(new BigDecimal("5.0000"))
                                         .totalArrears(new BigDecimal("4200.00"))
                                         .moneyJudgmentWanted(YesOrNo.NO)
                                         .build());
        claim.addOccupant(ClaimOccupantEntity.builder().rank(1).firstName("Sam").lastName("Occupier").build());
        claim.addPaymentHistoryLine(ClaimPaymentHistoryLineEntity.builder()
                                        .rank(1)
                                        .paymentDueDate(LocalDate.of(2026, 1, 1))
                                        .amountDue(new BigDecimal("700.00"))
                                        .amountPaid(BigDecimal.ZERO)
                                        .cumulativeArrears(new BigDecimal("700.00"))
                                        .build());
    }
}
