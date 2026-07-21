package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.documentamend.AmendDocumentDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.documentamend.SelectDocumentPage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.time.Clock;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmendDocumentsTest extends BaseEventTest {

    @Mock
    private DocumentSelectionService documentSelectionService;
    @Mock
    private DocumentAmendService documentAmendService;
    @Mock
    private AddressFormatter addressFormatter;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new AmendDocuments(
            documentSelectionService,
            documentAmendService,
            addressFormatter,
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
        assertThat(configuredEvent.getGrants().get(UserRole.PCS_SOLICITOR)).isEmpty();
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
    void shouldReturnConfirmationWhenSubmitted() {
        AddressUK propertyAddress = AddressUK.builder()
            .addressLine1("1 Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        when(documentAmendService.amendDocument(caseData, TEST_CASE_REFERENCE))
            .thenReturn(new DocumentAmendService.AmendedDocument("rent statement 16042021", "Defendant One"));
        when(addressFormatter.formatMediumAddress(propertyAddress, AddressFormatter.COMMA_DELIMITER))
            .thenReturn("1 Street, London, SW1A 1AA");

        SubmitResponse<State> response = callSubmitHandler(caseData);

        assertThat(response.getConfirmationBody())
            .contains("Document rent statement 16042021 amended")
            .contains("Case number #1234")
            .contains("1 Street, London, SW1A 1AA")
            .contains("Defendant One")
            .contains("The amended document is available to view in case file view.");
    }
}
