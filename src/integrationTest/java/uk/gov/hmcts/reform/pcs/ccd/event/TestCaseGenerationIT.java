package uk.gov.hmcts.reform.pcs.ccd.event;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestCaseSupportHelper;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.util.IdamHelper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.TestCaseGeneration.ENFORCEMENT_CASE_GENERATOR;
import static uk.gov.hmcts.reform.pcs.ccd.event.TestCaseGeneration.MAKE_A_CLAIM_CASE_GENERATOR;
import static uk.gov.hmcts.reform.pcs.ccd.event.TestCaseGeneration.NO_NON_PROD_CASE_AVAILABLE;

/**
 * This is based on the test json sitting in src/main/java/resources/test-case-generation
 * A change to the domain CAN lead to a failure here as the test json will need to be updated so to keep the test
 * creation event working.   In order to update the json when you change the domain then a walk though the broken
 * journey is required and a capture of the json from the draft_case_data BEFORE submit of that event need to be made
 * and replace the existing json within the relevant file.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("TestCaseGenerationIT Integration Tests")
public class TestCaseGenerationIT extends AbstractPostgresContainerIT {

    private static final String SYSTEM_USER_ID_TOKEN = "system-user-id-token";
    private static final UUID USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final long CASE_REFERENCE = 1234567890123456L;

    @Autowired
    private TestCaseGeneration underTest;
    @Autowired
    private TestCaseSupportHelper testCaseSupportHelper;
    @MockitoBean
    private IdamClient idamClient;
    @Autowired
    private IdamHelper idamHelper;
    @MockitoBean
    private FeeService feeService;

    @BeforeEach
    void setUp() {
        idamHelper.stubIdamSystemUser(idamClient, SYSTEM_USER_ID_TOKEN);

        // Stub IDAM user info for token validation
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getUid()).thenReturn(USER_ID.toString());
        when(idamClient.getUserInfo(anyString())).thenReturn(userInfo);

        User user = new User("testing", userInfo);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        FeeDetails feeDetails = FeeDetails.builder().code("FEE0001").feeAmount(new BigDecimal("123.45")).build();
        when(feeService.getFee(any(FeeType.class))).thenReturn(feeDetails);

    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void shouldSubmitMakeAClaim() {
        // Given
        String label = selectLabelStartingWith(MAKE_A_CLAIM_CASE_GENERATOR);
        EventPayload<PCSCase, State> eventPayload = buildEventPayload(label);

        // When
        SubmitResponse<State> response = underTest.submit(eventPayload);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo(CASE_ISSUED);
    }

    @Test
    @Transactional
    void shouldSubmitEnforcement() {
        // Given
        String label = selectLabelStartingWith(ENFORCEMENT_CASE_GENERATOR);
        EventPayload<PCSCase, State> eventPayload = buildEventPayload(label);

        // When
        SubmitResponse<State> response = underTest.submit(eventPayload);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo(CASE_ISSUED);
    }

    @Test
    @Transactional
    void shouldThrowWhenNoTestFilesListAvailable() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        EventPayload<PCSCase, State> eventPayload =
            new EventPayload<>(CASE_REFERENCE, pcsCase, null);

        // When / Then
        assertThatThrownBy(() -> underTest.submit(eventPayload))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(NO_NON_PROD_CASE_AVAILABLE);
    }

    private EventPayload<PCSCase, State> buildEventPayload(String label) {
        DynamicListElement selected = DynamicListElement.builder()
            .code(UUID.randomUUID())
            .label(label)
            .build();

        DynamicList testFilesList = DynamicList.builder()
            .value(selected)
            .listItems(List.of(selected))
            .build();

        PCSCase pcsCase = PCSCase.builder().testCaseSupportFileList(testFilesList).build();
        return new EventPayload<>(CASE_REFERENCE, pcsCase, null);
    }

    private String selectLabelStartingWith(String prefix) {
        DynamicList fileList = testCaseSupportHelper.getFileList();
        assertThat(fileList).isNotNull();
        assertThat(fileList.getListItems()).isNotNull();

        return fileList.getListItems().stream()
            .map(DynamicListElement::getLabel)
            .filter(l -> l != null && l.startsWith(prefix))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No test-case-generation file found with prefix: " + prefix));
    }

}
