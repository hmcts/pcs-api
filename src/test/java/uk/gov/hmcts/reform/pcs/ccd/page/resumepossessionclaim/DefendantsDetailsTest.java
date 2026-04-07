package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
        assertThat(response.getErrorMessageOverride()).isEqualTo("error 1\nerror 2");
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
        assertThat(response.getErrorMessageOverride()).isEqualTo("%s\n%s\n%s",
                                                                 errorMessage1, errorMessage2, errorMessage3);
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
            argumentSet("No additional defendants", VerticalYesNo.NO, "defendant's"),
            argumentSet("Additional defendants", VerticalYesNo.YES, "defendants'")
        );
    }

    @Nested
    class ClearFieldsLogicTests {

        @Test
        void shouldClearPrimaryDefendantNameFieldsWhenNameUnknown() {
            // Given
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .firstName("John")
                .lastName("Doe")
                .build();

            PCSCase caseData = PCSCase.builder()
                .defendant1(defendant1)
                .defendantCircumstances(new DefendantCircumstances())
                .addAnotherDefendant(VerticalYesNo.NO)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getClearFields()).isNotNull();
            assertThat(response.getData().getClearFields()).contains(
                "defendant1.firstName",
                "defendant1.lastName"
            );
        }

        @Test
        void shouldClearPrimaryDefendantAddressFieldsWhenAddressUnknown() {
            // Given
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .addressKnown(VerticalYesNo.NO)
                .build();

            PCSCase caseData = PCSCase.builder()
                .defendant1(defendant1)
                .defendantCircumstances(new DefendantCircumstances())
                .addAnotherDefendant(VerticalYesNo.NO)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getClearFields()).isNotNull();
            assertThat(response.getData().getClearFields()).contains(
                "defendant1.addressSameAsPossession",
                "defendant1.correspondenceAddress"
            );
        }

        @Test
        void shouldClearPrimaryDefendantCorrespondenceAddressWhenSameAsPossession() {
            // Given
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsPossession(VerticalYesNo.YES)
                .build();

            PCSCase caseData = PCSCase.builder()
                .defendant1(defendant1)
                .defendantCircumstances(new DefendantCircumstances())
                .addAnotherDefendant(VerticalYesNo.NO)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getClearFields()).isNotNull();
            assertThat(response.getData().getClearFields()).contains(
                "defendant1.correspondenceAddress"
            );
            assertThat(response.getData().getClearFields()).doesNotContain(
                "defendant1.addressSameAsPossession"
            );
        }

        @Test
        void shouldClearAdditionalDefendantsArrayWhenNotNeeded() {
            // Given
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            DefendantDetails additionalDefendant = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Jane")
                .lastName("Smith")
                .build();

            List<ListValue<DefendantDetails>> additionalDefendants = wrapListItems(List.of(additionalDefendant));

            PCSCase caseData = PCSCase.builder()
                .defendant1(defendant1)
                .defendantCircumstances(new DefendantCircumstances())
                .addAnotherDefendant(VerticalYesNo.NO)
                .additionalDefendants(additionalDefendants)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getClearFields()).isNotNull();
            assertThat(response.getData().getClearFields()).contains("additionalDefendants");
        }

        @Test
        void shouldClearAdditionalDefendantNameFieldsDirectly() {
            // Given
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            DefendantDetails additionalDefendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .firstName("ShouldBeCleared")
                .lastName("ShouldBeCleared")
                .build();

            DefendantDetails additionalDefendant2 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Jane")
                .lastName("Smith")
                .build();

            List<ListValue<DefendantDetails>> additionalDefendants = wrapListItems(List.of(
                additionalDefendant1,
                additionalDefendant2
            ));

            PCSCase caseData = PCSCase.builder()
                .defendant1(defendant1)
                .defendantCircumstances(new DefendantCircumstances())
                .addAnotherDefendant(VerticalYesNo.YES)
                .additionalDefendants(additionalDefendants)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then - First additional defendant should have names cleared
            assertThat(response.getData().getAdditionalDefendants().get(0).getValue().getFirstName()).isNull();
            assertThat(response.getData().getAdditionalDefendants().get(0).getValue().getLastName()).isNull();

            // Second additional defendant should keep names
            assertThat(response.getData().getAdditionalDefendants().get(1).getValue().getFirstName()).isEqualTo("Jane");
            assertThat(response.getData().getAdditionalDefendants().get(1).getValue().getLastName()).isEqualTo("Smith");
        }

        @Test
        void shouldClearAdditionalDefendantAddressFieldsDirectly() {
            // Given
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("John")
                .lastName("Doe")
                .build();

            DefendantDetails additionalDefendant = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Jane")
                .lastName("Smith")
                .addressKnown(VerticalYesNo.NO)
                .addressSameAsPossession(VerticalYesNo.YES)
                .build();

            List<ListValue<DefendantDetails>> additionalDefendants = wrapListItems(List.of(additionalDefendant));

            PCSCase caseData = PCSCase.builder()
                .defendant1(defendant1)
                .defendantCircumstances(new DefendantCircumstances())
                .addAnotherDefendant(VerticalYesNo.YES)
                .additionalDefendants(additionalDefendants)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then
            assertThat(response.getData().getAdditionalDefendants().get(0).getValue()
                .getAddressSameAsPossession()).isNull();
            assertThat(response.getData().getAdditionalDefendants().get(0).getValue()
                .getCorrespondenceAddress()).isNull();
        }

        @Test
        void shouldHandleMixAndMatchScenarios() {
            // Given - Primary defendant: name unknown, address unknown
            //        Additional defendant 1: name known, address same as possession
            //        Additional defendant 2: name unknown, address known but different
            DefendantDetails defendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .firstName("ShouldClear")
                .lastName("ShouldClear")
                .addressKnown(VerticalYesNo.NO)
                .build();

            DefendantDetails additionalDefendant1 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("Jane")
                .lastName("Smith")
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsPossession(VerticalYesNo.YES)
                .build();

            DefendantDetails additionalDefendant2 = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .firstName("ClearMe")
                .lastName("ClearMe")
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsPossession(VerticalYesNo.NO)
                .build();

            List<ListValue<DefendantDetails>> additionalDefendants = wrapListItems(List.of(
                additionalDefendant1,
                additionalDefendant2
            ));

            PCSCase caseData = PCSCase.builder()
                .defendant1(defendant1)
                .defendantCircumstances(new DefendantCircumstances())
                .addAnotherDefendant(VerticalYesNo.YES)
                .additionalDefendants(additionalDefendants)
                .build();

            // When
            AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

            // Then - Primary defendant clearFields
            assertThat(response.getData().getClearFields()).contains(
                "defendant1.firstName",
                "defendant1.lastName",
                "defendant1.addressSameAsPossession",
                "defendant1.correspondenceAddress"
            );

            // Additional defendant 1 - keep names, clear correspondence address
            assertThat(response.getData().getAdditionalDefendants().get(0).getValue()
                .getFirstName()).isEqualTo("Jane");
            assertThat(response.getData().getAdditionalDefendants().get(0).getValue()
                .getLastName()).isEqualTo("Smith");
            assertThat(response.getData().getAdditionalDefendants().get(0).getValue()
                .getCorrespondenceAddress()).isNull();

            // Additional defendant 2 - clear names, keep address fields
            assertThat(response.getData().getAdditionalDefendants().get(1).getValue().getFirstName()).isNull();
            assertThat(response.getData().getAdditionalDefendants().get(1).getValue().getLastName()).isNull();
            assertThat(response.getData().getAdditionalDefendants().get(1).getValue().getAddressSameAsPossession())
                .isEqualTo(VerticalYesNo.NO);
        }
    }

}
