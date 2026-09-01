package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entercounterclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CounterClaimAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.CourtPermission;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.HelpWithFees;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.PartyCounterClaimAgainst;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.TypeOfCounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim.UploadCounterClaimForm;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterCounterClaimTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private CounterClaimService counterClaimService;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Mock
    private CourtPermission courtPermission;
    @Mock
    private TypeOfCounterClaim typeOfCounterClaim;
    @Mock
    private CounterClaimAmount counterClaimAmount;
    @Mock
    private HelpWithFees helpWithFees;
    @Mock
    private PartyCounterClaimAgainst partyCounterClaimAgainst;
    @Mock
    private UploadCounterClaimForm uploadCounterClaimForm;

    @BeforeEach
    void setUp() {
        EnterCounterClaim enterCounterClaim = new EnterCounterClaim(
            pcsCaseService, partyService, counterClaimService,
            courtPermission, typeOfCounterClaim, counterClaimAmount, helpWithFees,
            partyCounterClaimAgainst, uploadCounterClaimForm);
        setEventUnderTest(enterCounterClaim);
    }

    @Test
    void shouldPopulateSubmittingPartyListFromDefendantsOnly() {
        // Given
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));

        DynamicList partyList = DynamicList.builder()
            .listItems(List.of(DynamicListElement.builder().code(UUID.randomUUID()).label("Jane Doe - Defendant 1")
                .build()))
            .build();
        when(partyService.buildPartyDynamicList(claimEntity, PartyRole.DEFENDANT))
            .thenReturn(partyList);

        PCSCase caseData = PCSCase.builder().build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getPartyRadioList()).isEqualTo(partyList);
    }

    @Test
    void shouldSaveCounterClaimOnSubmit() {
        // Given
        UUID submittingPartyId = UUID.randomUUID();
        PartyEntity submittingParty = mock(PartyEntity.class);

        when(partyService.getPartyEntityByEntityId(submittingPartyId, TEST_CASE_REFERENCE))
            .thenReturn(submittingParty);

        LocalDate permissionOrderDate = LocalDate.of(2026, 1, 15);
        LocalDate claimReceivedDate = LocalDate.of(2026, 1, 20);
        BigDecimal counterClaimAmount = new BigDecimal("250.00");

        EnterCounterClaimDetails enterCounterClaimDetails = EnterCounterClaimDetails.builder()
            .claimTypeOption(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .courtPermissionGranted(VerticalYesNo.YES)
            .permissionOrderDate(permissionOrderDate)
            .claimReceivedDate(claimReceivedDate)
            .counterClaimAmount(counterClaimAmount)
            .appliedForHwf(VerticalYesNo.YES)
            .hwfReferenceNumber("HWF-123")
            .build();

        DynamicList submittingPartyList = DynamicList.builder()
            .value(DynamicListElement.builder().code(submittingPartyId).build())
            .build();

        UUID againstPartyId = UUID.randomUUID();
        DynamicMultiSelectStringList partyMultiSelectionList = DynamicMultiSelectStringList.builder()
            .value(List.of(DynamicStringListElement.builder().code(againstPartyId.toString()).build()))
            .build();

        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(enterCounterClaimDetails)
            .partyRadioList(submittingPartyList)
            .partyMultiSelectionList(partyMultiSelectionList)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        ArgumentCaptor<CounterClaim> counterClaimCaptor = ArgumentCaptor.forClass(CounterClaim.class);
        verify(counterClaimService).saveCounterClaim(
            eq(TEST_CASE_REFERENCE),
            counterClaimCaptor.capture(),
            eq(submittingParty));

        CounterClaim savedCounterClaim = counterClaimCaptor.getValue();
        assertThat(savedCounterClaim.getClaimType()).isEqualTo(CounterClaimType.PAYMENT_OR_COMPENSATION);
        assertThat(savedCounterClaim.getCourtPermissionGranted()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedCounterClaim.getPermissionOrderDate()).isEqualTo(permissionOrderDate);
        assertThat(savedCounterClaim.getClaimReceivedDate()).isEqualTo(claimReceivedDate);
        assertThat(savedCounterClaim.getIsClaimAmountKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedCounterClaim.getClaimAmount()).isEqualTo(counterClaimAmount);
        assertThat(savedCounterClaim.getAppliedForHwf()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedCounterClaim.getHwfReferenceNumber()).isEqualTo("HWF-123");
        assertThat(savedCounterClaim.getCounterClaimAgainst())
            .extracting(ListValue::getId)
            .containsExactly(againstPartyId.toString());
    }

    @Test
    void shouldNullHwfReferenceNumberWhenNotAppliedForHwf() {
        // Given
        UUID submittingPartyId = UUID.randomUUID();
        PartyEntity submittingParty = mock(PartyEntity.class);

        when(partyService.getPartyEntityByEntityId(submittingPartyId, TEST_CASE_REFERENCE))
            .thenReturn(submittingParty);

        EnterCounterClaimDetails enterCounterClaimDetails = EnterCounterClaimDetails.builder()
            .claimTypeOption(CounterClaimType.SOMETHING_ELSE)
            .appliedForHwf(VerticalYesNo.NO)
            .hwfReferenceNumber("stale-reference")
            .build();

        DynamicList submittingPartyList = DynamicList.builder()
            .value(DynamicListElement.builder().code(submittingPartyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(enterCounterClaimDetails)
            .partyRadioList(submittingPartyList)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        ArgumentCaptor<CounterClaim> counterClaimCaptor = ArgumentCaptor.forClass(CounterClaim.class);
        verify(counterClaimService).saveCounterClaim(
            eq(TEST_CASE_REFERENCE),
            counterClaimCaptor.capture(),
            eq(submittingParty));

        assertThat(counterClaimCaptor.getValue().getAppliedForHwf()).isEqualTo(VerticalYesNo.NO);
        assertThat(counterClaimCaptor.getValue().getHwfReferenceNumber()).isNull();
    }

    @Test
    void shouldSaveAllSelectedCounterClaimAgainstParties() {
        // Given
        UUID submittingPartyId = UUID.randomUUID();
        PartyEntity submittingParty = mock(PartyEntity.class);

        when(partyService.getPartyEntityByEntityId(submittingPartyId, TEST_CASE_REFERENCE))
            .thenReturn(submittingParty);

        EnterCounterClaimDetails enterCounterClaimDetails = EnterCounterClaimDetails.builder()
            .claimTypeOption(CounterClaimType.SOMETHING_ELSE)
            .build();

        DynamicList submittingPartyList = DynamicList.builder()
            .value(DynamicListElement.builder().code(submittingPartyId).build())
            .build();

        UUID claimantId = UUID.randomUUID();
        UUID defendantId = UUID.randomUUID();
        DynamicMultiSelectStringList partyMultiSelectionList = DynamicMultiSelectStringList.builder()
            .value(List.of(
                DynamicStringListElement.builder().code(claimantId.toString()).build(),
                DynamicStringListElement.builder().code(defendantId.toString()).build()
            ))
            .build();

        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(enterCounterClaimDetails)
            .partyRadioList(submittingPartyList)
            .partyMultiSelectionList(partyMultiSelectionList)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        ArgumentCaptor<CounterClaim> counterClaimCaptor = ArgumentCaptor.forClass(CounterClaim.class);
        verify(counterClaimService).saveCounterClaim(
            eq(TEST_CASE_REFERENCE),
            counterClaimCaptor.capture(),
            eq(submittingParty));

        assertThat(counterClaimCaptor.getValue().getCounterClaimAgainst())
            .extracting(ListValue::getId)
            .containsExactly(claimantId.toString(), defendantId.toString());
    }
}
