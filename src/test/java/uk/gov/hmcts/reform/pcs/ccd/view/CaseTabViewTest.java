package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.DefendantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimGroundSummaryBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseTabViewTest {

    private CaseTabView underTest;

    @Mock
    private ClaimGroundSummaryBuilder claimGroundSummaryBuilder;

    @Mock
    private CaseSummaryTabView caseSummaryTabView;

    @Mock
    private CaseDetailsTabView caseDetailsTabView;

    @BeforeEach
    void setUp() {
        underTest = new CaseTabView(claimGroundSummaryBuilder, caseSummaryTabView, caseDetailsTabView);
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

    @Test
    void shouldSetDraftSummaryTabFieldsUsingSubmittedFallbacks() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        List<ListValue<Party>> submittedClaimants = List.of(
            listValue(Party.builder().orgName("Submitted claimant").build())
        );
        List<ListValue<Party>> submittedDefendants = List.of(
            listValue(Party.builder()
                          .nameKnown(VerticalYesNo.YES)
                          .firstName("Submitted")
                          .lastName("Defendant")
                          .addressKnown(VerticalYesNo.YES)
                          .address(propertyAddress)
                          .build())
        );
        List<ListValue<ClaimGroundSummary>> submittedGrounds = List.of(
            listValue(ClaimGroundSummary.builder().label("Submitted ground").build())
        );
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .allClaimants(submittedClaimants)
            .allDefendants(submittedDefendants)
            .claimGroundSummaries(submittedGrounds)
            .build();
        PCSCase draftCaseData = PCSCase.builder().build();

        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        assertThat(summaryTab.getRepossessedPropertyAddress()).isEqualTo(propertyAddress);
        assertThat(summaryTab.getClaimantDetails().getClaimantName()).isEqualTo("Submitted claimant");
        assertThat(summaryTab.getDefendantDetails().getFirstName()).isEqualTo("Submitted");
        assertThat(summaryTab.getDefendantDetails().getLastName()).isEqualTo("Defendant");
        assertThat(summaryTab.getDefendantDetails().getAddressForService()).isEqualTo(propertyAddress);
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isEqualTo("Submitted ground");
    }

    @Test
    void shouldSetDraftSummaryTabFieldsUsingDraftDefendantsAndGrounds() {
        // Given
        AddressUK submittedPropertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        AddressUK draftPropertyAddress = AddressUK.builder().postCode("E1 1AA").build();
        AddressUK defendantOneAddress = AddressUK.builder().postCode("M1 1AA").build();
        AddressUK additionalDefendantAddress = AddressUK.builder().postCode("B1 1AA").build();
        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(submittedPropertyAddress)
            .allClaimants(List.of(listValue(Party.builder().orgName("Submitted claimant").build())))
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder().label("Submitted ground").build())
            ))
            .build();
        PCSCase draftCaseData = PCSCase.builder()
            .propertyAddress(draftPropertyAddress)
            .allClaimants(List.of(listValue(Party.builder().orgName("Draft claimant").build())))
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Draft")
                            .lastName("Defendant")
                            .addressKnown(VerticalYesNo.YES)
                            .correspondenceAddress(defendantOneAddress)
                            .build())
            .addAnotherDefendant(VerticalYesNo.YES)
            .additionalDefendants(List.of(
                listValue(DefendantDetails.builder()
                              .nameKnown(VerticalYesNo.YES)
                              .firstName("Additional")
                              .lastName("Defendant")
                              .addressKnown(VerticalYesNo.YES)
                              .correspondenceAddress(additionalDefendantAddress)
                              .build())
            ))
            .build();
        List<ListValue<ClaimGroundSummary>> draftGrounds = List.of(
            listValue(ClaimGroundSummary.builder().label("Draft ground").build())
        );

        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(draftGrounds);

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        assertThat(summaryTab.getRepossessedPropertyAddress()).isEqualTo(draftPropertyAddress);
        assertThat(summaryTab.getClaimantDetails().getClaimantName()).isEqualTo("Draft claimant");
        assertThat(summaryTab.getDefendantDetails().getFirstName()).isEqualTo("Draft");
        assertThat(summaryTab.getDefendantDetails().getLastName()).isEqualTo("Defendant");
        assertThat(summaryTab.getDefendantDetails().getAddressForService()).isEqualTo(defendantOneAddress);
        assertThat(summaryTab.getAdditionalDefendants()).hasSize(1);
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getFirstName()).isEqualTo("Additional");
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getLastName()).isEqualTo("Defendant");
        assertThat(summaryTab.getAdditionalDefendants().getFirst().getValue().getAddressForService())
            .isEqualTo(additionalDefendantAddress);
        assertThat(summaryTab.getGroundsForPossession().getGrounds()).isEqualTo("Draft ground");
        assertThat(draftCaseData.getAllDefendants()).hasSize(2);
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
