package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.entercounterclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long TEST_CASE_REFERENCE = 1234L;

    @Mock
    private PartyService partyService;
    @Mock
    private CounterClaimService counterClaimService;
    @Mock
    private AddressFormatter addressFormatter;

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        when(addressFormatter.formatShortAddress(any(), eq(COMMA_DELIMITER)))
            .thenReturn("1 High Street, London, W1 1AA");

        underTest = new SubmitEventHandler(partyService, counterClaimService, addressFormatter);
    }

    private SubmitResponse<State> submit(PCSCase caseData) {
        return underTest.submit(new EventPayload<>(TEST_CASE_REFERENCE, caseData, null));
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
        submit(caseData);

        // Then
        ArgumentCaptor<CounterClaim> counterClaimCaptor = ArgumentCaptor.forClass(CounterClaim.class);
        verify(counterClaimService).saveCaseworkerEnteredCounterClaim(
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
        submit(caseData);

        // Then
        ArgumentCaptor<CounterClaim> counterClaimCaptor = ArgumentCaptor.forClass(CounterClaim.class);
        verify(counterClaimService).saveCaseworkerEnteredCounterClaim(
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
        submit(caseData);

        // Then
        ArgumentCaptor<CounterClaim> counterClaimCaptor = ArgumentCaptor.forClass(CounterClaim.class);
        verify(counterClaimService).saveCaseworkerEnteredCounterClaim(
            eq(TEST_CASE_REFERENCE),
            counterClaimCaptor.capture(),
            eq(submittingParty));

        assertThat(counterClaimCaptor.getValue().getCounterClaimAgainst())
            .extracting(ListValue::getId)
            .containsExactly(claimantId.toString(), defendantId.toString());
    }

    @ParameterizedTest
    @MethodSource("emptyCounterClaimAgainstSelections")
    void shouldSaveNoCounterClaimAgainstPartiesWhenNothingSelected(DynamicMultiSelectStringList selectedParties) {
        // Given
        UUID submittingPartyId = UUID.randomUUID();
        PartyEntity submittingParty = mock(PartyEntity.class);

        when(partyService.getPartyEntityByEntityId(submittingPartyId, TEST_CASE_REFERENCE))
            .thenReturn(submittingParty);

        DynamicList submittingPartyList = DynamicList.builder()
            .value(DynamicListElement.builder().code(submittingPartyId).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(EnterCounterClaimDetails.builder()
                .claimTypeOption(CounterClaimType.SOMETHING_ELSE)
                .build())
            .partyRadioList(submittingPartyList)
            .partyMultiSelectionList(selectedParties)
            .build();

        // When
        submit(caseData);

        // Then
        ArgumentCaptor<CounterClaim> counterClaimCaptor = ArgumentCaptor.forClass(CounterClaim.class);
        verify(counterClaimService).saveCaseworkerEnteredCounterClaim(
            eq(TEST_CASE_REFERENCE),
            counterClaimCaptor.capture(),
            eq(submittingParty));

        assertThat(counterClaimCaptor.getValue().getCounterClaimAgainst()).isEmpty();
    }

    private static Stream<DynamicMultiSelectStringList> emptyCounterClaimAgainstSelections() {
        return Stream.of(
            null,                                          // selection list not set
            DynamicMultiSelectStringList.builder().build() // selection list present but no value
        );
    }

    @Test
    void shouldReturnSubmittedConfirmationWhenNotAppliedForHwf() {
        // Given
        UUID submittingPartyId = UUID.randomUUID();
        when(partyService.getPartyEntityByEntityId(submittingPartyId, TEST_CASE_REFERENCE))
            .thenReturn(mock(PartyEntity.class));

        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(EnterCounterClaimDetails.builder()
                .claimTypeOption(CounterClaimType.SOMETHING_ELSE)
                .appliedForHwf(VerticalYesNo.NO)
                .build())
            .partyRadioList(DynamicList.builder()
                .value(DynamicListElement.builder().code(submittingPartyId).build())
                .build())
            .caseNameHmctsInternal("Smith v Jones")
            .build();

        // When
        SubmitResponse<State> response = submit(caseData);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Counterclaim submitted")
            .contains("Case number: " + TEST_CASE_REFERENCE)
            .contains("1 High Street, London, W1 1AA")
            .contains("Smith v Jones")
            .doesNotContain("pending issue")
            .doesNotContain("What happens next");
    }

    @Test
    void shouldReturnPendingIssueConfirmationWhenAppliedForHwf() {
        // Given
        UUID submittingPartyId = UUID.randomUUID();
        when(partyService.getPartyEntityByEntityId(submittingPartyId, TEST_CASE_REFERENCE))
            .thenReturn(mock(PartyEntity.class));

        PCSCase caseData = PCSCase.builder()
            .enterCounterClaim(EnterCounterClaimDetails.builder()
                .claimTypeOption(CounterClaimType.SOMETHING_ELSE)
                .appliedForHwf(VerticalYesNo.YES)
                .hwfReferenceNumber("HWF-123")
                .build())
            .partyRadioList(DynamicList.builder()
                .value(DynamicListElement.builder().code(submittingPartyId).build())
                .build())
            .caseNameHmctsInternal("Smith v Jones")
            .build();

        // When
        SubmitResponse<State> response = submit(caseData);

        // Then
        assertThat(response.getConfirmationBody())
            .contains("Counterclaim pending issue")
            .contains("Case number: " + TEST_CASE_REFERENCE)
            .contains("Smith v Jones")
            .contains("What happens next")
            .contains("Help With Fees application")
            .contains("you must issue the counterclaim")
            .doesNotContain("Counterclaim submitted");
    }
}
