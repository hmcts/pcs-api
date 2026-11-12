package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenPossessionClaimViewTest {

    private static final String CITIZEN_ID = "citizen-1";

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private SecurityContextService securityContextService;

    private CitizenPossessionClaimView underTest;
    private PcsCaseEntity pcsCaseEntity;
    private PartyEntity claimCreator;

    @BeforeEach
    void setUp() {
        underTest = new CitizenPossessionClaimView(featureToggleService, securityContextService);

        pcsCaseEntity = new PcsCaseEntity();
        claimCreator = new PartyEntity();
        claimCreator.setClaimCreator(true);
        claimCreator.setIdamId(CITIZEN_ID);
        pcsCaseEntity.addParty(claimCreator);

        ClaimEntity claim = ClaimEntity.builder().build();
        claim.addFeePayment(FeePaymentEntity.builder()
                                .id(1)
                                .serviceRequestReference("2026-1")
                                .amount(new BigDecimal("404.00"))
                                .paymentStatus(PaymentStatus.NOT_PAID)
                                .paymentCallbackHandlerType(PaymentCallbackHandlerType.CLAIM)
                                .build());
        pcsCaseEntity.addClaim(claim);
    }

    @Test
    void shouldGiveTheClaimingCitizenTheirServiceRequest() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(securityContextService.getCurrentUserId()).thenReturn(CITIZEN_ID);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        CitizenClaimSubmission submission = pcsCase.getCitizenPossessionClaim().getSubmission();
        assertThat(submission.getServiceRequestReference()).isEqualTo("2026-1");
        assertThat(submission.getFeeAmount()).isEqualByComparingTo("404.00");
        assertThat(submission.getPaymentStatus()).isEqualTo("NOT_PAID");
    }

    @Test
    void shouldNotShowTheServiceRequestToAnyoneElse() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(securityContextService.getCurrentUserId()).thenReturn("someone-else");
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertThat(pcsCase.getCitizenPossessionClaim()).isNull();
    }

    @Test
    void shouldDoNothingWhenTheFlagIsOffWithoutLookingAtTheCase() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(false);
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, caseEntity);

        assertThat(pcsCase.getCitizenPossessionClaim()).isNull();
        verifyNoInteractions(caseEntity, securityContextService);
    }

    @Test
    void shouldNotLookUpTheUserForCasesNotMadeByACitizen() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        claimCreator.setIdamId(null);
        claimCreator.setOrganisationId("ORG1");
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertThat(pcsCase.getCitizenPossessionClaim()).isNull();
        verifyNoInteractions(securityContextService);
    }

}
