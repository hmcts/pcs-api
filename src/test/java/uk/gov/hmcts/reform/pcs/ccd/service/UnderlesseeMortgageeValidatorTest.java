package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.service.UnderlesseeMortgageeValidator.EXUI_POFCC81_ERROR;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class UnderlesseeMortgageeValidatorTest {

    @Mock
    private AddressValidator addressValidator;

    private UnderlesseeMortgageeValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new UnderlesseeMortgageeValidator(addressValidator);
    }

    @Test
    void shouldValidateUnderlesseeOrMortgagee1AddressWithNoSectionHintWhenSingle() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);
        UnderlesseeMortgageeDetails underlesseeOrMortgagee1 = buildUnderlesseeMortgageeDetails(correspondenceAddress);

        List<String> expectedValidationErrors = List.of("some error 1", "some error 2");
        when(addressValidator.validateAddressFields(correspondenceAddress, ""))
            .thenReturn(expectedValidationErrors);

        // When
        List<String> actualValidationErrors = underTest.validateUnderlesseeOrMortgagee1(
            underlesseeOrMortgagee1,
            false);

        // Then
        assertThat(actualValidationErrors).isEqualTo(expectedValidationErrors);
    }

    @Test
    void shouldValidateUnderlesseeOrMortgagee1AddressWithSectionHintWhenMultiple() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);
        UnderlesseeMortgageeDetails underlesseeOrMortgagee1 = buildUnderlesseeMortgageeDetails(correspondenceAddress);

        List<String> expectedValidationErrors = List.of("some error 1", "some error 2");
        when(addressValidator.validateAddressFields(correspondenceAddress, "Underlessee or mortgagee 1"))
            .thenReturn(expectedValidationErrors);

        // When
        List<String> actualValidationErrors = underTest.validateUnderlesseeOrMortgagee1(
            underlesseeOrMortgagee1,
            true);

        // Then
        assertThat(actualValidationErrors).isEqualTo(expectedValidationErrors);
    }

    @Test
    void shouldValidateAdditionalUnderlesseeMortgageeAddressesWithSectionHints() {
        // Given
        AddressUK additionalUnderlesseeAddress = mock(AddressUK.class);
        UnderlesseeMortgageeDetails additionalUnderlessee =
            buildUnderlesseeMortgageeDetails(additionalUnderlesseeAddress);

        AddressUK additionalMortgageeAddress = mock(AddressUK.class);
        UnderlesseeMortgageeDetails additionalMortgagee = buildUnderlesseeMortgageeDetails(additionalMortgageeAddress);

        List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeOrMortgagee = wrapListItems(List.of(
            additionalUnderlessee,
            additionalMortgagee
        ));

        String errorMessage1 = "some error 1";
        String errorMessage2 = "some error 2";
        String errorMessage3 = "some error 3";

        when(addressValidator.validateAddressFields(additionalUnderlesseeAddress,
                                                    "additional underlessee or mortgagee 1"))
            .thenReturn(List.of(errorMessage1));

        when(addressValidator.validateAddressFields(additionalMortgageeAddress,
                                                    "additional underlessee or mortgagee 2"))
            .thenReturn(List.of(errorMessage2, errorMessage3));

        // When
        List<String> actualValidationErrors =
            underTest.validateAdditionalUnderlesseeOrMortgagee(additionalUnderlesseeOrMortgagee);

        // Then
        assertThat(actualValidationErrors).containsExactly(errorMessage1, errorMessage2, errorMessage3);
    }

    @Test
    void shouldHandleMissingAddressForAdditionalDefendant() {
        // Given
        UnderlesseeMortgageeDetails additionalMortgagee = buildUnderlesseeMortgageeDetails(null);

        List<ListValue<UnderlesseeMortgageeDetails>> additionalDefendants = wrapListItems(List.of(additionalMortgagee));

        // When
        List<String> actualValidationErrors = underTest.validateAdditionalUnderlesseeOrMortgagee(additionalDefendants);

        // Then
        assertThat(actualValidationErrors).containsExactly(EXUI_POFCC81_ERROR);
    }

    private static UnderlesseeMortgageeDetails buildUnderlesseeMortgageeDetails(AddressUK address) {
        return UnderlesseeMortgageeDetails.builder()
            .addressKnown(VerticalYesNo.YES)
            .address(address)
            .build();
    }

}
