package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.amenddocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.documentamend.AmendDocumentDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.documentamend.SelectDocumentPage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

import java.time.Clock;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmendDocumentsTest extends BaseEventTest {

    @Mock
    private DocumentSelectionService documentSelectionService;
    @Mock
    private DocumentAmendService documentAmendService;
    @Mock
    private ConfirmationPanelBuilder confirmationPanelBuilder;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new AmendDocuments(
            documentSelectionService,
            documentAmendService,
            confirmationPanelBuilder,
            new SelectDocumentPage(documentSelectionService, documentAmendService),
            new AmendDocumentDetailsPage(Clock.systemUTC())
        ));
    }

    @Test
    void shouldConfigureBatchOneEventAccessAndNavigation() {
        assertThat(configuredEvent.getName()).isEqualTo("Manage documents: Amend");
        assertThat(configuredEvent.getPreState()).containsExactlyInAnyOrder(
            State.CASE_ISSUED,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.CASE_PROGRESSION,
            State.ALL_FINAL_ORDERS_ISSUED,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.CLOSED
        );
        assertThat(configuredEvent.isShowSummary()).isTrue();
        assertThat(configuredEvent.getEndButtonLabel()).isEqualTo("Submit");
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_TEAM_LEADER))
            .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U);
        assertThat(configuredEvent.getGrants().get(UserRole.HEARING_CENTRE_ADMIN))
            .containsExactlyInAnyOrder(Permission.C, Permission.R, Permission.U);
        assertThat(configuredEvent.getGrants().get(UserRole.GA_CLAIMANT_SOLICITOR)).isEmpty();
        assertThat(configuredEvent.getGrants().get(UserRole.CTSC_ADMIN)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.CTSC_TEAM_LEADER)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.CIRCUIT_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.FEE_PAID_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.LEADERSHIP_JUDGE)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.WLU_ADMIN)).containsExactly(Permission.R);
        assertThat(configuredEvent.getGrants().get(UserRole.WLU_TEAM_LEADER)).containsExactly(Permission.R);
    }

    @Test
    void shouldConfigureCheckYourAnswersFieldsForBatchFive() {
        Map<String, Field<?, ?, ?, ?>> fields = configuredEvent.getFields().getFields().stream()
            .map(fieldBuilder -> (Field<?, ?, ?, ?>) fieldBuilder.build())
            .collect(Collectors.toMap(Field::getId, Function.identity(), (first, second) -> first));

        assertSummaryField(fields, "documentAmend_AmendedFileName");
        assertSummaryField(fields, "documentAmend_IssueDate");
        assertSummaryField(fields, "documentAmend_RelatedParty");
        assertSummaryField(fields, "documentAmend_RelatedSubmissionsDocumentType");
        assertSummaryField(fields, "documentAmend_StandaloneDocumentType");

        Field<?, ?, ?, ?> relatedSubmission = fields.get("documentAmend_RelatedSubmission");
        assertThat(relatedSubmission).isNotNull();
        assertThat(relatedSubmission.isShowSummary()).isTrue();
        assertThat(relatedSubmission.getLabel())
            .isEqualTo("Which application or counterclaim does this document relate to?");
        assertThat(relatedSubmission.getShowCondition())
            .isEqualTo("documentAmend_ShowRelatedSubmissionsList=\"YES\"");

        assertThat(fields.get("documentAmend_RelatedSubmissionsDocumentType").getShowCondition())
            .isEqualTo("documentAmend_RelatedSubmission=\"NONE\" "
                + "AND documentAmend_ShowRelatedSubmissionsList=\"YES\"");
        assertThat(fields.get("documentAmend_StandaloneDocumentType").getShowCondition())
            .isEqualTo("documentAmend_ShowRelatedSubmissionsList!=\"YES\"");
    }

    private void assertSummaryField(Map<String, Field<?, ?, ?, ?>> fields, String fieldId) {
        assertThat(fields.get(fieldId))
            .withFailMessage("Expected %s to be configured for the CYA summary", fieldId)
            .isNotNull();
        assertThat(fields.get(fieldId).isShowSummary()).isTrue();
    }

    @Test
    void shouldInitialiseDocumentAmendDetailsWhenMissingOnStart() {
        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getDocumentAmendDetails()).isNotNull();
        verify(documentSelectionService).initialise(
            TEST_CASE_REFERENCE, result, result.getDocumentAmendDetails());
    }

    @Test
    void shouldRetainExistingDocumentAmendDetailsOnStart() {
        DocumentAmendDetails existingDetails = new DocumentAmendDetails();
        PCSCase caseData = PCSCase.builder().documentAmendDetails(existingDetails).build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getDocumentAmendDetails()).isSameAs(existingDetails);
        verify(documentSelectionService).initialise(TEST_CASE_REFERENCE, result, existingDetails);
    }

    @Test
    void shouldReturnConfirmationWhenSubmitted() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .build();

        when(documentAmendService.amendDocument(caseData, TEST_CASE_REFERENCE))
            .thenReturn(new DocumentAmendService.AmendedDocument("rent statement 16042021", "Defendant One"));

        String confirmationPanelMarkup = stubConfirmationPanelBuilder();

        // When
        SubmitResponse<State> response = callSubmitHandler(caseData);

        // Then
        verify(confirmationPanelBuilder)
            .buildStandardConfirmationPanel("Document rent statement 16042021 amended", caseData, TEST_CASE_REFERENCE);

        assertThat(response.getConfirmationBody())
            .contains(confirmationPanelMarkup)
            .contains("The amended document is available to view in case file view.");
    }

    private String stubConfirmationPanelBuilder() {
        String confirmationPanelMarkup = "confirmation panel markup";
        when(confirmationPanelBuilder
                 .buildStandardConfirmationPanel(anyString(), any(PCSCase.class), anyLong()))
            .thenReturn(confirmationPanelMarkup);

        return confirmationPanelMarkup;
    }
}
