package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.DefendantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimGroundSummaryBuilder;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;

@ExtendWith(MockitoExtension.class)
class CaseTabViewTest {

    @Mock
    private ClaimGroundSummaryBuilder claimGroundSummaryBuilder;

    @Mock
    private CaseSummaryTabView caseSummaryTabView;

    @Mock
    private CaseDetailsTabView caseDetailsTabView;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private CaseTabView underTest;

    @BeforeEach
    void setUp() {
        UserInfo userInfo = UserInfo.builder()
            .roles(List.of("caseworker-pcs"))
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
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
    void shouldSetTabFieldsUsingDraftDataOnly() {
        // Given
        PCSCase draftCaseData = PCSCase.builder().build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);
    }

    @Test
    void shouldSetDraftTabFieldsUsingDraftDefendantsAndGrounds() {
        // Given
        AddressUK submittedPropertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        AddressUK draftPropertyAddress = AddressUK.builder().postCode("E1 1AA").build();
        AddressUK defendantOneAddress = AddressUK.builder().postCode("M1 1AA").build();
        AddressUK additionalDefendantAddress = AddressUK.builder().postCode("B1 1AA").build();
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

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(draftGrounds);

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(submittedPropertyAddress)
            .allClaimants(List.of(listValue(Party.builder().orgName("Submitted claimant").build())))
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder().label("Submitted ground").build())
            ))
            .build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        assertThat(draftCaseData.getAllDefendants()).hasSize(2);
        Party firstDefendant = draftCaseData.getAllDefendants().getFirst().getValue();
        Party secondDefendant = draftCaseData.getAllDefendants().getLast().getValue();
        assertThat(firstDefendant.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(firstDefendant.getFirstName()).isEqualTo("Draft");
        assertThat(firstDefendant.getLastName()).isEqualTo("Defendant");
        assertThat(firstDefendant.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(firstDefendant.getAddress()).isEqualTo(defendantOneAddress);
        assertThat(secondDefendant.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(secondDefendant.getFirstName()).isEqualTo("Additional");
        assertThat(secondDefendant.getLastName()).isEqualTo("Defendant");
        assertThat(secondDefendant.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(secondDefendant.getAddress()).isEqualTo(additionalDefendantAddress);
        assertThat(draftCaseData.getClaimGroundSummaries()).hasSize(1);
        assertThat(draftCaseData.getClaimGroundSummaries().getFirst().getValue().getLabel())
            .isEqualTo("Draft ground");
    }

    @Test
    void shouldSetDraftDefendantWhenNoAdditionalDefendantHasBeenSelected() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        AddressUK defendantAddress = AddressUK.builder().postCode("M1 1AA").build();
        PCSCase draftCaseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Single")
                            .lastName("Defendant")
                            .addressKnown(VerticalYesNo.YES)
                            .correspondenceAddress(defendantAddress)
                            .build())
            .addAnotherDefendant(VerticalYesNo.NO)
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        assertThat(draftCaseData.getAllDefendants()).hasSize(1);
        Party defendant = draftCaseData.getAllDefendants().getFirst().getValue();
        assertThat(defendant.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendant.getFirstName()).isEqualTo("Single");
        assertThat(defendant.getLastName()).isEqualTo("Defendant");
        assertThat(defendant.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(defendant.getAddress()).isEqualTo(defendantAddress);
    }

    @Test
    void shouldNotSetAdditionalDraftDefendantsWhenAdditionalDefendantListIsEmpty() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        PCSCase draftCaseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Only")
                            .lastName("Defendant")
                            .addressKnown(VerticalYesNo.NO)
                            .build())
            .addAnotherDefendant(VerticalYesNo.YES)
            .additionalDefendants(List.of())
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);
        assertThat(draftCaseData.getAllDefendants()).hasSize(1);
        Party defendant = draftCaseData.getAllDefendants().getFirst().getValue();
        assertThat(defendant.getFirstName()).isEqualTo("Only");
        assertThat(defendant.getLastName()).isEqualTo("Defendant");
        assertThat(defendant.getAddress()).isNull();
    }

    @Test
    void shouldNotOverwriteExistingDraftDefendantsWhenDefendantOneIsMissing() {
        // Given
        AddressUK propertyAddress = AddressUK.builder().postCode("SW1A 1AA").build();
        List<ListValue<Party>> existingDefendants = List.of(
            listValue(Party.builder()
                          .nameKnown(VerticalYesNo.YES)
                          .firstName("Existing")
                          .lastName("Defendant")
                          .build())
        );
        PCSCase draftCaseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .allDefendants(existingDefendants)
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        assertThat(draftCaseData.getAllDefendants()).isSameAs(existingDefendants);
    }

    @Test
    void shouldSetDraftUnderlesseeOrMortgagePartiesWhenThereIsMoreThanOneParty() {
        // Given
        String name1 = "name1";
        AddressUK address1 = AddressUK.builder().build();
        UnderlesseeMortgageeDetails underlesseeMortgageeDetails1 = UnderlesseeMortgageeDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .name(name1)
            .addressKnown(VerticalYesNo.YES)
            .address(address1)
            .build();

        UnderlesseeMortgageeDetails underlesseeMortgageeDetails2 = UnderlesseeMortgageeDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();

        PCSCase draftCaseData = PCSCase.builder()
            .underlesseeOrMortgagee1(underlesseeMortgageeDetails1)
            .additionalUnderlesseeOrMortgagee(List.of(listValue(underlesseeMortgageeDetails2)))
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        List<ListValue<Party>> allUnderlesseeOrMortgagees = draftCaseData.getAllUnderlesseeOrMortgagees();
        assertThat(allUnderlesseeOrMortgagees).hasSize(2);
        Party party1 = allUnderlesseeOrMortgagees.getFirst().getValue();
        assertThat(party1.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(party1.getOrgName()).isEqualTo(name1);
        assertThat(party1.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(party1.getAddress()).isEqualTo(address1);

        Party party2 = allUnderlesseeOrMortgagees.getLast().getValue();
        assertThat(party2.getNameKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(party2.getOrgName()).isNull();
        assertThat(party2.getAddressKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(party2.getAddress()).isNull();
    }

    @Test
    void shouldSetDraftUnderlesseeOrMortgagePartiesWhenThereIsOnlyOneParty() {
        // Given
        String name1 = "name1";
        AddressUK address1 = AddressUK.builder().build();
        UnderlesseeMortgageeDetails underlesseeMortgageeDetails1 = UnderlesseeMortgageeDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .name(name1)
            .addressKnown(VerticalYesNo.YES)
            .address(address1)
            .build();

        PCSCase draftCaseData = PCSCase.builder()
            .underlesseeOrMortgagee1(underlesseeMortgageeDetails1)
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        List<ListValue<Party>> allUnderlesseeOrMortgagees = draftCaseData.getAllUnderlesseeOrMortgagees();
        assertThat(allUnderlesseeOrMortgagees).hasSize(1);
        Party party1 = allUnderlesseeOrMortgagees.getFirst().getValue();
        assertThat(party1.getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(party1.getOrgName()).isEqualTo(name1);
        assertThat(party1.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(party1.getAddress()).isEqualTo(address1);
    }

    @Test
    void shouldNotSetDraftUnderlesseeOrMortgagePartiesWhenThereIsNoParties() {
        // Given
        PCSCase draftCaseData = PCSCase.builder().build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        List<ListValue<Party>> allUnderlesseeOrMortgagees = draftCaseData.getAllUnderlesseeOrMortgagees();
        assertThat(allUnderlesseeOrMortgagees).isNull();
    }

    @Test
    void shouldNotSetDraftUnderlesseeOrMortgagePartiesWhenUnderlesseeMortgageeDetails1IsNotSet() {
        // Given
        UnderlesseeMortgageeDetails additionalUnderlesseeMortgageeDetails = UnderlesseeMortgageeDetails.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();

        PCSCase draftCaseData = PCSCase.builder()
            .additionalUnderlesseeOrMortgagee(List.of(listValue(additionalUnderlesseeMortgageeDetails)))
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        List<ListValue<Party>> allUnderlesseeOrMortgagees = draftCaseData.getAllUnderlesseeOrMortgagees();
        assertThat(allUnderlesseeOrMortgagees).isNull();
    }

    @Test
    void shouldSetSuspensionOfRightToBuyAndDemotionOfTenancyDraftDataWhenBothSelected() {
        // Given
        String suspensionReason = "Suspension reason";
        String demotionReason = "Demotion reason";
        SuspensionOfRightToBuyDemotionOfTenancy suspensionOfRightToBuyDemotionOfTenancy =
            SuspensionOfRightToBuyDemotionOfTenancy.builder()
                .suspensionOfRightToBuyActs(SuspensionOfRightToBuyHousingAct.SECTION_6A_2)
                .suspensionOrderReason(suspensionReason)
                .demotionOfTenancyActs(DemotionOfTenancyHousingAct.SECTION_6A_2)
                .demotionOrderReason(demotionReason)
                .build();

        PCSCase draftCaseData = PCSCase.builder()
            .suspensionOfRightToBuyDemotionOfTenancy(suspensionOfRightToBuyDemotionOfTenancy)
            .alternativesToPossession(Set.of(SUSPENSION_OF_RIGHT_TO_BUY, DEMOTION_OF_TENANCY))
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        SuspensionOfRightToBuy suspensionOfRightToBuy = draftCaseData.getSuspensionOfRightToBuy();
        assertThat(suspensionOfRightToBuy.getHousingAct()).isEqualTo(SuspensionOfRightToBuyHousingAct.SECTION_6A_2);
        assertThat(suspensionOfRightToBuy.getReason()).isEqualTo(suspensionReason);

        DemotionOfTenancy demotionOfTenancy = draftCaseData.getDemotionOfTenancy();
        assertThat(demotionOfTenancy.getHousingAct()).isEqualTo(DemotionOfTenancyHousingAct.SECTION_6A_2);
        assertThat(demotionOfTenancy.getReason()).isEqualTo(demotionReason);
    }

    @Test
    void shouldNotSetSuspensionOfRightToBuyAndDemotionOfTenancyDraftDataWhenOnlyDemotionSelected() {
        // Given
        String suspensionReason = "Suspension reason";
        String demotionReason = "Demotion reason";
        SuspensionOfRightToBuyDemotionOfTenancy suspensionOfRightToBuyDemotionOfTenancy =
            SuspensionOfRightToBuyDemotionOfTenancy.builder()
                .suspensionOfRightToBuyActs(SuspensionOfRightToBuyHousingAct.SECTION_6A_2)
                .suspensionOrderReason(suspensionReason)
                .demotionOfTenancyActs(DemotionOfTenancyHousingAct.SECTION_6A_2)
                .demotionOrderReason(demotionReason)
                .build();

        PCSCase draftCaseData = PCSCase.builder()
            .suspensionOfRightToBuyDemotionOfTenancy(suspensionOfRightToBuyDemotionOfTenancy)
            .alternativesToPossession(Set.of(DEMOTION_OF_TENANCY))
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        SuspensionOfRightToBuy suspensionOfRightToBuy = draftCaseData.getSuspensionOfRightToBuy();
        assertThat(suspensionOfRightToBuy).isNull();

        DemotionOfTenancy demotionOfTenancy = draftCaseData.getDemotionOfTenancy();
        assertThat(demotionOfTenancy).isNull();
    }

    @Test
    void shouldNotSetSuspensionOfRightToBuyAndDemotionOfTenancyDraftDataWhenOnlySuspensionSelected() {
        // Given
        String suspensionReason = "Suspension reason";
        String demotionReason = "Demotion reason";
        SuspensionOfRightToBuyDemotionOfTenancy suspensionOfRightToBuyDemotionOfTenancy =
            SuspensionOfRightToBuyDemotionOfTenancy.builder()
                .suspensionOfRightToBuyActs(SuspensionOfRightToBuyHousingAct.SECTION_6A_2)
                .suspensionOrderReason(suspensionReason)
                .demotionOfTenancyActs(DemotionOfTenancyHousingAct.SECTION_6A_2)
                .demotionOrderReason(demotionReason)
                .build();

        PCSCase draftCaseData = PCSCase.builder()
            .suspensionOfRightToBuyDemotionOfTenancy(suspensionOfRightToBuyDemotionOfTenancy)
            .alternativesToPossession(Set.of(SUSPENSION_OF_RIGHT_TO_BUY))
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, times(0)).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, times(0)).buildCaseDetailsTab(pcsCase);

        SuspensionOfRightToBuy suspensionOfRightToBuy = draftCaseData.getSuspensionOfRightToBuy();
        assertThat(suspensionOfRightToBuy).isNull();

        DemotionOfTenancy demotionOfTenancy = draftCaseData.getDemotionOfTenancy();
        assertThat(demotionOfTenancy).isNull();
    }

    @Test
    void shouldNotSetSuspensionOfRightToBuyAndDemotionOfTenancyDraftDataWithNoData() {
        // Given
        PCSCase draftCaseData = PCSCase.builder()
            .alternativesToPossession(Set.of(SUSPENSION_OF_RIGHT_TO_BUY, DEMOTION_OF_TENANCY))
            .build();

        when(caseSummaryTabView.buildSummaryTab(draftCaseData)).thenReturn(
            SummaryTab.builder().build()
        );
        when(caseDetailsTabView.buildCaseDetailsTab(draftCaseData)).thenReturn(
            CaseDetailsTab.builder().build()
        );
        when(claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData)).thenReturn(List.of());

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        SummaryTab summaryTab = pcsCase.getSummaryTab();
        CaseDetailsTab caseDetailsTab = pcsCase.getCaseDetailsTab();
        assertThat(summaryTab).isNotNull();
        assertThat(caseDetailsTab).isNotNull();
        verify(caseSummaryTabView, times(1)).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, times(1)).buildCaseDetailsTab(draftCaseData);
        verify(caseSummaryTabView, never()).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, never()).buildCaseDetailsTab(pcsCase);

        SuspensionOfRightToBuy suspensionOfRightToBuy = draftCaseData.getSuspensionOfRightToBuy();
        assertThat(suspensionOfRightToBuy).isNull();

        DemotionOfTenancy demotionOfTenancy = draftCaseData.getDemotionOfTenancy();
        assertThat(demotionOfTenancy).isNull();
    }

    @Test
    void shouldNotSetCaseTabsWhenUserIsCitizen() {
        // Given
        UserInfo userInfo = UserInfo.builder()
            .roles(List.of("citizen"))
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseTabFields(pcsCase);

        // Then
        verify(caseSummaryTabView, never()).buildSummaryTab(pcsCase);
        verify(caseDetailsTabView, never()).buildCaseDetailsTab(pcsCase);
        assertThat(pcsCase.getCasePartiesTab()).isNull();
        assertThat(pcsCase.getSummaryTab()).isNull();
        assertThat(pcsCase.getCaseDetailsTab()).isNull();
    }

    @Test
    void shouldNotSetDraftCaseTabsWhenUserIsCitizen() {
        // Given
        UserInfo userInfo = UserInfo.builder()
            .roles(List.of("citizen"))
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        PCSCase pcsCase = PCSCase.builder().build();
        PCSCase draftCaseData = PCSCase.builder().build();

        // When
        underTest.setDraftCaseTabFields(pcsCase, draftCaseData);

        // Then
        verify(caseSummaryTabView, never()).buildSummaryTab(draftCaseData);
        verify(caseDetailsTabView, never()).buildCaseDetailsTab(draftCaseData);
        assertThat(pcsCase.getSummaryTab()).isNull();
        assertThat(pcsCase.getCaseDetailsTab()).isNull();
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }
}
