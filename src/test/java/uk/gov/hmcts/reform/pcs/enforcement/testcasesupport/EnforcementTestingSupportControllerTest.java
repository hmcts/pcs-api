package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementTestingSupportControllerTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private EnforcementOrderService enforcementOrderService;
    @InjectMocks
    private EnforcementTestingSupportController underTest;

    @Test
    void shouldReturnNoContentWhenTheFeeIsRecorded() {
        when(enforcementOrderService.recordFeePaid(CASE_REFERENCE)).thenReturn(true);

        assertThat(underTest.recordFeePaid("s2s", CASE_REFERENCE).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturnNotFoundWhenNoApplicationIsAwaitingPayment() {
        when(enforcementOrderService.recordFeePaid(CASE_REFERENCE)).thenReturn(false);

        assertThat(underTest.recordFeePaid("s2s", CASE_REFERENCE).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
