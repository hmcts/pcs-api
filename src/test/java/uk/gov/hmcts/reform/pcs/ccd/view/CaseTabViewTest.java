package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.DefendantTabDetails;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CaseTabViewTest {

    private CaseTabView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseTabView();
    }

    @Test
    void shouldSetClaimantDetailsInCasePartiesTab() {
        // Given
        String name = "claimant";
        AddressUK address = AddressUK.builder().build();
        String telephoneNumber = "telephone number";
        String emailAddress = "email@test.com";
        Party claimant = Party.builder()
            .orgName(name)
            .address(address)
            .phoneNumber(telephoneNumber)
            .emailAddress(emailAddress)
            .build();
        List<ListValue<Party>> claimants = List.of(
            ListValue.<Party>builder()
                .value(claimant)
                .build()
        );

        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(claimants)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        ClaimantTabDetails claimantTabDetails = pcsCase.getCasePartiesTab().getClaimantDetails();
        assertThat(claimantTabDetails).isNotNull();
        assertThat(claimantTabDetails.getName()).isEqualTo(name);
        assertThat(claimantTabDetails.getServiceAddress()).isEqualTo(address);
        assertThat(claimantTabDetails.getEmailAddress()).isEqualTo(emailAddress);
        assertThat(claimantTabDetails.getTelephoneNumber()).isEqualTo(telephoneNumber);
    }

    @Test
    void shouldSetDefendantOneDetailsInCasePartiesTab() {
        // Given
        String firstName = "defendant";
        String lastName = "one";
        AddressUK address = AddressUK.builder().build();
        Party defendant = Party.builder()
            .firstName(firstName)
            .lastName(lastName)
            .nameKnown(VerticalYesNo.YES)
            .address(address)
            .build();

        ListValue<Party> defendantListValue = ListValue.<Party>builder()
            .value(defendant)
            .build();

        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(defendantListValue);

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(defendants)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        DefendantTabDetails defendant1TabDetails = pcsCase.getCasePartiesTab().getDefendantOneDetails();
        List<ListValue<DefendantTabDetails>> additionalDefendantsTabDetails =
            pcsCase.getCasePartiesTab().getDefendantsDetails();

        assertThat(defendant1TabDetails.getFirstName()).isEqualTo(firstName);
        assertThat(defendant1TabDetails.getLastName()).isEqualTo(lastName);
        assertThat(defendant1TabDetails.getServiceAddress()).isEqualTo(address);
        assertThat(additionalDefendantsTabDetails).isNull();
    }

    @Test
    void shouldSetMultipleDefendantDetailsInCasePartiesTab() {
        // Given
        String defendant1FirstName = "defendant1";
        String defendant1LastName = "one";
        AddressUK address1 = AddressUK.builder().build();
        Party defendant1 = Party.builder()
            .firstName(defendant1FirstName)
            .lastName(defendant1LastName)
            .nameKnown(VerticalYesNo.YES)
            .address(address1)
            .build();

        String defendant2FirstName = "defendant2";
        String defendant2LastName = "two";
        AddressUK address2 = AddressUK.builder().build();
        Party defendant2 = Party.builder()
            .firstName(defendant2FirstName)
            .lastName(defendant2LastName)
            .nameKnown(VerticalYesNo.YES)
            .address(address2)
            .build();

        ListValue<Party> defendant1ListValue = ListValue.<Party>builder()
            .value(defendant1)
            .build();

        ListValue<Party> defendant2ListValue = ListValue.<Party>builder()
            .value(defendant2)
            .build();

        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(defendant1ListValue);
        defendants.add(defendant2ListValue);

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(defendants)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        DefendantTabDetails defendant1TabDetails = pcsCase.getCasePartiesTab().getDefendantOneDetails();
        List<ListValue<DefendantTabDetails>> additionalDefendantsTabDetails =
            pcsCase.getCasePartiesTab().getDefendantsDetails();

        assertThat(defendant1TabDetails.getFirstName()).isEqualTo(defendant1FirstName);
        assertThat(defendant1TabDetails.getLastName()).isEqualTo(defendant1LastName);
        assertThat(defendant1TabDetails.getServiceAddress()).isEqualTo(address1);
        assertThat(additionalDefendantsTabDetails).isNotNull();
        assertThat(additionalDefendantsTabDetails.size()).isEqualTo(1);

        DefendantTabDetails defendant2TabDetails = additionalDefendantsTabDetails.getFirst().getValue();
        assertThat(defendant2TabDetails.getFirstName()).isEqualTo(defendant2FirstName);
        assertThat(defendant2TabDetails.getLastName()).isEqualTo(defendant2LastName);
        assertThat(defendant2TabDetails.getServiceAddress()).isEqualTo(address2);
    }

    @Test
    void shouldSetDefaultDefendantNameInCasePartiesTabIfNameNotKnown() {
        // Given
        String firstName = "defendant";
        String lastName = "One";
        AddressUK address = AddressUK.builder().build();
        Party defendant = Party.builder()
            .firstName(firstName)
            .lastName(lastName)
            .nameKnown(VerticalYesNo.NO)
            .address(address)
            .build();

        ListValue<Party> defendantListValue = ListValue.<Party>builder()
            .value(defendant)
            .build();

        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(defendantListValue);

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(defendants)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        DefendantTabDetails defendant1TabDetails = pcsCase.getCasePartiesTab().getDefendantOneDetails();
        List<ListValue<DefendantTabDetails>> additionalDefendantsTabDetails =
            pcsCase.getCasePartiesTab().getDefendantsDetails();

        assertThat(defendant1TabDetails.getFirstName()).isEqualTo("Person unknown");
        assertThat(defendant1TabDetails.getLastName()).isEqualTo("Person unknown");
        assertThat(defendant1TabDetails.getServiceAddress()).isEqualTo(address);
        assertThat(additionalDefendantsTabDetails).isNull();
    }

    @Test
    void shouldSetDefaultDefendantAddressToPropertyAddressIfNotKnown() {
        // Given
        String firstName = "defendant";
        String lastName = "One";
        AddressUK address = AddressUK.builder().build();
        Party defendant = Party.builder()
            .firstName(firstName)
            .lastName(lastName)
            .nameKnown(VerticalYesNo.YES)
            .address(null)
            .build();

        ListValue<Party> defendantListValue = ListValue.<Party>builder()
            .value(defendant)
            .build();

        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(defendantListValue);

        PCSCase pcsCase = PCSCase.builder()
            .allDefendants(defendants)
            .propertyAddress(address)
            .build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        DefendantTabDetails defendant1TabDetails = pcsCase.getCasePartiesTab().getDefendantOneDetails();
        List<ListValue<DefendantTabDetails>> additionalDefendantsTabDetails =
            pcsCase.getCasePartiesTab().getDefendantsDetails();

        assertThat(defendant1TabDetails.getFirstName()).isEqualTo(firstName);
        assertThat(defendant1TabDetails.getLastName()).isEqualTo(lastName);
        assertThat(defendant1TabDetails.getServiceAddress()).isEqualTo(address);
        assertThat(additionalDefendantsTabDetails).isNull();
    }

    @Test
    void shouldNotSetCasePartiesTabWithNoData() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        assertThat(pcsCase.getCasePartiesTab()).isNotNull();
        assertThat(pcsCase.getCasePartiesTab().getClaimantDetails()).isNull();
        assertThat(pcsCase.getCasePartiesTab().getDefendantOneDetails()).isNull();
        assertThat(pcsCase.getCasePartiesTab().getDefendantsDetails()).isNull();
    }
}
