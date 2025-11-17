package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class DefendantValidatorTest {

    @Mock
    private AddressValidator addressValidator;

    private DefendantValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantValidator(addressValidator);
    }

    @Test
    void shouldValidateDefendant1AddressWithNoSectionHintWhenSingleDefendant() {
        // Given
        AddressUK defendant1Address = mock(AddressUK.class);
        DefendantDetails defendant1 = createDefendantWithAddress(defendant1Address);

        List<String> expectedValidationErrors = List.of("error 1", "error 2");
        when(addressValidator.validateAddressFields(defendant1Address, ""))
            .thenReturn(expectedValidationErrors);

        // When
        List<String> actualValidationErrors = underTest.validateDefendant1(defendant1, false);

        // Then
        assertThat(actualValidationErrors).isEqualTo(expectedValidationErrors);
    }

    @Test
    void shouldValidateDefendant1AddressWithSectionHintWhenMultipleDefendant() {
        // Given
        AddressUK defendant1Address = mock(AddressUK.class);
        DefendantDetails defendant1 = createDefendantWithAddress(defendant1Address);

        List<String> expectedValidationErrors = List.of("error 1", "error 2");
        when(addressValidator.validateAddressFields(defendant1Address, "defendant 1"))
            .thenReturn(expectedValidationErrors);

        // When
        List<String> actualValidationErrors = underTest.validateDefendant1(defendant1, true);

        // Then
        assertThat(actualValidationErrors).isEqualTo(expectedValidationErrors);
    }

    @Test
    void shouldValidateAdditionalDefendantAddressesWithSectionHints() {
        // Given
        AddressUK additionalDefendant1Address = mock(AddressUK.class);
        DefendantDetails additionalDefendant1 = createDefendantWithAddress(additionalDefendant1Address);

        AddressUK additionalDefendant2Address = mock(AddressUK.class);
        DefendantDetails additionalDefendant2 = createDefendantWithAddress(additionalDefendant2Address);

        List<ListValue<DefendantDetails>> additionalDefendants = wrapListItems(List.of(
            additionalDefendant1,
            additionalDefendant2
        ));

        String errorMessage1 = "error 1";
        String errorMessage2 = "error 2";
        String errorMessage3 = "error 3";

        when(addressValidator.validateAddressFields(additionalDefendant1Address, "additional defendant 1"))
            .thenReturn(List.of(errorMessage1));

        when(addressValidator.validateAddressFields(additionalDefendant2Address, "additional defendant 2"))
            .thenReturn(List.of(errorMessage2, errorMessage3));

        // When
        List<String> actualValidationErrors = underTest.validateAdditionalDefendants(additionalDefendants);

        // Then
        assertThat(actualValidationErrors).containsExactly(errorMessage1, errorMessage2, errorMessage3);
    }

    @Test
    void shouldHandleMissingAddressForAdditionalDefendant() {
        // Given
        DefendantDetails additionalDefendant1 = createDefendantWithAddress(null);

        List<ListValue<DefendantDetails>> additionalDefendants = wrapListItems(List.of(additionalDefendant1));

        // When
        List<String> actualValidationErrors = underTest.validateAdditionalDefendants(additionalDefendants);

        // Then
        assertThat(actualValidationErrors).containsExactly(DefendantValidator.EXUI_POFCC81_ERROR);
    }

    private static DefendantDetails createDefendantWithAddress(AddressUK address) {
        return DefendantDetails.builder()
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(address)
            .build();
    }

}
