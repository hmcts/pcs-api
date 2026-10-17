package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.enforcement.testcasesupport.EnforcementTestingSupportController.AllocateBailiffRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementTestingSupportControllerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String S2S_TOKEN = "s2s";
    private static final AllocateBailiffRequest ALLOCATE_REQUEST =
        new AllocateBailiffRequest("60d20156-049f-3f7f-881d-fce79ba89b76", "Bob Bailiff", "pcs-bailiff@localhost");
    private static final BailiffUser EXPECTED_BAILIFF =
        new BailiffUser("60d20156-049f-3f7f-881d-fce79ba89b76", "Bob Bailiff", "pcs-bailiff@localhost");

    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock
    private BailiffAllocationService bailiffAllocationService;
    @InjectMocks
    private EnforcementTestingSupportController underTest;

    @Test
    void shouldReturnNoContentWhenTheFeeIsRecorded() {
        when(enforcementOrderService.recordFeePaid(CASE_REFERENCE)).thenReturn(true);

        assertThat(underTest.recordFeePaid(S2S_TOKEN, CASE_REFERENCE).getStatusCode())
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturnNotFoundWhenNoApplicationIsAwaitingPayment() {
        when(enforcementOrderService.recordFeePaid(CASE_REFERENCE)).thenReturn(false);

        assertThat(underTest.recordFeePaid(S2S_TOKEN, CASE_REFERENCE).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnNoContentWhenTheBailiffIsAllocated() {
        assertThat(underTest.allocateBailiff(S2S_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST).getStatusCode())
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldAllocateTheRequestedBailiffToTheCase() {
        underTest.allocateBailiff(S2S_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST);

        verify(bailiffAllocationService).allocate(CASE_REFERENCE, EXPECTED_BAILIFF);
    }

    @Test
    void shouldPropagateWhenTheCaseHasNoEnforcement() {
        doThrow(new NoEnforcementException())
            .when(bailiffAllocationService).allocate(CASE_REFERENCE, EXPECTED_BAILIFF);

        assertThatThrownBy(() -> underTest.allocateBailiff(S2S_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST))
            .isInstanceOf(NoEnforcementException.class);
    }

    @Test
    void shouldPropagateWhenTheWarrantIsNotAnApprovedLiveWarrant() {
        doThrow(new EnforcementEventNotAvailableException("not allocatable"))
            .when(bailiffAllocationService).allocate(CASE_REFERENCE, EXPECTED_BAILIFF);

        assertThatThrownBy(() -> underTest.allocateBailiff(S2S_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST))
            .isInstanceOf(EnforcementEventNotAvailableException.class)
            .hasMessage("not allocatable");
    }
}
