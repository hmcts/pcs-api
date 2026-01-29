package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimantProvidedInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantProvided;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class RespondPossessionClaimDraftServiceTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Captor
    private ArgumentCaptor<PCSCase> pcsCaseCaptor;

    private RespondPossessionClaimDraftService underTest;

    @BeforeEach
    void setUp() {
        underTest = new RespondPossessionClaimDraftService(draftCaseDataService);
    }

    @Test
    void shouldReturnTrueWhenDraftExists() {
        // Given
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(true);

        // When
        boolean result = underTest.exists(CASE_REFERENCE);

        // Then
        assertThat(result).isTrue();
        verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldReturnFalseWhenDraftDoesNotExist() {
        // Given
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(false);

        // When
        boolean result = underTest.exists(CASE_REFERENCE);

        // Then
        assertThat(result).isFalse();
        verify(draftCaseDataService).hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldLoadExistingDraftAndMergeWithPayloadData() {
        // Given - Draft data from database
        Party draftParty = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(draftParty)
            .contactByPhone(YesOrNo.YES)
            .build();

        DefendantProvided defendantProvided = DefendantProvided.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse draftResponse = PossessionClaimResponse.builder()
            .defendantProvided(defendantProvided)
            .build();

        PCSCase draftData = PCSCase.builder()
            .possessionClaimResponse(draftResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();

        // Given - Payload data from CCD (contains fields like legislativeCountry)
        PCSCase payloadData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .formattedPropertyAddress("123 Test Street, London")
            .feeAmount("100.00")
            .build();

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draftData));

        // When
        PCSCase result = underTest.load(CASE_REFERENCE, payloadData);

        // Then - Verify draft data overlays onto payload
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isEqualTo(draftResponse);
        assertThat(result.getPossessionClaimResponse().getDefendantProvided()
            .getContactDetails().getParty().getFirstName()).isEqualTo("John");
        assertThat(result.getPossessionClaimResponse().getDefendantProvided()
            .getContactDetails().getParty().getLastName()).isEqualTo("Doe");
        assertThat(result.getPossessionClaimResponse().getDefendantProvided()
            .getContactDetails().getContactByPhone()).isEqualTo(YesOrNo.YES);
        assertThat(result.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);  // Drafts always have NO
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);

        // Then - Verify payload fields are preserved (no data loss from toBuilder)
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(result.getFormattedPropertyAddress()).isEqualTo("123 Test Street, London");
        assertThat(result.getFeeAmount()).isEqualTo("100.00");

        // Then - Verify original payload is immutable (not mutated by toBuilder)
        assertThat(payloadData.getPossessionClaimResponse()).isNull();
        assertThat(payloadData.getHasUnsubmittedCaseData()).isNull();
        assertThat(payloadData.getSubmitDraftAnswers()).isNull();

        verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenDraftNotFound() {
        // Given
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.load(CASE_REFERENCE, PCSCase.builder().build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Draft not found for case " + CASE_REFERENCE);
    }

    @Test
    void shouldInitializeNewDraftAndMergeWithPayloadData() {
        // Given - Initial response from database
        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Smith")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .build();

        DefendantProvided defendantProvided = DefendantProvided.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder()
            .defendantProvided(defendantProvided)
            .build();

        // Given - Payload data from CCD (contains fields like legislativeCountry)
        PCSCase payloadData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .formattedPropertyAddress("456 Property Lane, Cardiff")
            .feeAmount("200.00")
            .caseTitleMarkdown("Test Case Title")
            .build();

        // When
        PCSCase result = underTest.initialize(CASE_REFERENCE, initialResponse, payloadData);

        // Then - Verify initial response overlays onto payload
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isEqualTo(initialResponse);
        assertThat(result.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);

        // Then - Verify payload fields are preserved (no data loss from toBuilder)
        assertThat(result.getLegislativeCountry()).isEqualTo(LegislativeCountry.WALES);
        assertThat(result.getFormattedPropertyAddress()).isEqualTo("456 Property Lane, Cardiff");
        assertThat(result.getFeeAmount()).isEqualTo("200.00");
        assertThat(result.getCaseTitleMarkdown()).isEqualTo("Test Case Title");

        // Then - Verify original payload is immutable (not mutated by toBuilder)
        assertThat(payloadData.getPossessionClaimResponse()).isNull();
        assertThat(payloadData.getSubmitDraftAnswers()).isNull();

        // Then - Verify only filtered data is saved to database (NOT payload fields)
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase patchedDraft = pcsCaseCaptor.getValue();
        assertThat(patchedDraft.getPossessionClaimResponse()).isEqualTo(initialResponse);
        assertThat(patchedDraft.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);
        assertThat(patchedDraft.getLegislativeCountry()).isNull(); // Should NOT save payload fields
        assertThat(patchedDraft.getFormattedPropertyAddress()).isNull();
        assertThat(patchedDraft.getFeeAmount()).isNull();
    }

    @Test
    void shouldSaveDraftWithCorrectDataStructure() {
        // Given - New data from defendant
        Party party = Party.builder()
            .firstName("John")
            .lastName("Doe")
            .emailAddress("john@example.com")
            .phoneNumber("07700900000")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(party)
            .contactByPhone(YesOrNo.YES)
            .build();

        DefendantProvided defendantProvided = DefendantProvided.builder()
            .contactDetails(contactDetails)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantProvided(defendantProvided)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        underTest.save(CASE_REFERENCE, caseData);

        // Then
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse()).isNotNull();

        // Verify claimantProvided is NOT in patch (will be preserved by merge logic)
        assertThat(savedDraft.getPossessionClaimResponse().getClaimantProvided()).isNull();

        // Verify defendantProvided is in the patch
        DefendantProvided savedDefendantProvided = savedDraft.getPossessionClaimResponse().getDefendantProvided();
        assertThat(savedDefendantProvided).isNotNull();
        assertThat(savedDefendantProvided.getContactDetails()).isNotNull();
        assertThat(savedDefendantProvided.getContactDetails().getParty()).isNotNull();
        assertThat(savedDefendantProvided.getContactDetails().getParty().getFirstName()).isEqualTo("John");
        assertThat(savedDefendantProvided.getContactDetails().getParty().getLastName()).isEqualTo("Doe");
        assertThat(savedDefendantProvided.getContactDetails().getParty().getEmailAddress())
            .isEqualTo("john@example.com");
        assertThat(savedDefendantProvided.getContactDetails().getParty().getPhoneNumber())
            .isEqualTo("07700900000");
        assertThat(savedDefendantProvided.getContactDetails().getContactByPhone()).isEqualTo(YesOrNo.YES);
        assertThat(savedDraft.getSubmitDraftAnswers()).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldPreventClaimantProvidedFromBeingModified() {
        // Given - Malicious client tries to modify claimantProvided
        Party tamperedClaimantParty = Party.builder()
            .orgName("HACKED - Modified Landlord")
            .nameKnown(VerticalYesNo.NO)
            .build();

        ClaimantProvidedInfo tamperedClaimantProvided = ClaimantProvidedInfo.builder()
            .party(tamperedClaimantParty)
            .tenancyType("HACKED - Different tenancy")
            .dailyRentAmount(new java.math.BigDecimal("1"))  // Tampered amount
            .rentArrearsOwed(new java.math.BigDecimal("1"))  // Tampered amount
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        DefendantProvided defendantProvided = DefendantProvided.builder()
            .contactDetails(DefendantContactDetails.builder()
                .party(Party.builder().firstName("John").build())
                .build())
            .build();

        PossessionClaimResponse tamperedResponse = PossessionClaimResponse.builder()
            .claimantProvided(tamperedClaimantProvided)  // Attempt to tamper
            .defendantProvided(defendantProvided)
            .build();

        PCSCase caseDataWithTamperedClaimant = PCSCase.builder()
            .possessionClaimResponse(tamperedResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        underTest.save(CASE_REFERENCE, caseDataWithTamperedClaimant);

        // Then
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();

        // Verify tampered claimantProvided is NOT in patch (completely ignored)
        ClaimantProvidedInfo savedClaimantProvided = savedDraft.getPossessionClaimResponse().getClaimantProvided();
        assertThat(savedClaimantProvided).isNull();

        // Verify defendantProvided is in the patch
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantProvided()).isNotNull();
    }

    @Test
    void shouldMergeDefendantProvidedWhenUserEditsOnlyFirstName() {
        // Given - User edited ONLY firstName (UI sends back complete defendantProvided with updated firstName)
        uk.gov.hmcts.ccd.sdk.type.AddressUK initialAddress = uk.gov.hmcts.ccd.sdk.type.AddressUK.builder()
            .addressLine1("123 Initial Street")
            .postCode("SW1A 1AA")
            .build();

        Party updatedParty = Party.builder()
            .firstName("Jonathan")  // ← CHANGED
            .lastName("Doe")        // ← UNCHANGED
            .emailAddress("john@example.com")  // ← UNCHANGED
            .phoneNumber("07700900000")  // ← UNCHANGED
            .address(initialAddress)  // ← UNCHANGED
            .build();

        PossessionClaimResponse userSubmittedResponse = PossessionClaimResponse.builder()
            .defendantProvided(DefendantProvided.builder()
                .contactDetails(DefendantContactDetails.builder()
                    .party(updatedParty)
                    .contactByPhone(YesOrNo.YES)
                    .build())
                .build())
            .build();

        PCSCase userSubmittedData = PCSCase.builder()
            .possessionClaimResponse(userSubmittedResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        underTest.save(CASE_REFERENCE, userSubmittedData);

        // Then - Verify patch contains only defendantProvided
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        PossessionClaimResponse savedResponse = savedDraft.getPossessionClaimResponse();

        // Verify claimantProvided is NOT in patch (merge logic will preserve from DB)
        assertThat(savedResponse.getClaimantProvided()).isNull();

        // Verify defendantProvided is in the patch with user's edits
        Party savedParty = savedResponse.getDefendantProvided().getContactDetails().getParty();
        assertThat(savedParty.getFirstName()).isEqualTo("Jonathan"); // ← Updated
        assertThat(savedParty.getLastName()).isEqualTo("Doe");  // ← Same
        assertThat(savedParty.getEmailAddress()).isEqualTo("john@example.com");  // ← Same
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07700900000");  // ← Same
        assertThat(savedParty.getAddress()).isEqualTo(initialAddress);  // ← Same
    }

    @Test
    void shouldMergeDefendantProvidedWhenUserEditsOnlyAddress() {
        // Given - User edited ONLY address (UI sends back complete defendantProvided with new address)
        uk.gov.hmcts.ccd.sdk.type.AddressUK newAddress = uk.gov.hmcts.ccd.sdk.type.AddressUK.builder()
            .addressLine1("456 New Street")  // ← CHANGED
            .addressLine2("Apartment 10B")   // ← NEW
            .postCode("W1A 1AA")             // ← CHANGED
            .build();

        Party updatedParty = Party.builder()
            .firstName("John")       // ← UNCHANGED
            .lastName("Doe")         // ← UNCHANGED
            .emailAddress("john@example.com")  // ← UNCHANGED
            .phoneNumber("07700900000")  // ← UNCHANGED
            .address(newAddress)     // ← CHANGED
            .build();

        PossessionClaimResponse userSubmittedResponse = PossessionClaimResponse.builder()
            .defendantProvided(DefendantProvided.builder()
                .contactDetails(DefendantContactDetails.builder()
                    .party(updatedParty)
                    .contactByPhone(YesOrNo.YES)
                    .build())
                .build())
            .build();

        PCSCase userSubmittedData = PCSCase.builder()
            .possessionClaimResponse(userSubmittedResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        underTest.save(CASE_REFERENCE, userSubmittedData);

        // Then - Verify patch contains only defendantProvided
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        PossessionClaimResponse savedResponse = savedDraft.getPossessionClaimResponse();

        // Verify claimantProvided is NOT in patch (merge logic will preserve from DB)
        assertThat(savedResponse.getClaimantProvided()).isNull();

        // Verify defendantProvided is in the patch with user's edits
        Party savedParty = savedResponse.getDefendantProvided().getContactDetails().getParty();
        assertThat(savedParty.getFirstName()).isEqualTo("John");  // ← Same
        assertThat(savedParty.getLastName()).isEqualTo("Doe");  // ← Same
        assertThat(savedParty.getEmailAddress()).isEqualTo("john@example.com");  // ← Same
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07700900000");  // ← Same
        assertThat(savedParty.getAddress()).isEqualTo(newAddress);  // ← Updated
        assertThat(savedParty.getAddress().getAddressLine1()).isEqualTo("456 New Street");
        assertThat(savedParty.getAddress().getAddressLine2()).isEqualTo("Apartment 10B");
        assertThat(savedParty.getAddress().getPostCode()).isEqualTo("W1A 1AA");
    }

    @Test
    void shouldReplaceEntireDefendantProvidedOnSave() {
        // Given - User submits with updated firstName and lastName
        uk.gov.hmcts.ccd.sdk.type.AddressUK initialAddress = uk.gov.hmcts.ccd.sdk.type.AddressUK.builder()
            .addressLine1("123 Initial Street")
            .postCode("SW1A 1AA")
            .build();

        Party updatedParty = Party.builder()
            .firstName("Jonathan")  // ← CHANGED
            .lastName("Smith")      // ← CHANGED
            .orgName("Housing Association Ltd")  // Same
            .nameKnown(VerticalYesNo.YES)  // Same (read-only routing flag)
            .emailAddress("john@example.com")  // Same
            .phoneNumber("07700900000")  // Same
            .address(initialAddress)  // Same
            .addressKnown(VerticalYesNo.YES)  // Same (read-only routing flag)
            .addressSameAsProperty(VerticalYesNo.NO)  // Same (read-only routing flag)
            .build();

        PossessionClaimResponse userSubmittedResponse = PossessionClaimResponse.builder()
            .defendantProvided(DefendantProvided.builder()
                .contactDetails(DefendantContactDetails.builder()
                    .party(updatedParty)
                    .contactByPhone(YesOrNo.YES)
                    .build())
                .build())
            .build();

        PCSCase userSubmittedData = PCSCase.builder()
            .possessionClaimResponse(userSubmittedResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        underTest.save(CASE_REFERENCE, userSubmittedData);

        // Then - Verify patch contains ONLY defendantProvided (no claimantProvided)
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            pcsCaseCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        PossessionClaimResponse savedResponse = savedDraft.getPossessionClaimResponse();

        // Verify claimantProvided is NOT in patch (merge logic will preserve from DB)
        assertThat(savedResponse.getClaimantProvided()).isNull();

        // Verify defendantProvided is in the patch
        DefendantProvided savedDefendantProvided = savedResponse.getDefendantProvided();
        assertThat(savedDefendantProvided).isEqualTo(userSubmittedResponse.getDefendantProvided());

        // Verify updated fields in the defendantProvided
        Party savedParty = savedDefendantProvided.getContactDetails().getParty();
        assertThat(savedParty.getFirstName()).isEqualTo("Jonathan");  // ← Updated
        assertThat(savedParty.getLastName()).isEqualTo("Smith");  // ← Updated
        assertThat(savedParty.getOrgName()).isEqualTo("Housing Association Ltd");  // Same
        assertThat(savedParty.getNameKnown()).isEqualTo(VerticalYesNo.YES);  // Same
        assertThat(savedParty.getAddressKnown()).isEqualTo(VerticalYesNo.YES);  // Same
        assertThat(savedParty.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.NO);  // Same
    }

}
