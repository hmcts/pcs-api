package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseTabView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AdditionalDefendantInformationTabDetailsBuilderTest {

    private AdditionalDefendantInformationTabDetailsBuilder additionalDefendantInformationTabDetailsBuilder;

    @BeforeEach
    void setUp() {
        additionalDefendantInformationTabDetailsBuilder = new AdditionalDefendantInformationTabDetailsBuilder();
    }

    @Test
    void shouldSetAdditionalDefendantsInSummaryTabWhenThereIsMoreThanOneDefendant() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        String firstName = "Defendant2";
        String lastName = "Two";
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName(firstName)
                              .lastName(lastName)
                              .addressKnown(VerticalYesNo.YES)
                              .address(address)
                              .build())
            ))
            .build();

        // When
        List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants =
            additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase);

        // Then
        assertThat(additionalDefendants).hasSize(1);
        assertThat(additionalDefendants.getFirst().getValue().getNameKnown()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getFirstName())
            .isEqualTo(firstName);
        assertThat(additionalDefendants.getFirst().getValue().getLastName())
            .isEqualTo(lastName);
        assertThat(additionalDefendants.getFirst().getValue().getAddressKnown()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getAddressForService())
            .isEqualTo(address);
    }

    @Test
    void shouldNotSetAdditionalDefendantsInSummaryTabWhenThereIsOnlyOneDefendant() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build())
            ))
            .build();

        // When
        List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants =
            additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase);

        // Then
        assertThat(additionalDefendants).isNull();
    }

    @Test
    void shouldSetUnknownAdditionalDefendantNameWhenNameNotKnownButAddressKnownInSummaryTab() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.YES)
                              .address(address)
                              .build())
            ))
            .build();

        // When
        List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants =
            additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase);

        // Then
        assertThat(additionalDefendants).hasSize(1);
        assertThat(additionalDefendants.getFirst().getValue().getNameKnown()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getFirstName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(additionalDefendants.getFirst().getValue().getLastName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(additionalDefendants.getFirst().getValue().getAddressKnown()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getAddressForService())
            .isEqualTo(address);
    }

    @Test
    void shouldDefaultAdditionalDefendantAddressForServiceToPropertyAddressWhenAddressNotKnownInSummaryTab() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.NO)
                              .build())
            ))
            .build();

        // When
        List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants =
            additionalDefendantInformationTabDetailsBuilder.buildSummaryAdditionalDefendantsDetails(pcsCase);

        // Then
        assertThat(additionalDefendants).hasSize(1);
        assertThat(additionalDefendants.getFirst().getValue().getNameKnown()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getFirstName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(additionalDefendants.getFirst().getValue().getLastName())
            .isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(additionalDefendants.getFirst().getValue().getAddressKnown()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getAddressForService())
            .isEqualTo(propertyAddress);
    }

    @Test
    void shouldSetAdditionalDefendantsInDetailsTabWhenThereIsMoreThanOneDefendant() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        String firstName = "Defendant2";
        String lastName = "Two";
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName(firstName)
                              .lastName(lastName)
                              .addressKnown(VerticalYesNo.YES)
                              .address(address)
                              .build())
            ))
            .build();

        // When
        List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants =
            additionalDefendantInformationTabDetailsBuilder.buildDetailedAdditionalDefendantsDetails(pcsCase);

        // Then
        assertThat(additionalDefendants).hasSize(1);
        assertThat(additionalDefendants.getFirst().getValue().getNameKnown())
            .isEqualTo("Yes");
        assertThat(additionalDefendants.getFirst().getValue().getFirstName())
            .isEqualTo(firstName);
        assertThat(additionalDefendants.getFirst().getValue().getLastName())
            .isEqualTo(lastName);
        assertThat(additionalDefendants.getFirst().getValue().getAddressKnown())
            .isEqualTo("Yes");
        assertThat(additionalDefendants.getFirst().getValue().getAddressForService())
            .isEqualTo(address);
    }

    @Test
    void shouldNotSetAdditionalDefendantsInDetailsTabWhenThereIsOnlyOneDefendant() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build())
            ))
            .build();

        // When
        List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants =
            additionalDefendantInformationTabDetailsBuilder.buildDetailedAdditionalDefendantsDetails(pcsCase);

        // Then
        assertThat(additionalDefendants).isNull();
    }

    @Test
    void shouldSetUnknownAdditionalDefendantNameToNullWhenNameNotKnownButAddressKnownInDetailsTab() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.YES)
                              .address(address)
                              .build())
            ))
            .build();

        // When
        List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants =
            additionalDefendantInformationTabDetailsBuilder.buildDetailedAdditionalDefendantsDetails(pcsCase);

        // Then
        assertThat(additionalDefendants).hasSize(1);
        assertThat(additionalDefendants.getFirst().getValue().getNameKnown())
            .isEqualTo("No");
        assertThat(additionalDefendants.getFirst().getValue().getFirstName()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getLastName()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getAddressKnown())
            .isEqualTo("Yes");
        assertThat(additionalDefendants.getFirst().getValue().getAddressForService()).isEqualTo(address);
    }

    @Test
    void shouldDefaultAdditionalDefendantAddressForServiceToNullWhenAddressNotKnownInDetailsTab() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Defendant")
                              .lastName("One")
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.NO)
                              .build())
            ))
            .build();

        // When
        List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendants =
            additionalDefendantInformationTabDetailsBuilder.buildDetailedAdditionalDefendantsDetails(pcsCase);

        // Then
        assertThat(additionalDefendants).hasSize(1);
        assertThat(additionalDefendants.getFirst().getValue().getNameKnown())
            .isEqualTo("No");
        assertThat(additionalDefendants.getFirst().getValue().getFirstName()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getLastName()).isNull();
        assertThat(additionalDefendants.getFirst().getValue().getAddressKnown())
            .isEqualTo("No");
        assertThat(additionalDefendants.getFirst().getValue().getAddressForService()).isNull();
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
