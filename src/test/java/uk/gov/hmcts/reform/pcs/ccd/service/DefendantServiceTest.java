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
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENT_SET_NAME_PLACEHOLDER;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class DefendantServiceTest {

    @Mock
    private PCSCase pcsCase;

    private DefendantService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantService();
    }

    @Test
    void shouldThrowExceptionForNullDefendant1() {
        // Given
        when(pcsCase.getDefendant1()).thenReturn(null);

        // When
        Throwable throwable = catchThrowable(() -> underTest.buildDefendantsList(pcsCase));

        // Then
        assertThat(throwable)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Defendant 1 must be provided");
    }

    @ParameterizedTest(name = ARGUMENT_SET_NAME_PLACEHOLDER)
    @MethodSource("singleDefendantScenarios")
    void shouldBuildListWithSingleDefendant(DefendantDetails defendant1Details, Defendant expectedDefendant) {
        // Given
        when(pcsCase.getDefendant1()).thenReturn(defendant1Details);

        // When
        List<Defendant> defendantList = underTest.buildDefendantsList(pcsCase);

        // Then
        assertThat(defendantList).containsExactly(expectedDefendant);
    }

    @Test
    void shouldBuildListWithMultipleDefendants() {
        // Given
        AddressUK defendant1Address = mock(AddressUK.class);

        DefendantDetails defendant1Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("defendant 1 first name")
            .lastName(("defendant 1 last name"))
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(defendant1Address)
            .build();

        DefendantDetails defendant2Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("defendant 2 first name")
            .lastName(("defendant 2 last name"))
            .addressKnown(VerticalYesNo.NO)
            .build();

        DefendantDetails defendant3Details = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.YES)
            .build();

        when(pcsCase.getDefendant1()).thenReturn(defendant1Details);

        List<DefendantDetails> additionalDefendantsDetails = List.of(defendant2Details, defendant3Details);
        when(pcsCase.getAdditionalDefendants()).thenReturn(wrapListItems(additionalDefendantsDetails));

        // When
        List<Defendant> defendantList = underTest.buildDefendantsList(pcsCase);

        // Then

        Defendant expectedDefendant1 = Defendant.builder()
            .nameKnown(true)
            .firstName("defendant 1 first name")
            .lastName("defendant 1 last name")
            .addressKnown(true)
            .addressSameAsPossession(false)
            .correspondenceAddress(defendant1Address)
            .build();

        Defendant expectedDefendant2 = Defendant.builder()
            .nameKnown(true)
            .firstName("defendant 2 first name")
            .lastName("defendant 2 last name")
            .addressKnown(false)
            .build();

        Defendant expectedDefendant3 = Defendant.builder()
            .nameKnown(false)
            .addressKnown(true)
            .addressSameAsPossession(true)
            .build();

        assertThat(defendantList).containsExactly(expectedDefendant1, expectedDefendant2, expectedDefendant3);

    }

    @Test
    void shouldBlankFieldsIfNotKnownSelected() {
        // Given
        AddressUK correspondenceAddress = mock(AddressUK.class);

        DefendantDetails defendantDetails = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .firstName("should be ignored")
            .lastName(("should be ignored"))
            .addressKnown(VerticalYesNo.NO)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(correspondenceAddress)
            .build();

        DefendantDetails additionalDefendantDetails = DefendantDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .firstName("should be ignored")
            .lastName(("should be ignored"))
            .addressKnown(VerticalYesNo.NO)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(correspondenceAddress)
            .build();

        when(pcsCase.getDefendant1()).thenReturn(defendantDetails);
        when(pcsCase.getAdditionalDefendants()).thenReturn(wrapListItems(List.of(additionalDefendantDetails)));

        // When
        List<Defendant> defendantList = underTest.buildDefendantsList(pcsCase);

        // Then
        Defendant expectedDefendant1 = Defendant.builder()
            .nameKnown(false)
            .addressKnown(false)
            .build();

        Defendant expectedDefendant2 = Defendant.builder()
            .nameKnown(false)
            .addressKnown(false)
            .build();

        assertThat(defendantList).containsExactly(expectedDefendant1, expectedDefendant2);
    }

    private static Stream<Arguments> singleDefendantScenarios() {
        AddressUK correspondenceAddress = mock(AddressUK.class);

        return Stream.of(
            argumentSet(
                "Name and address not known",

                // Case data DefendantDetails
                DefendantDetails.builder()
                    .nameKnown(VerticalYesNo.NO)
                    .addressKnown(VerticalYesNo.NO)
                    .build(),

                // Expected Defendant
                Defendant.builder()
                    .nameKnown(false)
                    .addressKnown(false)
                    .build()
            ),

            argumentSet(
                "Name known, address not known",

                // Case data DefendantDetails
                DefendantDetails.builder()
                    .nameKnown(VerticalYesNo.YES)
                    .firstName("expected first name")
                    .lastName(("expected last name"))
                    .addressKnown(VerticalYesNo.NO)
                    .build(),

                // Expected Defendant
                Defendant.builder()
                    .nameKnown(true)
                    .firstName("expected first name")
                    .lastName("expected last name")
                    .addressKnown(false)
                    .build()
            ),

            argumentSet(
                "Name not known, address same as possession",

                // Case data DefendantDetails
                DefendantDetails.builder()
                    .nameKnown(VerticalYesNo.NO)
                    .addressKnown(VerticalYesNo.YES)
                    .addressSameAsPossession(VerticalYesNo.YES)
                    .build(),

                // Expected Defendant
                Defendant.builder()
                    .nameKnown(false)
                    .addressKnown(true)
                    .addressSameAsPossession(true)
                    .build()
            ),

            argumentSet(
                "Name known, different correspondence address",

                // Case data DefendantDetails
                DefendantDetails.builder()
                    .nameKnown(VerticalYesNo.YES)
                    .firstName("expected first name")
                    .lastName(("expected last name"))
                    .addressKnown(VerticalYesNo.YES)
                    .addressSameAsPossession(VerticalYesNo.NO)
                    .correspondenceAddress(correspondenceAddress)
                    .build(),

                // Expected Defendant
                Defendant.builder()
                    .nameKnown(true)
                    .firstName("expected first name")
                    .lastName("expected last name")
                    .addressKnown(true)
                    .addressSameAsPossession(false)
                    .correspondenceAddress(correspondenceAddress)
                    .build()
            )
        );
    }
}
