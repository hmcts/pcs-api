package uk.gov.hmcts.reform.pcs.feesandpay.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementDataUtil.buildEnforcementTypes;

@ExtendWith(MockitoExtension.class)
class EnforcementPaymentCallbackHandlerTest {

    private static final UUID ENFORCEMENT_APPLICATION_ID = UUID.randomUUID();

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;
    @Mock
    private PaymentStatusCallback paymentStatusCallback;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PartyService partyService;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private EnforcementPaymentCallbackHandler underTest;

    @Test
    void shouldMoveEnforcementApplicationToCaseProgressionWhenPaymentIsPaid() {
        EnforcementOrderEntity order = pendingOrder(SelectEnforcementType.WARRANT);

        handlePaidPayment();

        assertThat(order.getState()).isEqualTo(EnforcementState.CASE_PROGRESSION);
        verify(enforcementOrderRepository).save(order);
    }

    @Test
    void shouldNotChangeStateWhenPaymentIsPaidButStateIsNotPendingCaseIssued() {
        EnforcementOrderEntity order = pendingOrder(SelectEnforcementType.WARRANT);
        order.setState(EnforcementState.CASE_PROGRESSION);

        handlePaidPayment();

        verify(enforcementOrderRepository, never()).save(order);
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldNotLookUpEnforcementApplicationWhenPaymentIsNotPaid() {
        underTest.handle(paymentStatusCallback, feePayment(PaymentStatus.NOT_PAID));

        verify(enforcementOrderRepository, never()).findById(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldThrowExceptionWhenEnforcementApplicationIsNotFound() {
        when(enforcementOrderRepository.findById(ENFORCEMENT_APPLICATION_ID)).thenReturn(Optional.empty());

        Throwable throwable = catchThrowable(this::handlePaidPayment);

        assertThat(throwable).isInstanceOf(EnforcementApplicationNotFoundException.class);
    }

    @Test
    @DisplayName("A paid warrant sends the warrant submitted email in the application's language")
    void givenWarrantPaidAndEnforcementFlagOn_whenHandle_thenSendWarrantOfPossessionSubmittedEmail() {
        EnforcementOrderEntity order = pendingOrder(SelectEnforcementType.WARRANT);
        order.setWarrantDetails(WarrantEntity.builder().languageUsed(LanguageUsed.WELSH).build());
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);
        PartyEntity claimant = claimantFor(order);

        handlePaidPayment();

        verify(notificationService).sendEnforcementSubmittedEmail(
            claimant, EmailTemplate.WARRANT_OF_POSSESSION_SUBMITTED, LanguageUsed.WELSH);
    }

    @Test
    @DisplayName("No warrant submitted email is sent while the enforcement flag is off")
    void givenWarrantPaidAndEnforcementFlagOff_whenHandle_thenDoNotSendEmail() {
        pendingOrder(SelectEnforcementType.WARRANT);
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(false);

        handlePaidPayment();

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("The warrant submitted email defaults to English when no language is set")
    void givenNoLanguage_whenHandle_thenSendEnglishEmail() {
        EnforcementOrderEntity order = pendingOrder(SelectEnforcementType.WARRANT);
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);
        PartyEntity claimant = claimantFor(order);

        handlePaidPayment();

        verify(notificationService).sendEnforcementSubmittedEmail(
            claimant, EmailTemplate.WARRANT_OF_POSSESSION_SUBMITTED, LanguageUsed.ENGLISH);
    }

    @Test
    @DisplayName("A failure while sending the email does not stop the payment update")
    void givenEmailSendingFails_whenHandle_thenStateIsStillUpdated() {
        EnforcementOrderEntity order = pendingOrder(SelectEnforcementType.WARRANT);
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);
        PartyEntity claimant = claimantFor(order);
        doThrow(new IllegalArgumentException("Missing legacy template"))
            .when(notificationService)
            .sendEnforcementSubmittedEmail(
                claimant, EmailTemplate.WARRANT_OF_POSSESSION_SUBMITTED, LanguageUsed.ENGLISH);

        Throwable throwable = catchThrowable(this::handlePaidPayment);

        assertThat(throwable).isNull();
        assertThat(order.getState()).isEqualTo(EnforcementState.CASE_PROGRESSION);
        verify(enforcementOrderRepository).save(order);
    }

    @Test
    @DisplayName("A missing enforcement type does not stop the payment update")
    void givenNoEnforcementType_whenHandle_thenStateIsStillUpdated() {
        EnforcementOrderEntity order = pendingOrder(SelectEnforcementType.WARRANT);
        order.setEnforcementOrder(EnforcementOrder.builder().build());
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);

        Throwable throwable = catchThrowable(this::handlePaidPayment);

        assertThat(throwable).isNull();
        assertThat(order.getState()).isEqualTo(EnforcementState.CASE_PROGRESSION);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("No email is sent for an enforcement type that has no submitted email template")
    void givenTypeWithoutTemplatePaid_whenHandle_thenDoNotSendEmail() {
        pendingOrder(SelectEnforcementType.WRIT_OF_RESTITUTION);
        when(featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)).thenReturn(true);

        handlePaidPayment();

        verifyNoInteractions(notificationService);
    }

    private EnforcementOrderEntity pendingOrder(SelectEnforcementType enforcementType) {
        EnforcementOrderEntity order = new EnforcementOrderEntity();
        order.setId(ENFORCEMENT_APPLICATION_ID);
        order.setState(EnforcementState.PENDING_CASE_ISSUED);
        order.setPcsCase(new PcsCaseEntity());
        order.setEnforcementOrder(EnforcementOrder.builder()
                                      .chooseEnforcementType(buildEnforcementTypes(enforcementType))
                                      .build());
        when(enforcementOrderRepository.findById(ENFORCEMENT_APPLICATION_ID)).thenReturn(Optional.of(order));
        return order;
    }

    private PartyEntity claimantFor(EnforcementOrderEntity order) {
        PartyEntity claimant = new PartyEntity();
        when(partyService.getPrimaryClaimantPartyEntity(order.getPcsCase())).thenReturn(claimant);
        return claimant;
    }

    private void handlePaidPayment() {
        underTest.handle(paymentStatusCallback, feePayment(PaymentStatus.PAID));
    }

    private static FeePaymentEntity feePayment(PaymentStatus paymentStatus) {
        return FeePaymentEntity.builder()
            .relatedEntityId(ENFORCEMENT_APPLICATION_ID)
            .paymentStatus(paymentStatus)
            .build();
    }
}
