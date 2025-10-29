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
    void shouldValidateDefendant1AddressWithNoSectionHintWhenSingleDefendant() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);
        UnderlesseeMortgageeDetails underlesseeMortgagee1 = buildUnderlesseeMortgageeDetails(correspondenceAddress);

        List<String> expectedValidationErrors = List.of("some error 1", "some error 2");
        when(addressValidator.validateAddressFields(correspondenceAddress, ""))
            .thenReturn(expectedValidationErrors);

        // When
        List<String> actualValidationErrors = underTest.validateUnderlesseeOrMortgagee1(underlesseeMortgagee1,
                                                                                        false);

        // Then
        assertThat(actualValidationErrors).isEqualTo(expectedValidationErrors);
    }

    @Test
    void shouldValidateUnderlesseeMortgagee1AddressWithSectionHintWhenMultiple() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);
        UnderlesseeMortgageeDetails underlesseeMortgagee1 = buildUnderlesseeMortgageeDetails(correspondenceAddress);

        List<String> expectedValidationErrors = List.of("some error 1", "some error 2");
        when(addressValidator.validateAddressFields(correspondenceAddress, "Underlessee or mortgagee 1"))
            .thenReturn(expectedValidationErrors);

        // When
        List<String> actualValidationErrors = underTest.validateUnderlesseeOrMortgagee1(underlesseeMortgagee1,
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

        List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeMortgagee = wrapListItems(List.of(
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
            underTest.validateAdditionalUnderlesseeOrMortgagee(additionalUnderlesseeMortgagee);

        // Then
        assertThat(actualValidationErrors).containsExactly(errorMessage1, errorMessage2, errorMessage3);
    }

    private static UnderlesseeMortgageeDetails buildUnderlesseeMortgageeDetails(AddressUK address) {
        return UnderlesseeMortgageeDetails.builder()
            .underlesseeOrMortgageeAddressKnown(VerticalYesNo.YES)
            .underlesseeOrMortgageeAddress(address)
            .build();
    }

}
