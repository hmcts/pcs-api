package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule.EvictionConfirmationFormat;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementNotificationServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;
    private static final LocalDateTime EVICTION_DATE_TIME = LocalDateTime.of(2026, 6, 25, 10, 0);

    @Mock
    private EnforcementNotificationSender sender;

    @InjectMocks
    private EnforcementNotificationService underTest;

    @AfterEach
    void clearSynchronisation() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldEmailTheClaimantTheWarrantNumberOnApproval() {
        underTest.notifyClaimantOfApproval(CASE_REFERENCE, SelectEnforcementType.WARRANT, "W123456");

        assertThat(personalisationSentFor(EnforcementEmail.WARRANT_APPROVED))
            .containsExactly(Map.entry(EnforcementEmail.ENFORCEMENT_NUMBER, "W123456"));
    }

    @Test
    void shouldEmailTheClaimantTheWritNumberOnApproval() {
        underTest.notifyClaimantOfApproval(CASE_REFERENCE, SelectEnforcementType.WRIT, "P123456");

        assertThat(personalisationSentFor(EnforcementEmail.WRIT_APPROVED))
            .containsExactly(Map.entry(EnforcementEmail.ENFORCEMENT_NUMBER, "P123456"));
    }

    @Test
    void shouldEmailTheClaimantTheReasonOnRejection() {
        underTest.notifyClaimantOfRejection(CASE_REFERENCE, SelectEnforcementType.WARRANT, "No order was made");

        assertThat(personalisationSentFor(EnforcementEmail.APPLICATION_REJECTED))
            .containsExactly(Map.entry(EnforcementEmail.REJECTION_REASON, "No order was made"));
    }

    @Test
    void shouldAskTheClaimantToConfirmTheScheduledEvictionDate() {
        underTest.notifyClaimantOfScheduledEviction(CASE_REFERENCE, EVICTION_DATE_TIME);

        assertThat(personalisationSentFor(EnforcementEmail.EVICTION_SCHEDULED))
            .containsExactly(Map.entry(EnforcementEmail.EVICTION_DATE_TIME, "Thursday 25 June 2026 at 10:00am"));
    }

    @Test
    void shouldAskTheClaimantToConfirmTheRescheduledEvictionDate() {
        underTest.notifyClaimantOfRescheduledEviction(CASE_REFERENCE, EVICTION_DATE_TIME);

        assertThat(personalisationSentFor(EnforcementEmail.EVICTION_RESCHEDULED))
            .containsExactly(Map.entry(EnforcementEmail.EVICTION_DATE_TIME, "Thursday 25 June 2026 at 10:00am"));
    }

    @ParameterizedTest
    @CsvSource({
        "WARRANT, WARRANT_APPLICATION_RECEIVED",
        "WRIT, WRIT_APPLICATION_RECEIVED",
        "WARRANT_OF_RESTITUTION, WARRANT_OF_RESTITUTION_APPLICATION_RECEIVED",
        "WRIT_OF_RESTITUTION, WRIT_OF_RESTITUTION_APPLICATION_RECEIVED",
    })
    void shouldTellTheClaimantTheApplicationWasReceived(SelectEnforcementType enforcementType,
                                                       EnforcementEmail email) {
        underTest.notifyClaimantOfApplicationReceived(CASE_REFERENCE, enforcementType);

        assertThat(personalisationSentFor(email)).isEmpty();
    }

    @Test
    void shouldNotEmailThatAnApplicationOfUnknownTypeWasReceived() {
        underTest.notifyClaimantOfApplicationReceived(CASE_REFERENCE, null);

        verifyNoInteractions(sender);
    }

    @Test
    void shouldTellTheClaimantTheyConfirmedTheEvictionDate() {
        underTest.notifyClaimantOfConfirmedAttendance(CASE_REFERENCE, EVICTION_DATE_TIME);

        assertThat(personalisationSentFor(EnforcementEmail.CLAIMANT_CONFIRMED_ATTENDANCE))
            .containsExactly(Map.entry(EnforcementEmail.EVICTION_DATE_TIME, "Thursday 25 June 2026 at 10:00am"));
    }

    @Test
    void shouldTellTheClaimantTheySaidTheyCannotAttend() {
        underTest.notifyClaimantOfDeclinedAttendance(CASE_REFERENCE);

        assertThat(personalisationSentFor(EnforcementEmail.CLAIMANT_DECLINED_ATTENDANCE)).isEmpty();
    }

    /**
     * HDPI-7538 has no approval wording for either kind of restitution, so nothing is sent rather than an
     * email the service has not agreed (OPEN-QUESTIONS 56).
     */
    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class,
        names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void shouldNotEmailOnApprovalOfARestitutionApplication(SelectEnforcementType enforcementType) {
        underTest.notifyClaimantOfApproval(CASE_REFERENCE, enforcementType, "W123456");

        verifyNoInteractions(sender);
    }

    /**
     * HDPI-7538 only has rejection wording for a warrant of possession (OPEN-QUESTIONS 56).
     */
    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class,
        names = {"WRIT", "WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void shouldNotEmailOnRejectionOfAnApplicationWithNoAgreedWording(SelectEnforcementType enforcementType) {
        underTest.notifyClaimantOfRejection(CASE_REFERENCE, enforcementType, "No order was made");

        verifyNoInteractions(sender);
    }

    @Test
    void shouldNotEmailWhenTheEnforcementTypeIsUnknown() {
        underTest.notifyClaimantOfApproval(CASE_REFERENCE, null, "W123456");
        underTest.notifyClaimantOfRejection(CASE_REFERENCE, null, "No order was made");

        verifyNoInteractions(sender);
    }

    @Test
    void shouldQueueTheEmailInsideTheEventTransaction() {
        TransactionSynchronizationManager.initSynchronization();

        underTest.notifyClaimantOfScheduledEviction(CASE_REFERENCE, EVICTION_DATE_TIME);

        verify(sender).send(eq(CASE_REFERENCE), eq(EnforcementEmail.EVICTION_SCHEDULED), any());
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    /**
     * A failure to queue the email fails the event, so the decision is rolled back rather than recorded
     * without its email.
     */
    @Test
    void shouldFailTheEventWhenTheEmailCannotBeQueued() {
        when(sender.send(anyLong(), any(), any())).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(
            () -> underTest.notifyClaimantOfApproval(CASE_REFERENCE, SelectEnforcementType.WARRANT, "W123456"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldSendEmptyPersonalisationRatherThanNull() {
        underTest.notifyClaimantOfApproval(CASE_REFERENCE, SelectEnforcementType.WARRANT, null);
        assertThat(personalisationSentFor(EnforcementEmail.WARRANT_APPROVED))
            .containsExactly(Map.entry(EnforcementEmail.ENFORCEMENT_NUMBER, ""));
    }

    /**
     * The email has to say what the confirmation screen said, so the bailiff and the claimant see one date.
     */
    @ParameterizedTest
    @ValueSource(strings = {"2026-06-25T10:00", "2026-07-01T09:30", "2026-12-31T13:05", "2026-01-05T00:00"})
    void shouldFormatTheEvictionDateAsTheConfirmationScreensDo(String evictionDateTime) {
        LocalDateTime dateTime = LocalDateTime.parse(evictionDateTime);

        underTest.notifyClaimantOfScheduledEviction(CASE_REFERENCE, dateTime);

        assertThat(personalisationSentFor(EnforcementEmail.EVICTION_SCHEDULED))
            .containsExactly(Map.entry(EnforcementEmail.EVICTION_DATE_TIME,
                                       EvictionConfirmationFormat.format(dateTime)));
    }

    @Test
    void shouldSendEmptyEvictionDateRatherThanNull() {
        underTest.notifyClaimantOfScheduledEviction(CASE_REFERENCE, null);

        assertThat(personalisationSentFor(EnforcementEmail.EVICTION_SCHEDULED))
            .containsExactly(Map.entry(EnforcementEmail.EVICTION_DATE_TIME, ""));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> personalisationSentFor(EnforcementEmail email) {
        ArgumentCaptor<Map<String, Object>> personalisation = ArgumentCaptor.forClass(Map.class);
        verify(sender).send(eq(CASE_REFERENCE), eq(email), personalisation.capture());
        return personalisation.getValue();
    }
}
