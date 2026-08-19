package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reasonable adjustment flags a defendant supplies via the cui-ra microsite are stored in the
 * draft table as serialised case data, so they have to survive a round trip through the draft mapper.
 * The payload below is as sent by the frontend.
 */
class DefendantFlagsDraftSerialisationTest {

    private static final String DRAFT_SAVE_PAYLOAD = """
        {
          "possessionClaimResponse": {
            "defendantFlags": {
              "details": [
                {
                  "value": {
                    "name": "Braille documents",
                    "path": [
                      { "value": "Party" },
                      { "value": "Reasonable adjustment" },
                      { "value": "I need documents in an alternative format" }
                    ],
                    "status": "Active",
                    "name_cy": "Dogfennau Braille",
                    "flagCode": "RA0012",
                    "flagComment": "",
                    "flagComment_cy": "",
                    "dateTimeCreated": "2026-07-29T11:26:50.201Z",
                    "hearingRelevant": "YES",
                    "availableExternally": "YES"
                  }
                }
              ],
              "partyName": "2 2",
              "roleOnCase": "Defendant"
            }
          }
        }
        """;

    /**
     * A complete draft as stored in draft.draft_case_data, with the flags alongside the answers the
     * defendant had already given.
     */
    private static final String STORED_DRAFT = """
        {
          "possessionClaimResponse": {
            "defendantFlags": {
              "details": [
                {
                  "value": {
                    "name": "Accessible toilet",
                    "path": [
                      { "value": "Party" },
                      { "value": "Reasonable adjustment" },
                      { "value": "I need adjustments to get to, into and around our buildings" }
                    ],
                    "status": "Active",
                    "name_cy": "Toiled hygyrch",
                    "flagCode": "RA0022",
                    "flagComment": "",
                    "flagComment_cy": "",
                    "dateTimeCreated": "2026-07-29T16:23:53.169Z",
                    "hearingRelevant": "YES",
                    "availableExternally": "YES"
                  }
                }
              ],
              "partyName": "2 2",
              "roleOnCase": "Defendant"
            },
            "defendantResponses": {
              "dateOfBirth": "2000-01-01",
              "disputeClaim": "NO",
              "hasSolicitor": "NO",
              "languageUsed": "ENGLISH",
              "contactByPost": "YES",
              "contactByEmail": "NO",
              "contactByPhone": "NO",
              "freeLegalAdvice": "NO",
              "makeCounterClaim": "NO",
              "statementOfTruth": { "accepted": "YES", "fullName": "q" },
              "completedSections": [
                "START_NOW_AND_DETAILS",
                "PERSONAL_DETAILS",
                "DISPUTE_AND_TENANCY",
                "SITUATION_AND_CIRCUMSTANCES",
                "INCOME_AND_EXPENDITURE",
                "UPLOAD_FILES"
              ],
              "defendantDocuments": [],
              "otherConsiderations": "NO",
              "householdCircumstances": {
                "otherTenants": "NO",
                "otherDependants": "NO",
                "dependantChildren": "NO",
                "exceptionalHardship": "NO",
                "alternativeAccommodation": "NO",
                "shareIncomeExpenseDetails": "NO",
                "shareAdditionalCircumstances": "NO"
              },
              "tenancyTypeConfirmation": "YES",
              "defendantNameConfirmation": "YES",
              "propertyAddressConfirmation": "YES"
            },
            "defendantContactDetails": {
              "party": {
                "address": {
                  "County": "",
                  "Country": "United Kingdom",
                  "PostCode": "W3 7RX",
                  "PostTown": "London",
                  "AddressLine1": "1 Second Avenue",
                  "AddressLine2": "",
                  "AddressLine3": ""
                }
              }
            }
          }
        }
        """;

    private final ObjectMapper underTest = new JacksonConfiguration().draftCaseDataObjectMapper();

    @Test
    void shouldReadDefendantFlagsFromAStoredDraftAlongsideTheAnswers() throws JsonProcessingException {
        // When
        PossessionClaimResponse response =
            underTest.readValue(STORED_DRAFT, PCSCase.class).getPossessionClaimResponse();

        // Then
        assertThat(response.getDefendantResponses()).isNotNull();
        assertThat(response.getDefendantContactDetails()).isNotNull();
        assertThat(response.getDefendantFlags()).isNotNull();
        assertThat(response.getDefendantFlags().getDetails())
            .extracting(detail -> detail.getValue().getFlagCode())
            .containsExactly("RA0022");
    }

    @Test
    void shouldReadDefendantFlagsSuppliedByReasonableAdjustmentsService() throws JsonProcessingException {
        // When
        PCSCase caseData = underTest.readValue(DRAFT_SAVE_PAYLOAD, PCSCase.class);

        // Then
        Flags defendantFlags = caseData.getPossessionClaimResponse().getDefendantFlags();
        assertThat(defendantFlags.getPartyName()).isEqualTo("2 2");
        assertThat(defendantFlags.getRoleOnCase()).isEqualTo("Defendant");
        assertThat(defendantFlags.getDetails()).hasSize(1);

        FlagDetail flagDetail = defendantFlags.getDetails().getFirst().getValue();
        assertThat(flagDetail.getFlagCode()).isEqualTo("RA0012");
        assertThat(flagDetail.getName()).isEqualTo("Braille documents");
        assertThat(flagDetail.getNameCy()).isEqualTo("Dogfennau Braille");
        assertThat(flagDetail.getStatus()).isEqualTo("Active");
        assertThat(flagDetail.getHearingRelevant()).isEqualTo(YesOrNo.YES);
        assertThat(flagDetail.getAvailableExternally()).isEqualTo(YesOrNo.YES);
        assertThat(flagDetail.getDateTimeCreated())
            .isEqualTo(LocalDateTime.of(2026, 7, 29, 11, 26, 50, 201_000_000));
        assertThat(flagDetail.getPath())
            .extracting(ListValue::getValue)
            .containsExactly("Party", "Reasonable adjustment", "I need documents in an alternative format");
    }

    @Test
    void shouldWriteAndReadBackDefendantFlagsUnchanged() throws JsonProcessingException {
        // Given
        PCSCase caseData = underTest.readValue(DRAFT_SAVE_PAYLOAD, PCSCase.class);

        // When
        String savedDraft = underTest.writeValueAsString(caseData);
        PCSCase restored = underTest.readValue(savedDraft, PCSCase.class);

        // Then
        assertThat(restored.getPossessionClaimResponse().getDefendantFlags())
            .isEqualTo(caseData.getPossessionClaimResponse().getDefendantFlags());
    }
}
