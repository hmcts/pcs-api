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
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.model.UnderlesseeMortgagee;

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
        when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(null);

        // When
        Throwable throwable = catchThrowable(() -> underTest.buildUnderlesseeMortgageeList(pcsCase));

        // Then
        assertThat(throwable)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("First underlessee or mortgagee must be provided");
    }

    @ParameterizedTest
    @MethodSource("singleUnderlesseeOrMortgageeScenarios")
    void shouldBuildListWithSingleUnderlesseeOrMortgagee(UnderlesseeMortgageeDetails underlesseeOrMortgagee1,
                                                         UnderlesseeMortgagee expectedUnderlesseeOrMortgagee) {
        // Given
        when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(underlesseeOrMortgagee1);
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);

        // When
        List<UnderlesseeMortgagee> underlesseeMortgageeList = underTest.buildUnderlesseeMortgageeList(pcsCase);

        // Then
        assertThat(underlesseeMortgageeList).containsExactly(expectedUnderlesseeOrMortgagee);
    }

    @Test
    void shouldBuildListWithMultipleUnderlesseesOrMortgagees() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);

        UnderlesseeMortgageeDetails mortgagee1 = buildUnderlesseeOrMortgageeDetails(
            VerticalYesNo.YES, "Mortgagee1 name", VerticalYesNo.YES,correspondenceAddress);


        UnderlesseeMortgageeDetails underlessee1 = buildUnderlesseeOrMortgageeDetails(
             VerticalYesNo.YES,"Underlessee1 name", VerticalYesNo.YES, correspondenceAddress);


        UnderlesseeMortgageeDetails underlessee2 = buildUnderlesseeOrMortgageeDetails(
             VerticalYesNo.YES,"Underlessee2 name", VerticalYesNo.NO, correspondenceAddress);


        when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(mortgagee1);

        List<UnderlesseeMortgageeDetails> additionalUnderlesseeOrMortgagee = List.of(underlessee1, underlessee2);
        when(pcsCase.getAdditionalUnderlesseeOrMortgagee()).thenReturn(wrapListItems(additionalUnderlesseeOrMortgagee));
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);

        // When
        List<UnderlesseeMortgagee> underlesseeMortgageeList = underTest.buildUnderlesseeMortgageeList(pcsCase);

        // Then
        UnderlesseeMortgagee expectedMortgagee1 = buildExpectedUnderlesseeOrMortgagee(
            true, "Mortgagee1 name",true, correspondenceAddress,true);

        UnderlesseeMortgagee expectedUnderlessee1 =  buildExpectedUnderlesseeOrMortgagee(
            true, "Underlessee1 name",true, correspondenceAddress,null);

        UnderlesseeMortgagee expectedUnderlessee2 =  buildExpectedUnderlesseeOrMortgagee(
            true, "Underlessee2 name",false, null,null);

        assertThat(underlesseeMortgageeList)
            .containsExactly(expectedMortgagee1, expectedUnderlessee1, expectedUnderlessee2);
    }

    @Test
    void shouldIgnoreMultipleUnderlesseesOrMortgageesIfAdditionalFlagIsFalse() {
        // Given
        AddressUK address = mock(AddressUK.class);

        UnderlesseeMortgageeDetails mortgagee1 = buildUnderlesseeOrMortgageeDetails(
             VerticalYesNo.YES,"Mortgagee1 name", VerticalYesNo.YES, address);

        UnderlesseeMortgageeDetails underlessee1 = buildUnderlesseeOrMortgageeDetails(
             VerticalYesNo.YES, "Underlessee1 name",VerticalYesNo.NO, null);

        when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(mortgagee1);
        when(pcsCase.getAdditionalUnderlesseeOrMortgagee())
            .thenReturn(wrapListItems(List.of(underlessee1)));
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);

        // When
        List<UnderlesseeMortgagee> result = underTest.buildUnderlesseeMortgageeList(pcsCase);

        // Then
        UnderlesseeMortgagee expected = buildExpectedUnderlesseeOrMortgagee(
            true, "Mortgagee1 name", true, address,false);

        assertThat(result).containsExactly(expected);
    }

    @Test
    void shouldReturnBlankIfNotKnownIsSelected() {
        // Given
        AddressUK address = mock(AddressUK.class);

        UnderlesseeMortgageeDetails mortgagee1 = buildUnderlesseeOrMortgageeDetails(
            VerticalYesNo.NO,"ignored",  VerticalYesNo.NO, address);

        UnderlesseeMortgageeDetails underlessee1 = buildUnderlesseeOrMortgageeDetails(
             VerticalYesNo.NO,"ignored", VerticalYesNo.NO, address);

        when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(mortgagee1);
        when(pcsCase.getAdditionalUnderlesseeOrMortgagee())
            .thenReturn(wrapListItems(List.of(underlessee1)));
        when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);

        // When
        List<UnderlesseeMortgagee> result = underTest.buildUnderlesseeMortgageeList(pcsCase);

        // Then
        UnderlesseeMortgagee expected1 = buildExpectedUnderlesseeOrMortgagee(
            false, null, false, null,true);
        UnderlesseeMortgagee expected2 = buildExpectedUnderlesseeOrMortgagee(
            false, null, false, null,null);

        assertThat(result).containsExactly(expected1, expected2);
    }

    private static Stream<Arguments> singleUnderlesseeOrMortgageeScenarios() {

        AddressUK correspondenceAddress = mock(AddressUK.class);

        return Stream.of(
            // Name and address not known
            Arguments.of(
                buildUnderlesseeOrMortgageeDetails(VerticalYesNo.NO, null, VerticalYesNo.NO, null),
                buildExpectedUnderlesseeOrMortgagee(false, null, false, null,false)
            ),

            // Name known and address not known
            Arguments.of(
                buildUnderlesseeOrMortgageeDetails(
                     VerticalYesNo.YES,"expected name", VerticalYesNo.NO, null),
                buildExpectedUnderlesseeOrMortgagee(
                    true, "expected name", false, null,false)
            ),

            // Name not known and address known
            Arguments.of(
                buildUnderlesseeOrMortgageeDetails(
                     VerticalYesNo.NO,null, VerticalYesNo.YES, correspondenceAddress),
                buildExpectedUnderlesseeOrMortgagee(
                    false, null, true, correspondenceAddress,false)
            )
        );
    }

    private static UnderlesseeMortgageeDetails buildUnderlesseeOrMortgageeDetails(VerticalYesNo nameKnown, String name,
                                                                                  VerticalYesNo addressKnown,
                                                                                  AddressUK address) {
        return UnderlesseeMortgageeDetails.builder()
            .nameKnown(nameKnown)
            .name(name)
            .addressKnown(addressKnown)
            .address(address)
            .build();
    }

    private static UnderlesseeMortgagee buildExpectedUnderlesseeOrMortgagee(boolean nameKnown,
                                                                            String name,
                                                                            boolean addressKnown,
                                                                            AddressUK address,
                                                                            Boolean addAdditionalUnderlesseeMortgagee) {
        return UnderlesseeMortgagee.builder()
            .underlesseeOrMortgageeNameKnown(nameKnown)
            .underlesseeOrMortgageeName(name)
            .underlesseeOrMortgageeAddressKnown(addressKnown)
            .underlesseeOrMortgageeAddress(address)
            .addAdditionalUnderlesseeOrMortgagee(addAdditionalUnderlesseeMortgagee)
            .build();
    }

}
