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
        // Given - Existing draft with claimantProvided (read-only)
        Party claimantProvidedParty = Party.builder()
            .orgName("Original Landlord")
            .build();

        PossessionClaimResponse existingDraftResponse = PossessionClaimResponse.builder()
            .claimantProvided(ClaimantProvidedInfo.builder()
                .party(claimantProvidedParty)
                .tenancyType("Assured tenancy")
                .legislativeCountry(LegislativeCountry.ENGLAND)
                .build())
            .build();

        PCSCase existingDraft = PCSCase.builder()
            .possessionClaimResponse(existingDraftResponse)
            .build();

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(existingDraft));

        // New data from defendant
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

        // Verify claimantProvided is preserved (read-only)
        assertThat(savedDraft.getPossessionClaimResponse().getClaimantProvided()).isNotNull();
        assertThat(savedDraft.getPossessionClaimResponse().getClaimantProvided().getParty().getOrgName())
            .isEqualTo("Original Landlord");

        // Verify defendantProvided is updated
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
        // Given - Existing draft with original claimantProvided
        Party originalClaimantParty = Party.builder()
            .orgName("Original Landlord Ltd")
            .nameKnown(VerticalYesNo.YES)
            .build();

        ClaimantProvidedInfo originalClaimantProvided = ClaimantProvidedInfo.builder()
            .party(originalClaimantParty)
            .tenancyType("Assured tenancy")
            .dailyRentAmount(new java.math.BigDecimal("17614"))
            .rentArrearsOwed(new java.math.BigDecimal("122200"))
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        PossessionClaimResponse existingDraftResponse = PossessionClaimResponse.builder()
            .claimantProvided(originalClaimantProvided)
            .build();

        PCSCase existingDraft = PCSCase.builder()
            .possessionClaimResponse(existingDraftResponse)
            .build();

        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(existingDraft));

        // Malicious client tries to modify claimantProvided
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

        // Verify claimantProvided is NOT modified - original values preserved
        ClaimantProvidedInfo savedClaimantProvided = savedDraft.getPossessionClaimResponse().getClaimantProvided();
        assertThat(savedClaimantProvided).isNotNull();
        assertThat(savedClaimantProvided.getParty().getOrgName()).isEqualTo("Original Landlord Ltd");
        assertThat(savedClaimantProvided.getParty().getNameKnown()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedClaimantProvided.getTenancyType()).isEqualTo("Assured tenancy");
        assertThat(savedClaimantProvided.getDailyRentAmount())
            .isEqualByComparingTo(new java.math.BigDecimal("17614"));
        assertThat(savedClaimantProvided.getRentArrearsOwed())
            .isEqualByComparingTo(new java.math.BigDecimal("122200"));
        assertThat(savedClaimantProvided.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);

        // Verify defendantProvided is updated
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantProvided()).isNotNull();
    }

}
