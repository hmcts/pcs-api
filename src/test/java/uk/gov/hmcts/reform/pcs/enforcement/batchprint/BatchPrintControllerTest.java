package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.batchprint.BatchPrintController.BatchPrintRequest;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchPrintControllerTest {

    private static final String AUTHORIZATION = "Bearer bailiff-token";
    private static final String S2S = "Bearer s2s";
    private static final String BAILIFF = "bob-id";
    private static final List<String> CHOSEN = List.of("1111111111111111", "2222222222222222", "not-a-case");

    @Mock
    private IdamAuthenticator idamAuthenticator;
    @Mock
    private BatchPrintService batchPrintService;
    @Mock
    private BatchPrintPlanner batchPrintPlanner;

    private BatchPrintController underTest;

    @BeforeEach
    void setUp() {
        underTest = new BatchPrintController(idamAuthenticator, batchPrintService, batchPrintPlanner);
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn(BAILIFF);
        User user = mock(User.class);
        when(user.getUserDetails()).thenReturn(userInfo);
        when(idamAuthenticator.validateAuthToken(AUTHORIZATION)).thenReturn(user);
    }

    @Test
    void shouldReturnThePdfNamingThePrintedAndLeftOutCases() {
        Resource pdf = new ByteArrayResource(new byte[] {'%', 'P', 'D', 'F'});
        BatchPrintPlan plan = new BatchPrintPlan(
            List.of(new PrintableCase("1111111111111111", "WP-1", List.of(), List.of())),
            List.of(ExcludedCase.of("2222222222222222", BatchPrintExclusion.NOT_ALLOCATED),
                    ExcludedCase.of("not-a-case", BatchPrintExclusion.INVALID_REFERENCE)));
        when(batchPrintService.print(BAILIFF, CHOSEN)).thenReturn(new BatchPrint(pdf, plan));

        ResponseEntity<?> response = underTest.print(AUTHORIZATION, S2S, new BatchPrintRequest(CHOSEN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
            .isEqualTo("attachment; filename=\"enforcement-documents.pdf\"");
        assertThat(response.getHeaders().getFirst(BatchPrintController.INCLUDED_HEADER))
            .isEqualTo("1111111111111111");
        assertThat(response.getHeaders().getFirst(BatchPrintController.EXCLUDED_HEADER))
            .isEqualTo("2222222222222222=NOT_ALLOCATED");
        assertThat(response.getBody()).isSameAs(pdf);
    }

    @Test
    void shouldExplainWhyWhenNothingCanBePrinted() {
        BatchPrintPlan plan = new BatchPrintPlan(
            List.of(), List.of(ExcludedCase.of("2222222222222222", BatchPrintExclusion.NOT_ALLOCATED)));
        when(batchPrintService.print(BAILIFF, CHOSEN)).thenReturn(new BatchPrint(null, plan));

        ResponseEntity<?> response = underTest.print(AUTHORIZATION, S2S, new BatchPrintRequest(CHOSEN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isEqualTo(plan);
    }

    @Test
    void shouldCheckAsTheSignedInBailiff() {
        BatchPrintPlan plan = new BatchPrintPlan(List.of(), List.of());
        when(batchPrintPlanner.plan(BAILIFF, CHOSEN)).thenReturn(plan);

        ResponseEntity<BatchPrintPlan> response = underTest.check(AUTHORIZATION, S2S, new BatchPrintRequest(CHOSEN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(plan);
    }

    @Test
    void shouldListTheSignedInBailiffsWarrants() {
        LocalDate from = LocalDate.of(2026, 10, 12);
        LocalDate to = LocalDate.of(2026, 10, 18);
        List<AllocatedWarrant> warrants = List.of(new AllocatedWarrant(
            "1111111111111111", "WP-1", EnforcementStatus.CLAIMANT_CONFIRMED, LocalDateTime.of(2026, 10, 13, 9, 0)));
        when(batchPrintPlanner.allocatedWarrants(BAILIFF, from, to)).thenReturn(warrants);

        ResponseEntity<List<AllocatedWarrant>> response = underTest.allocatedWarrants(AUTHORIZATION, S2S, from, to);

        assertThat(response.getBody()).isSameAs(warrants);
    }
}
