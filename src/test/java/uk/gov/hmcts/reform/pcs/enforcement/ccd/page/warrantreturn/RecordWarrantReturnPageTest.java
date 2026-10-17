package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordWarrantReturnPageTest extends BasePageTest {

    @Mock
    private WarrantReturnService warrantReturnService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RecordWarrantReturnPage(warrantReturnService));
    }

    @Test
    void shouldAskForAMandatoryReturnOnTheRecordWarrantReturnPage() {
        Field<?, ?, ?, ?> type = field("warrantReturnType");

        assertThat(type.getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(type.getShowCondition()).isNull();
        assertThat(type.getPage()).isEqualTo("recordWarrantReturn");
        assertThat(event.getFields().getPageLabels()).containsEntry("recordWarrantReturn", "Record warrant return");
    }

    // "Final return asks for outcome and return name" / "Final return names depend on the outcome" /
    // "Interim return names".
    @Test
    void shouldShowTheOutcomeAndTheReturnNamesForTheChosenReturnAndOutcome() {
        assertMandatoryWhen("warrantReturnOutcome", "warrantReturnType=\"FINAL\"");
        assertMandatoryWhen("warrantReturnSuccessfulReturnName",
                            "warrantReturnType=\"FINAL\" AND warrantReturnOutcome=\"EVICTION_SUCCESSFUL\"");
        assertMandatoryWhen("warrantReturnUnsuccessfulReturnName",
                            "warrantReturnType=\"FINAL\" AND warrantReturnOutcome=\"*\""
                                + " AND warrantReturnOutcome!=\"EVICTION_SUCCESSFUL\"");
        assertMandatoryWhen("warrantReturnInterimReturnName", "warrantReturnType=\"INTERIM\"");
    }

    // "Successful eviction needs the eviction date and time".
    @Test
    void shouldAskForTheEvictionDateAndTimeForASuccessfulEviction() {
        assertMandatoryWhen("warrantReturnEvictionDateTime",
                            "warrantReturnType=\"FINAL\" AND warrantReturnOutcome=\"EVICTION_SUCCESSFUL\"");
    }

    @Test
    void shouldAlwaysShowAdditionalDetailsAndCheckThemInTheMidEvent() {
        Field<?, ?, ?, ?> additionalDetails = field("warrantReturnAdditionalDetails");

        assertThat(additionalDetails.getContext()).isEqualTo(DisplayContext.Optional);
        assertThat(additionalDetails.getShowCondition()).isNull();
        assertThat(event.getFields().getPagesToMidEvent()).containsOnlyKeys("recordWarrantReturn");
    }

    @Test
    void shouldShowTheEvictionDetailsWithoutAskingForThem() {
        Field<?, ?, ?, ?> markup = field("warrantReturnEvictionDetailsMarkup");

        assertThat(markup.getContext()).isEqualTo(DisplayContext.ReadOnly);
        assertThat(markup.getShowCondition()).isEqualTo("[STATE]=\"NEVER_SHOW\"");
        assertThat(field("recordWarrantReturn-eviction-details").getLabel())
            .isEqualTo("${warrantReturnEvictionDetailsMarkup}");
    }

    @Test
    void shouldReturnTheValidationErrors() {
        WarrantReturnDetails details = WarrantReturnDetails.builder().type(WarrantReturnType.INTERIM).build();
        when(warrantReturnService.validate(TEST_CASE_REFERENCE, details)).thenReturn(List.of("Select a return name"));

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseWith(details));

        assertThat(response.getErrors()).containsExactly("Select a return name");
    }

    @Test
    void shouldContinueWhenTheAnswersAreValid() {
        WarrantReturnDetails details = WarrantReturnDetails.builder().type(WarrantReturnType.INTERIM).build();
        PCSCase caseData = caseWith(details);
        when(warrantReturnService.validate(TEST_CASE_REFERENCE, details)).thenReturn(List.of());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getData()).isSameAs(caseData);
    }

    @Test
    void shouldReturnAnErrorWhenTheWarrantCannotTakeAReturn() {
        when(warrantReturnService.validate(TEST_CASE_REFERENCE, null)).thenThrow(new NoEnforcementException());

        assertThat(callMidEventHandler(PCSCase.builder().build()).getErrors())
            .containsExactly("There is no warrant on this case");
    }

    @Test
    void shouldReturnAnErrorWhenNoBailiffIsAllocated() {
        when(warrantReturnService.validate(TEST_CASE_REFERENCE, null)).thenThrow(
            new EnforcementEventNotAvailableException(WarrantReturnService.NOT_ALLOCATED_MESSAGE));

        assertThat(callMidEventHandler(PCSCase.builder().build()).getErrors())
            .containsExactly(WarrantReturnService.NOT_ALLOCATED_MESSAGE);
    }

    private void assertMandatoryWhen(String fieldId, String showCondition) {
        Field<?, ?, ?, ?> field = field(fieldId);
        assertThat(field.getContext()).as(fieldId).isEqualTo(DisplayContext.Mandatory);
        assertThat(field.getShowCondition()).as(fieldId).isEqualTo(showCondition);
    }

    private static PCSCase caseWith(WarrantReturnDetails details) {
        return PCSCase.builder().enforcementOrder(EnforcementOrder.builder().warrantReturn(details).build()).build();
    }

    private Field<?, ?, ?, ?> field(String id) {
        return allFields().filter(field -> id.equals(field.getId())).findFirst()
            .orElseThrow(() -> new AssertionError("No field " + id));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Stream<Field<?, ?, ?, ?>> allFields() {
        Stream<FieldCollection> complexFields = event.getFields().getComplexFields().stream()
            .map(builder -> ((FieldCollectionBuilder) builder).build());
        return Stream.concat(Stream.of(event.getFields()), complexFields)
            .flatMap(collection -> ((FieldCollection) collection).getFields().stream())
            .map(builder -> (Field<?, ?, ?, ?>) ((Field.FieldBuilder) builder).build());
    }
}
