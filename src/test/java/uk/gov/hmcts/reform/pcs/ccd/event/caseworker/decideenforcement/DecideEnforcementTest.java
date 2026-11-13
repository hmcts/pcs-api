package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.decideenforcement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.AcceptReject;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnforcementDecision;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RejectionReason;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementApplicationDecisionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WritEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.decideenforcement.AcceptOrRejectPage;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WarrantRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.WritRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.WarrantNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.WritNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecideEnforcementTest extends BaseEventTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private ClaimService claimService;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private WarrantRepository warrantRepository;
    @Mock
    private WritRepository writRepository;
    @Mock
    private AcceptOrRejectPage acceptOrRejectPage;
    @Mock
    private ClaimEntity claimEntity;

    @BeforeEach
    void setUp() {
        DecideEnforcement underTest = new DecideEnforcement(
            claimService,
            addressFormatter,
            warrantRepository,
            writRepository,
            acceptOrRejectPage
        );

        setEventUnderTest(underTest);
    }

    @Nested
    @DisplayName("Start Handler")
    class StartHandlerTests {

        @Test
        void shouldPropagateExceptionWhenLoadingClaim() {
            // Given
            ClaimNotFoundException claimNotFoundException = new ClaimNotFoundException(CASE_REFERENCE);
            when(claimService.loadClaim(CASE_REFERENCE)).thenThrow(claimNotFoundException);

            PCSCase caseData = PCSCase.builder().build();

            // When
            Throwable throwable = catchThrowable(() -> callStartHandler(caseData));

            // Then
            assertThat(throwable).isSameAs(claimNotFoundException);
        }

        @Test
        void shouldThrowExceptionWhenNoEnforcementOrder() {
            // Given
            when(claimService.loadClaim(CASE_REFERENCE)).thenReturn(claimEntity);
            when(claimEntity.getEnforcementOrders()).thenReturn(Set.of());

            PCSCase caseData = PCSCase.builder().build();

            // When
            Throwable throwable = catchThrowable(() -> callStartHandler(caseData));

            // Then
            assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No enforcement order found for case %s", CASE_REFERENCE);
        }

        @Test
        void shouldThrowExceptionWhenMultipleEnforcementOrders() {
            // Given
            when(claimService.loadClaim(CASE_REFERENCE)).thenReturn(claimEntity);
            EnforcementOrderEntity order1 = EnforcementOrderEntity.builder().build();
            EnforcementOrderEntity order2 = EnforcementOrderEntity.builder().build();
            when(claimEntity.getEnforcementOrders()).thenReturn(Set.of(order1, order2));

            PCSCase caseData = PCSCase.builder().build();

            // When
            Throwable throwable = catchThrowable(() -> callStartHandler(caseData));

            // Then
            assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Multiple active enforcement orders found for case %s", CASE_REFERENCE);
        }

        @Test
        void shouldThrowExceptionWhenNoWarrantOrWritForEnforcementOrder() {
            // Given
            when(claimService.loadClaim(CASE_REFERENCE)).thenReturn(claimEntity);

            UUID enforcementOrderId = UUID.randomUUID();
            EnforcementOrderEntity enforcementOrder = EnforcementOrderEntity.builder()
                .id(enforcementOrderId)
                .build();

            when(claimEntity.getEnforcementOrders()).thenReturn(Set.of(enforcementOrder));

            PCSCase caseData = PCSCase.builder().build();

            // When
            Throwable throwable = catchThrowable(() -> callStartHandler(caseData));

            // Then
            assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No warrant or writ found for enforcement order %s", enforcementOrderId);
        }

        @Test
        void shouldSetWarrantIdOnDecision() {
            // Given
            when(claimService.loadClaim(CASE_REFERENCE)).thenReturn(claimEntity);

            int warrantEntityId = 1001;
            WarrantEntity warrantEntity = WarrantEntity.builder()
                .id(warrantEntityId)
                .build();

            EnforcementOrderEntity enforcementOrder = EnforcementOrderEntity.builder()
                .warrantDetails(warrantEntity)
                .build();

            when(claimEntity.getEnforcementOrders()).thenReturn(Set.of(enforcementOrder));

            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(EnforcementDecision.builder().build())
                .build();

            // When
            callStartHandler(caseData);

            // Then
            assertThat(caseData.getEnforcementDecision().getWarrantId()).isEqualTo(warrantEntityId);
            assertThat(caseData.getEnforcementDecision().getWritId()).isNull();
        }

        @Test
        void shouldSetWritIdOnDecision() {
            // Given
            when(claimService.loadClaim(CASE_REFERENCE)).thenReturn(claimEntity);

            int writEntityId = 1002;
            WritEntity writEntity = WritEntity.builder()
                .id(writEntityId)
                .build();

            EnforcementOrderEntity enforcementOrder = EnforcementOrderEntity.builder()
                .writDetails(writEntity)
                .build();

            when(claimEntity.getEnforcementOrders()).thenReturn(Set.of(enforcementOrder));

            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(EnforcementDecision.builder().build())
                .build();

            // When
            callStartHandler(caseData);

            // Then
            assertThat(caseData.getEnforcementDecision().getWritId()).isEqualTo(writEntityId);
            assertThat(caseData.getEnforcementDecision().getWarrantId()).isNull();
        }
    }

    @Nested
    @DisplayName("Submit Handler")
    class SubmitHandlerTests {

        @Test
        void shouldThrowExceptionIfNoWarrantOrWritIdSet() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(EnforcementDecision.builder().build())
                .build();

            // When
            Throwable throwable = catchThrowable(() -> callSubmitHandler(caseData));

            // Then
            assertThat(throwable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No warrant or writ ID provided");
        }

        @Test
        void shouldThrowExceptionWhenUnknownWarrantId() {
            // Given
            int warrantId = 2001;
            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(EnforcementDecision.builder()
                                         .warrantId(warrantId)
                                         .build())
                .build();

            when(warrantRepository.findById(warrantId)).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> callSubmitHandler(caseData));

            // Then
            assertThat(throwable)
                .isInstanceOf(WarrantNotFoundException.class)
                .hasMessage("Unable to find warrant %d", warrantId);
        }

        @Test
        void shouldThrowExceptionWhenUnknownWritId() {
            // Given
            int writId = 2002;
            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(EnforcementDecision.builder()
                                         .writId(writId)
                                         .build())
                .build();

            when(writRepository.findById(writId)).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> callSubmitHandler(caseData));

            // Then
            assertThat(throwable)
                .isInstanceOf(WritNotFoundException.class)
                .hasMessage("Unable to find writ %d", writId);
        }

        @Test
        void shouldAttachAcceptEnforcementDecisionToWarrantEntity() {
            // Given
            int warrantId = 2001;
            EnforcementDecision enforcementDecision = EnforcementDecision.builder()
                .warrantId(warrantId)
                .acceptReject(AcceptReject.ACCEPT)
                .rejectionReason(RejectionReason.ANOTHER_ENFORCEMENT_ONGOING) // Should be ignored
                .changeInPartyCircumstancesDetails("should be ignored")
                .orderNotEnforceableDetails("should be ignored")
                .setAsideOrSuspendedDetails("should be ignored")
                .otherRejectionDetails("should be ignored")
                .build();

            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(enforcementDecision)
                .build();

            WarrantEntity warrantEntity = WarrantEntity.builder().build();
            when(warrantRepository.findById(warrantId)).thenReturn(Optional.of(warrantEntity));

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

            // Then
            EnforcementApplicationDecisionEntity decisionEntity
                = warrantEntity.getEnforcementApplicationDecision();

            assertThat(decisionEntity).isNotNull();
            assertThat(decisionEntity.getDecision()).isEqualTo(AcceptReject.ACCEPT);
            assertThat(decisionEntity.getRejectionReason()).isNull();
            assertThat(decisionEntity.getRejectionReasonAdditionalDetails()).isNull();

            assertThat(submitResponse.getConfirmationBody())
                .contains("Warrant or writ approved")
                .contains("If a warrant was approved, it will be sent to the bailiff.")
                .contains("If a writ was approved, it will be sent to the applicant.");
        }

        @Test
        void shouldAttachAcceptEnforcementDecisionToWritEntity() {
            // Given
            int writId = 2002;
            EnforcementDecision enforcementDecision = EnforcementDecision.builder()
                .writId(writId)
                .acceptReject(AcceptReject.ACCEPT)
                .rejectionReason(RejectionReason.ANOTHER_ENFORCEMENT_ONGOING) // Should be ignored
                .changeInPartyCircumstancesDetails("should be ignored")
                .orderNotEnforceableDetails("should be ignored")
                .setAsideOrSuspendedDetails("should be ignored")
                .otherRejectionDetails("should be ignored")
                .build();

            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(enforcementDecision)
                .build();

            WritEntity writEntity = WritEntity.builder().build();
            when(writRepository.findById(writId)).thenReturn(Optional.of(writEntity));

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

            // Then
            EnforcementApplicationDecisionEntity decisionEntity
                = writEntity.getEnforcementApplicationDecision();

            assertThat(decisionEntity).isNotNull();
            assertThat(decisionEntity.getDecision()).isEqualTo(AcceptReject.ACCEPT);
            assertThat(decisionEntity.getRejectionReason()).isNull();
            assertThat(decisionEntity.getRejectionReasonAdditionalDetails()).isNull();

            assertThat(submitResponse.getConfirmationBody())
                .contains("Warrant or writ approved")
                .contains("If a warrant was approved, it will be sent to the bailiff.")
                .contains("If a writ was approved, it will be sent to the applicant.");
        }

        @ParameterizedTest
        @MethodSource("rejectionScenarios")
        void shouldAttachRejectEnforcementDecisionToWarrantEntity(EnforcementDecision enforcementDecision,
                                                                  RejectionReason expectedRejectionReason,
                                                                  String expectedRejectionDetails) {
            // Given
            int warrantId = 2001;
            enforcementDecision.setWarrantId(warrantId);

            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(enforcementDecision)
                .build();

            WarrantEntity warrantEntity = WarrantEntity.builder().build();
            when(warrantRepository.findById(warrantId)).thenReturn(Optional.of(warrantEntity));

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

            // Then
            EnforcementApplicationDecisionEntity decisionEntity
                = warrantEntity.getEnforcementApplicationDecision();

            assertThat(decisionEntity).isNotNull();
            assertThat(decisionEntity.getDecision()).isEqualTo(AcceptReject.REJECT);
            assertThat(decisionEntity.getRejectionReason()).isEqualTo(expectedRejectionReason);
            assertThat(decisionEntity.getRejectionReasonAdditionalDetails()).isEqualTo(expectedRejectionDetails);

            assertThat(submitResponse.getConfirmationBody())
                .contains("Warrant or writ rejected")
                .contains("A notification will be sent to the applicant.");
        }

        @ParameterizedTest
        @MethodSource("rejectionScenarios")
        void shouldAttachRejectEnforcementDecisionToWritEntity(EnforcementDecision enforcementDecision,
                                                               RejectionReason expectedRejectionReason,
                                                               String expectedRejectionDetails) {
            // Given
            int writId = 2002;
            enforcementDecision.setWritId(writId);

            PCSCase caseData = PCSCase.builder()
                .enforcementDecision(enforcementDecision)
                .build();

            WritEntity writEntity = WritEntity.builder().build();
            when(writRepository.findById(writId)).thenReturn(Optional.of(writEntity));

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

            // Then
            EnforcementApplicationDecisionEntity decisionEntity
                = writEntity.getEnforcementApplicationDecision();

            assertThat(decisionEntity).isNotNull();
            assertThat(decisionEntity.getDecision()).isEqualTo(AcceptReject.REJECT);
            assertThat(decisionEntity.getRejectionReason()).isEqualTo(expectedRejectionReason);
            assertThat(decisionEntity.getRejectionReasonAdditionalDetails()).isEqualTo(expectedRejectionDetails);

            assertThat(submitResponse.getConfirmationBody())
                .contains("Warrant or writ rejected")
                .contains("A notification will be sent to the applicant.");
        }

        private static Stream<Arguments> rejectionScenarios() {
            return Stream.of(
                argumentSet(
                    "Change in party circumstances",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.CHANGE_IN_PARTY_CIRCUMSTANCES)
                        .changeInPartyCircumstancesDetails("details of change in party circumstances")
                        .orderNotEnforceableDetails("should be ignored")
                        .setAsideOrSuspendedDetails("should be ignored")
                        .otherRejectionDetails("should be ignored")
                        .build(),
                    RejectionReason.CHANGE_IN_PARTY_CIRCUMSTANCES, "details of change in party circumstances"
                ),
                argumentSet(
                    "Applying to enforce out of time",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.APPLYING_OUT_OF_TIME)
                        .changeInPartyCircumstancesDetails("should be ignored")
                        .orderNotEnforceableDetails("should be ignored")
                        .setAsideOrSuspendedDetails("should be ignored")
                        .otherRejectionDetails("should be ignored")
                        .build(),
                    RejectionReason.APPLYING_OUT_OF_TIME, null
                ),
                argumentSet(
                    "Order not enforceable",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.ORDER_NOT_ENFORCEABLE)
                        .changeInPartyCircumstancesDetails("should be ignored")
                        .orderNotEnforceableDetails("order not enforceable details")
                        .setAsideOrSuspendedDetails("should be ignored")
                        .otherRejectionDetails("should be ignored")
                        .build(),
                    RejectionReason.ORDER_NOT_ENFORCEABLE, "order not enforceable details"
                ),
                argumentSet(
                    "Possession order set aside or suspended",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.ORDER_SET_ASIDE_OR_SUSPENDED)
                        .changeInPartyCircumstancesDetails("should be ignored")
                        .orderNotEnforceableDetails("should be ignored")
                        .setAsideOrSuspendedDetails("set aside or suspended details")
                        .otherRejectionDetails("should be ignored")
                        .build(),
                    RejectionReason.ORDER_SET_ASIDE_OR_SUSPENDED, "set aside or suspended details"
                ),
                argumentSet(
                    "Applying to enforce before the possession date",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.APPLYING_BEFORE_POSSESSION_DATE)
                        .changeInPartyCircumstancesDetails("should be ignored")
                        .orderNotEnforceableDetails("should be ignored")
                        .setAsideOrSuspendedDetails("should be ignored")
                        .otherRejectionDetails("should be ignored")
                        .build(),
                    RejectionReason.APPLYING_BEFORE_POSSESSION_DATE, null
                ),
                argumentSet(
                    "Case not transferred to high court",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.CASE_NOT_TRANSFERRED_TO_HIGH_COURT)
                        .changeInPartyCircumstancesDetails("should be ignored")
                        .orderNotEnforceableDetails("should be ignored")
                        .setAsideOrSuspendedDetails("should be ignored")
                        .otherRejectionDetails("should be ignored")
                        .build(),
                    RejectionReason.CASE_NOT_TRANSFERRED_TO_HIGH_COURT, null
                ),
                argumentSet(
                    "Another possession enforcement journey ongoing",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.ANOTHER_ENFORCEMENT_ONGOING)
                        .changeInPartyCircumstancesDetails("should be ignored")
                        .orderNotEnforceableDetails("should be ignored")
                        .setAsideOrSuspendedDetails("should be ignored")
                        .otherRejectionDetails("should be ignored")
                        .build(),
                    RejectionReason.ANOTHER_ENFORCEMENT_ONGOING, null
                ),
                argumentSet(
                    "Defendant(s) in breathing space",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.DEFENDANTS_IN_BREATHING_SPACE)
                        .changeInPartyCircumstancesDetails("should be ignored")
                        .orderNotEnforceableDetails("should be ignored")
                        .setAsideOrSuspendedDetails("should be ignored")
                        .otherRejectionDetails("should be ignored")
                        .build(),
                    RejectionReason.DEFENDANTS_IN_BREATHING_SPACE, null
                ),
                argumentSet(
                    "Other rejection reason",
                    EnforcementDecision.builder()
                        .acceptReject(AcceptReject.REJECT)
                        .rejectionReason(RejectionReason.OTHER)
                        .changeInPartyCircumstancesDetails("should be ignored")
                        .orderNotEnforceableDetails("should be ignored")
                        .setAsideOrSuspendedDetails("should be ignored")
                        .otherRejectionDetails("other rejection details")
                        .build(),
                    RejectionReason.OTHER, "other rejection details"
                )
            );
        }
    }

}

