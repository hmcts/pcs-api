package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class DefendantsDetailsTest extends BasePageTest {

    @Mock
    private AddressValidator addressValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new DefendantsDetails(addressValidator));
    }

    @Test
    void shouldValidateAddressWithNoSectionHintWhenSingleDefendant() {
        // Given
        DefendantCircumstances defendantCircumstances = mock(DefendantCircumstances.class);
        AddressUK defendant1Address = mock(AddressUK.class);
        DefendantDetails defendant1 = createDefendantWithAddress(defendant1Address);

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .defendantCircumstances(defendantCircumstances)
            .addAnotherDefendant(VerticalYesNo.NO)
            .build();

        List<String> expectedValidationErrors = List.of("error 1", "error 2");
        when(addressValidator.validateAddressFields(defendant1Address, ""))
            .thenReturn(expectedValidationErrors);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isEqualTo(expectedValidationErrors);
    }

    @Test
    void shouldValidateAddressesWithSectionHintsWhenAdditionalDefendants() {
        // Given
        DefendantCircumstances defendantCircumstances = mock(DefendantCircumstances.class);
        AddressUK defendant1Address = mock(AddressUK.class);
        DefendantDetails defendant1 = createDefendantWithAddress(defendant1Address);

        AddressUK additionalDefendant1Address = mock(AddressUK.class);
        DefendantDetails additionalDefendant1 = createDefendantWithAddress(additionalDefendant1Address);

        AddressUK additionalDefendant2Address = mock(AddressUK.class);
        DefendantDetails additionalDefendant2 = createDefendantWithAddress(additionalDefendant2Address);

        PCSCase caseData = PCSCase.builder()
            .defendant1(defendant1)
            .defendantCircumstances(defendantCircumstances)
            .addAnotherDefendant(VerticalYesNo.YES)
            .additionalDefendants(wrapListItems(List.of(additionalDefendant1, additionalDefendant2)))
            .build();

        String errorMessage1 = "error 1";
        String errorMessage2 = "error 2";
        String errorMessage3 = "error 3";
        String errorMessage4 = "error 4";
        when(addressValidator.validateAddressFields(defendant1Address, "defendant 1"))
            .thenReturn(List.of(errorMessage1, errorMessage2));

        when(addressValidator.validateAddressFields(additionalDefendant1Address, "additional defendant 1"))
            .thenReturn(List.of(errorMessage3));

        when(addressValidator.validateAddressFields(additionalDefendant2Address, "additional defendant 2"))
            .thenReturn(List.of(errorMessage4));

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly(errorMessage1, errorMessage2, errorMessage3, errorMessage4);
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

    private static DefendantDetails createDefendantWithAddress(AddressUK address) {
        return DefendantDetails.builder()
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(address)
            .build();
    }

}
