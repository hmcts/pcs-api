package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantValidator;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class DefendantsDetailsTest extends BasePageTest {

    @Mock
    private DefendantValidator defendantValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new DefendantsDetails(defendantValidator));
    }

    @Test
    void shouldValidateSingleDefendant() {
        // Given
        DefendantCircumstances defendantCircumstances = mock(DefendantCircumstances.class);
        DefendantDetails defendant1 = mock(DefendantDetails.class);

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .defendantCircumstances(defendantCircumstances)
            .addAnotherDefendant(VerticalYesNo.NO)
            .build();

        List<String> expectedValidationErrors = List.of("error 1", "error 2");
        when(defendantValidator.validateDefendant1(defendant1, false))
            .thenReturn(expectedValidationErrors);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEqualTo(expectedValidationErrors);
        verify(defendantValidator, never()).validateAdditionalDefendants(anyList());
    }

    @Test
    void shouldValidateMultipleDefendants() {
        // Given
        DefendantCircumstances defendantCircumstances = mock(DefendantCircumstances.class);
        DefendantDetails defendant1 = mock(DefendantDetails.class);
        DefendantDetails additionalDefendant1 = mock(DefendantDetails.class);
        DefendantDetails additionalDefendant2 = mock(DefendantDetails.class);

        List<ListValue<DefendantDetails>> additionalDefendants = wrapListItems(List.of(
            additionalDefendant1,
            additionalDefendant2
        ));

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .defendantCircumstances(defendantCircumstances)
            .addAnotherDefendant(VerticalYesNo.YES)
            .additionalDefendants(additionalDefendants)
            .build();

        String errorMessage1 = "error 1";
        String errorMessage2 = "error 2";
        String errorMessage3 = "error 3";

        when(defendantValidator.validateDefendant1(defendant1, true))
            .thenReturn(List.of(errorMessage1, errorMessage2));

        when(defendantValidator.validateAdditionalDefendants(additionalDefendants))
            .thenReturn(List.of(errorMessage3));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(errorMessage1, errorMessage2, errorMessage3);
    }

    @ParameterizedTest
    @MethodSource("defendantTermScenarios")
    void shouldSetDefendantTermPossessive(VerticalYesNo additionalDefendants, String expectedTermPossessive) {
        // Given
        DefendantCircumstances defendantCircumstances = new DefendantCircumstances();
        DefendantDetails defendant1 = mock(DefendantDetails.class);

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .defendantCircumstances(defendantCircumstances)
            .addAnotherDefendant(additionalDefendants)
            .additionalDefendants(List.of())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()
                       .getDefendantCircumstances().getDefendantTermPossessive()).isEqualTo(expectedTermPossessive);
    }

    private static Stream<Arguments> defendantTermScenarios() {
        return Stream.of(
            argumentSet("No additional defendants", VerticalYesNo.NO, "defendant’s"),
            argumentSet("Additional defendants", VerticalYesNo.YES, "defendants’")
        );
    }

}
