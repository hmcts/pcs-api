package uk.gov.hmcts.reform.pcs.enforcement.testcasesupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.enforcement.testcasesupport.EnforcementTestingSupportController.AllocateBailiffRequest;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import org.springframework.security.core.context.SecurityContextHolder;

import uk.gov.hmcts.reform.pcs.enforcement.testcasesupport.EnforcementTestingSupportController.BackdateRequest;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementTestingSupportControllerTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String S2S_TOKEN = "s2s";
    private static final String USER_TOKEN = "Bearer user-token";
    private static final AllocateBailiffRequest ALLOCATE_REQUEST =
        new AllocateBailiffRequest("60d20156-049f-3f7f-881d-fce79ba89b76", "Bob Bailiff", "pcs-bailiff@localhost");
    private static final BailiffUser EXPECTED_BAILIFF =
        new BailiffUser("60d20156-049f-3f7f-881d-fce79ba89b76", "Bob Bailiff", "pcs-bailiff@localhost");

    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock
    private BailiffAllocationService bailiffAllocationService;
    @Mock
    private IdamAuthenticator idamAuthenticator;
    @Mock
    private EnforcementBackdateService enforcementBackdateService;
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
        givenTheCallerIsAuthenticated();
        givenTheAllocation(EnforcementChange.made());

        assertThat(underTest.allocateBailiff(S2S_TOKEN, USER_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST)
                       .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldAllocateTheRequestedBailiffToTheCase() {
        givenTheCallerIsAuthenticated();
        givenTheAllocation(EnforcementChange.made());

        underTest.allocateBailiff(S2S_TOKEN, USER_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST);

        verify(bailiffAllocationService).allocate(CASE_REFERENCE, EXPECTED_BAILIFF);
    }

    /**
     * BailiffAllocationService reads the current user, so the allocation must run with the caller on the
     * security context. The context is cleared again afterwards.
     */
    @Test
    void shouldRunTheAllocationAsTheCallerAndClearTheContextAfterwards() {
        User caller = givenTheCallerIsAuthenticated();
        AtomicReference<Object> principalDuringAllocation = new AtomicReference<>();
        when(bailiffAllocationService.allocate(CASE_REFERENCE, EXPECTED_BAILIFF)).thenAnswer(invocation -> {
            principalDuringAllocation.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            return EnforcementChange.made();
        });

        underTest.allocateBailiff(S2S_TOKEN, USER_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST);

        assertThat(principalDuringAllocation.get()).isSameAs(caller);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * The service refuses rather than throwing, so a refusal that came back as 204 would read as success.
     */
    @Test
    void shouldReportTheRefusalWhenTheWarrantCannotBeAllocated() {
        givenTheCallerIsAuthenticated();
        givenTheAllocation(EnforcementChange.refused("There is no warrant on this case"));

        ResponseEntity<List<String>> response =
            underTest.allocateBailiff(S2S_TOKEN, USER_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsExactly("There is no warrant on this case");
    }

    @Test
    void shouldReportEveryRefusalReason() {
        givenTheCallerIsAuthenticated();
        givenTheAllocation(EnforcementChange.refused(List.of("first reason", "second reason")));

        assertThat(underTest.allocateBailiff(S2S_TOKEN, USER_TOKEN, CASE_REFERENCE, ALLOCATE_REQUEST).getBody())
            .containsExactly("first reason", "second reason");
    }

    @Test
    void shouldReturnNoContentWhenTheEnforcementIsBackdated() {
        Period twelveMonths = Period.ofMonths(12);
        when(enforcementBackdateService.backdate(CASE_REFERENCE, "WP-00000001", twelveMonths, null))
            .thenReturn(Optional.of(UUID.randomUUID()));

        assertThat(underTest.backdate(S2S_TOKEN, CASE_REFERENCE,
                                      new BackdateRequest(twelveMonths, null, "WP-00000001")).getStatusCode())
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldReturnNotFoundWhenThereIsNoEnforcementToBackdate() {
        Duration seventyThreeHours = Duration.ofHours(73);
        when(enforcementBackdateService.backdate(CASE_REFERENCE, null, null, seventyThreeHours))
            .thenReturn(Optional.empty());

        assertThat(underTest.backdate(S2S_TOKEN, CASE_REFERENCE,
                                      new BackdateRequest(null, seventyThreeHours, null)).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectABackdateWithNoAmount() {
        assertThat(underTest.backdate(S2S_TOKEN, CASE_REFERENCE,
                                      new BackdateRequest(null, null, null)).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(enforcementBackdateService);
    }

    private void givenTheAllocation(EnforcementChange<Void> outcome) {
        when(bailiffAllocationService.allocate(CASE_REFERENCE, EXPECTED_BAILIFF)).thenReturn(outcome);
    }

    private User givenTheCallerIsAuthenticated() {
        User caller = new User(USER_TOKEN, UserInfo.builder().uid("caller-id").roles(List.of()).build());
        when(idamAuthenticator.validateAuthToken(USER_TOKEN)).thenReturn(caller);
        return caller;
    }
}
