package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseTabView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefendantInformationTabDetailsBuilderTest {

    private DefendantInformationTabDetailsBuilder defendantInformationTabDetailsBuilder;

    @BeforeEach
    void setUp() {
        defendantInformationTabDetailsBuilder = new DefendantInformationTabDetailsBuilder();
    }

    @Test
    void shouldSetDefendantInSummaryTabWhenThereIsAtLeastOneDefendant() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        String firstName = "Defendant";
        String lastName = "One";
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName(firstName)
                              .lastName(lastName)
                              .addressKnown(VerticalYesNo.YES)
                              .address(address)
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.NO)
                              .build())
            ))
            .build();

        // When
        DefendantInformationTabDetails defendantInformationTabDetails = defendantInformationTabDetailsBuilder
            .buildSummaryDefendantOneDetails(pcsCase);

        // Then
        assertThat(defendantInformationTabDetails.getNameKnown()).isEqualTo("Yes");
        assertThat(defendantInformationTabDetails.getFirstName()).isEqualTo(firstName);
        assertThat(defendantInformationTabDetails.getLastName()).isEqualTo(lastName);
        assertThat(defendantInformationTabDetails.getAddressForServiceKnown()).isEqualTo("Yes");
        assertThat(defendantInformationTabDetails.getAddressKnown()).isNull();
        assertThat(defendantInformationTabDetails.getAddressForService()).isEqualTo(address);
    }

    @Test
    void shouldNotSetDefendantInSummaryTabWhenThereIsNoDefendants() {
        PCSCase pcsCase = PCSCase.builder().build();
        // When
        DefendantInformationTabDetails defendantInformationTabDetails = defendantInformationTabDetailsBuilder
            .buildSummaryDefendantOneDetails(pcsCase);

        // Then
        assertThat(defendantInformationTabDetails).isNull();
    }

    @Test
    void shouldSetUnknownDefendantNameWhenNameNotKnownButAddressKnownInSummaryTab() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(listValue(Party.builder()
                                                 .nameKnown(VerticalYesNo.NO)
                                                 .addressKnown(VerticalYesNo.YES)
                                                 .address(address)
                                                 .build())))
            .build();

        // When
        DefendantInformationTabDetails defendantInformationTabDetails = defendantInformationTabDetailsBuilder
            .buildSummaryDefendantOneDetails(pcsCase);

        // Then
        assertThat(defendantInformationTabDetails.getNameKnown()).isEqualTo("No");
        assertThat(defendantInformationTabDetails.getFirstName()).isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(defendantInformationTabDetails.getLastName()).isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(defendantInformationTabDetails.getAddressForServiceKnown()).isEqualTo("Yes");
        assertThat(defendantInformationTabDetails.getAddressKnown()).isNull();
        assertThat(defendantInformationTabDetails.getAddressForService()).isEqualTo(address);
    }

    @Test
    void shouldDefaultDefendantAddressForServiceToPropertyAddressWhenAddressNotKnownInSummaryTab() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .allDefendants(List.of(listValue(Party.builder()
                                                 .nameKnown(VerticalYesNo.NO)
                                                 .addressKnown(VerticalYesNo.NO)
                                                 .build())))
            .build();

        // When
        DefendantInformationTabDetails defendantInformationTabDetails = defendantInformationTabDetailsBuilder
            .buildSummaryDefendantOneDetails(pcsCase);

        // Then
        assertThat(defendantInformationTabDetails.getNameKnown()).isEqualTo("No");
        assertThat(defendantInformationTabDetails.getFirstName()).isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(defendantInformationTabDetails.getLastName()).isEqualTo(CaseTabView.NAME_UNKNOWN);
        assertThat(defendantInformationTabDetails.getAddressForServiceKnown()).isEqualTo("No");
        assertThat(defendantInformationTabDetails.getAddressKnown()).isNull();
        assertThat(defendantInformationTabDetails.getAddressForService()).isEqualTo(propertyAddress);
    }

    @Test
    void shouldSetDefendantInDetailsTabWhenThereIsAtLeastOneDefendant() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        String firstName = "Defendant";
        String lastName = "One";
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName(firstName)
                              .lastName(lastName)
                              .addressKnown(VerticalYesNo.YES)
                              .address(address)
                              .build()),
                listValue(Party.builder()
                              .nameKnown(VerticalYesNo.NO)
                              .addressKnown(VerticalYesNo.NO)
                              .build())
            ))
            .build();

        // When
        DefendantInformationTabDetails defendantInformationTabDetails = defendantInformationTabDetailsBuilder
            .buildDetailedDefendantDetails(pcsCase);

        // Then
        assertThat(defendantInformationTabDetails.getNameKnown()).isEqualTo("Yes");
        assertThat(defendantInformationTabDetails.getFirstName()).isEqualTo(firstName);
        assertThat(defendantInformationTabDetails.getLastName()).isEqualTo(lastName);
        assertThat(defendantInformationTabDetails.getAddressKnown()).isEqualTo("Yes");
        assertThat(defendantInformationTabDetails.getAddressForService()).isEqualTo(address);
    }

    @Test
    void shouldNotSetDefendantInDetailsTabWhenThereIsNoDefendants() {
        PCSCase pcsCase = PCSCase.builder().build();
        // When
        DefendantInformationTabDetails defendantInformationTabDetails = defendantInformationTabDetailsBuilder
            .buildDetailedDefendantDetails(pcsCase);

        // Then
        assertThat(defendantInformationTabDetails).isNull();
    }

    @Test
    void shouldSetUnknownDefendantNameWhenNameNotKnownButAddressKnownInDetailsTab() {
        // Given
        AddressUK address = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(List.of(listValue(Party.builder()
                                                 .nameKnown(VerticalYesNo.NO)
                                                 .addressKnown(VerticalYesNo.YES)
                                                 .address(address)
                                                 .build())))
            .build();

        // When
        DefendantInformationTabDetails defendantInformationTabDetails = defendantInformationTabDetailsBuilder
            .buildDetailedDefendantDetails(pcsCase);

        // Then
        assertThat(defendantInformationTabDetails.getNameKnown()).isEqualTo("No");
        assertThat(defendantInformationTabDetails.getFirstName()).isNull();
        assertThat(defendantInformationTabDetails.getLastName()).isNull();
        assertThat(defendantInformationTabDetails.getAddressKnown()).isEqualTo("Yes");
        assertThat(defendantInformationTabDetails.getAddressForService()).isEqualTo(address);
    }

    @Test
    void shouldDefaultDefendantAddressForServiceToPropertyAddressWhenAddressNotKnownInDetailsTab() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .allDefendants(List.of(listValue(Party.builder()
                                                 .nameKnown(VerticalYesNo.NO)
                                                 .addressKnown(VerticalYesNo.NO)
                                                 .build())))
            .build();

        // When
        DefendantInformationTabDetails defendantInformationTabDetails = defendantInformationTabDetailsBuilder
            .buildDetailedDefendantDetails(pcsCase);

        // Then
        assertThat(defendantInformationTabDetails.getNameKnown()).isEqualTo("No");
        assertThat(defendantInformationTabDetails.getFirstName()).isNull();
        assertThat(defendantInformationTabDetails.getLastName()).isNull();
        assertThat(defendantInformationTabDetails.getAddressKnown()).isEqualTo("No");
        assertThat(defendantInformationTabDetails.getAddressForService()).isNull();
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
