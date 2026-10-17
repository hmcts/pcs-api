package uk.gov.hmcts.reform.pcs.enforcement.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantReturnNotificationServiceTest {

    private static final long CASE_REFERENCE = 1234567812345678L;

    @Mock
    private EnforcementNotificationSender sender;

    @InjectMocks
    private WarrantReturnNotificationService underTest;

    @Test
    void shouldEmailTheClaimantTheBodyForTheReturnName() {
        when(sender.send(anyLong(), any(), any())).thenReturn(true);

        boolean sent = underTest.notifyClaimant(
            CASE_REFERENCE, WarrantReturnName.OTHER_INTERIM_RETURN, "Occupant presented a court order");

        assertThat(sent).isTrue();
        ArgumentCaptor<Map<String, Object>> personalisation = captor();
        verify(sender).send(eq(CASE_REFERENCE), eq(EnforcementEmail.WARRANT_RETURN), personalisation.capture());
        assertThat(personalisation.getValue())
            .containsEntry(EnforcementEmail.RETURN_NAME, "Other interim return")
            .hasEntrySatisfying(EnforcementEmail.WARRANT_RETURN_BODY, body -> assertThat((String) body)
                .contains("The reason provided by the bailiff is: Occupant presented a court order"));
    }

    @ParameterizedTest
    @EnumSource(value = WarrantReturnName.class, names = {
        "EVICTION_SUCCESSFUL", "WARRANT_SUSPENDED_BY_ORDER", "POSSESSION_ORDER_SET_ASIDE"})
    void shouldNotEmailForAReturnNameThatDoesNotNotify(WarrantReturnName returnName) {
        assertThat(underTest.notifyClaimant(CASE_REFERENCE, returnName, "details")).isFalse();

        verifyNoInteractions(sender);
    }

    /**
     * No warrant return template exists in Notify yet, so the sender schedules nothing and the service has to
     * report that rather than claim the claimant was told.
     */
    @Test
    void shouldReportNothingSentWhenTheTemplateIsNotConfigured() {
        when(sender.send(anyLong(), any(), any())).thenReturn(false);

        assertThat(underTest.notifyClaimant(CASE_REFERENCE, WarrantReturnName.WARRANT_WITHDRAWN, "x")).isFalse();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<String, Object>> captor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
