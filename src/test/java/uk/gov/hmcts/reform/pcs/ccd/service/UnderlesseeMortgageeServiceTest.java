package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgagee;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class UnderlesseeMortgageeServiceTest {

    @Mock(strictness = LENIENT)
    private PCSCase pcsCase;

    private UnderlesseeMortgageeService underTest;

    @BeforeEach
    void setUp() {
        underTest = new UnderlesseeMortgageeService();
    }

    @Test
    void shouldThrowExceptionForNullUnderlesseeMortgagee() {
        // Given
        when(pcsCase.getUnderlesseeMortgagee1()).thenReturn(null);

        // When
        Throwable throwable = catchThrowable(() -> underTest.buildUnderlesseeMortgageeList(pcsCase));

        // Then
        assertThat(throwable)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("First underlessee or mortgagee must be provided");
    }

    @ParameterizedTest
    @MethodSource("singleUnderlesseeOrMortgageeScenarios")
    void shouldBuildListWithSingleUnderlesseeOrMortgagee(UnderlesseeMortgageeDetails underlesseeMortgageeDetails,
                                                         UnderlesseeMortgagee expectedUnderlesseeMortgagee) {
        // Given
        when(pcsCase.getUnderlesseeMortgagee1()).thenReturn(underlesseeMortgageeDetails);
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);

        // When
        List<UnderlesseeMortgagee> underlesseeMortgageeList = underTest.buildUnderlesseeMortgageeList(pcsCase);

        // Then
        assertThat(underlesseeMortgageeList).containsExactly(expectedUnderlesseeMortgagee);
    }

    @Test
    void shouldBuildListWithMultipleUnderlesseesOrMortgagees() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);

        UnderlesseeMortgageeDetails mortgagee1 = builduUnderlesseeMortgageeDetails(
            VerticalYesNo.YES, "Mortgagee1 name", VerticalYesNo.YES,correspondenceAddress);


        UnderlesseeMortgageeDetails underlessee1 = builduUnderlesseeMortgageeDetails(
             VerticalYesNo.YES,"Underlessee1 name", VerticalYesNo.YES, correspondenceAddress);


        UnderlesseeMortgageeDetails underlessee2 = builduUnderlesseeMortgageeDetails(
             VerticalYesNo.YES,"Underlessee2 name", VerticalYesNo.NO, correspondenceAddress);


        when(pcsCase.getUnderlesseeMortgagee1()).thenReturn(mortgagee1);

        List<UnderlesseeMortgageeDetails> additionalUnderlesseeMortgagee = List.of(underlessee1, underlessee2);
        when(pcsCase.getAdditionalUnderlesseeMortgagee()).thenReturn(wrapListItems(additionalUnderlesseeMortgagee));
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);

        // When
        List<UnderlesseeMortgagee> defendantList = underTest.buildUnderlesseeMortgageeList(pcsCase);

        // Then
        UnderlesseeMortgagee expectedMortgagee1 = buildExpectedUnderlesseeMortgagee(
            true, "Mortgagee1 name",true, correspondenceAddress);

        UnderlesseeMortgagee expectedUnderlessee1 =  buildExpectedUnderlesseeMortgagee(
            true, "Underlessee1 name",true, correspondenceAddress);

        UnderlesseeMortgagee expectedUnderlessee2 =  buildExpectedUnderlesseeMortgagee(
            true, "Underlessee2 name",false, null);

        assertThat(defendantList).containsExactly(expectedMortgagee1, expectedUnderlessee1, expectedUnderlessee2);
    }

    @Test
    void shouldIgnoreMultipleUnderlesseesOrMortgageesIfAdditionalFlagIsFalse() {
        // Given
        AddressUK address = mock(AddressUK.class);

        UnderlesseeMortgageeDetails mortgagee1 = builduUnderlesseeMortgageeDetails(
             VerticalYesNo.YES,"Mortgagee1 name", VerticalYesNo.YES, address);

        UnderlesseeMortgageeDetails underlessee1 = builduUnderlesseeMortgageeDetails(
             VerticalYesNo.YES, "Underlessee1 name",VerticalYesNo.NO, null);

        when(pcsCase.getUnderlesseeMortgagee1()).thenReturn(mortgagee1);
        when(pcsCase.getAdditionalUnderlesseeMortgagee())
            .thenReturn(wrapListItems(List.of(underlessee1)));
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);

        // When
        List<UnderlesseeMortgagee> result = underTest.buildUnderlesseeMortgageeList(pcsCase);

        // Then
        UnderlesseeMortgagee expected = buildExpectedUnderlesseeMortgagee(
            true, "Mortgagee1 name", true, address);

        assertThat(result).containsExactly(expected);
    }

    @Test
    void shouldReturnBlankIfNotKnownIsSelected() {
        // Given
        AddressUK address = mock(AddressUK.class);

        UnderlesseeMortgageeDetails mortgagee1 = builduUnderlesseeMortgageeDetails(
            VerticalYesNo.NO,"ignored",  VerticalYesNo.NO, address);

        UnderlesseeMortgageeDetails underlessee1 = builduUnderlesseeMortgageeDetails(
             VerticalYesNo.NO,"ignored", VerticalYesNo.NO, address);

        when(pcsCase.getUnderlesseeMortgagee1()).thenReturn(mortgagee1);
        when(pcsCase.getAdditionalUnderlesseeMortgagee())
            .thenReturn(wrapListItems(List.of(underlessee1)));
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);

        // When
        List<UnderlesseeMortgagee> result = underTest.buildUnderlesseeMortgageeList(pcsCase);

        // Then
        UnderlesseeMortgagee expected1 = buildExpectedUnderlesseeMortgagee(
            false, null, false, null);
        UnderlesseeMortgagee expected2 = buildExpectedUnderlesseeMortgagee(
            false, null, false, null);

        assertThat(result).containsExactly(expected1, expected2);
    }

    private static Stream<Arguments> singleUnderlesseeOrMortgageeScenarios() {

        AddressUK correspondenceAddress = mock(AddressUK.class);

        return Stream.of(
            // Name and address not known
            Arguments.of(
                builduUnderlesseeMortgageeDetails(VerticalYesNo.NO,null, VerticalYesNo.NO, null),
                buildExpectedUnderlesseeMortgagee(false, null, false, null)
            ),

            // Name known and address not known
            Arguments.of(
                builduUnderlesseeMortgageeDetails(
                     VerticalYesNo.YES,"expected name", VerticalYesNo.NO, null),
                buildExpectedUnderlesseeMortgagee(
                    true, "expected name", false, null)
            ),

            // Name not known and address known
            Arguments.of(
                builduUnderlesseeMortgageeDetails(
                     VerticalYesNo.NO,null, VerticalYesNo.YES, correspondenceAddress),
                buildExpectedUnderlesseeMortgagee(
                    false, null, true, correspondenceAddress)
            )
        );
    }

    private static UnderlesseeMortgageeDetails builduUnderlesseeMortgageeDetails(VerticalYesNo nameKnown,String name,
                                                                          VerticalYesNo addressKnown,
                                                                          AddressUK address) {
        return UnderlesseeMortgageeDetails.builder()
            .underlesseeOrMortgageeNameKnown(nameKnown)
            .underlesseeOrMortgageeName(name)
            .underlesseeOrMortgageeAddressKnown(addressKnown)
            .underlesseeOrMortgageeAddress(address)
            .build();
    }

    private static UnderlesseeMortgagee buildExpectedUnderlesseeMortgagee(boolean nameKnown,
                                                                          String name,
                                                                          boolean addressKnown,
                                                                          AddressUK address) {
        return UnderlesseeMortgagee.builder()
            .underlesseeOrMortgageeNameKnown(nameKnown)
            .underlesseeOrMortgageeName(name)
            .underlesseeOrMortgageeAddressKnown(addressKnown)
            .underlesseeOrMortgageeAddress(address)
            .build();
    }

}
