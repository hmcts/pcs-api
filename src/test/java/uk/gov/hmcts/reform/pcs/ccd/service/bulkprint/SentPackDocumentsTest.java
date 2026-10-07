package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDocumentRef;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SentPackDocumentsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SentPackDocuments underTest;

    @BeforeEach
    void setUp() {
        underTest = new SentPackDocuments(objectMapper);
    }

    @Test
    @DisplayName("Returns an empty key set for an empty activity log")
    void shouldReturnEmptyKeySetForEmptyLog() {
        assertThat(underTest.sentDocumentKeys(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Returns a key per document carried by a PACK_SENT SUCCESS row")
    void shouldReturnKeysForSentPackDocuments() throws Exception {
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        UUID documentOne = UUID.randomUUID();
        UUID documentTwo = UUID.randomUUID();
        PackDetails details = PackDetails.sent(LetterType.DEFENCE_PACK,
            List.of(new PackDocumentRef(documentOne, DocumentType.DEFENDANT_RESPONSE, 1, true),
                    new PackDocumentRef(documentTwo, DocumentType.COUNTERCLAIM, 1, true)),
            UUID.randomUUID());
        ClaimActivityLogEntity row = packRow(party, ClaimActivityType.PACK_SENT,
            ClaimActivityStatus.SUCCESS, objectMapper.writeValueAsString(details));

        Set<String> keys = underTest.sentDocumentKeys(List.of(row));

        assertThat(keys).containsExactlyInAnyOrder(
            SentPackDocuments.key(party.getId(), documentOne),
            SentPackDocuments.key(party.getId(), documentTwo));
    }

    @Test
    @DisplayName("Ignores a PACK_FAILED (non-SUCCESS) row")
    void shouldIgnoreNonSuccessRow() throws Exception {
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        PackDetails details = PackDetails.sent(LetterType.DEFENCE_PACK,
            List.of(new PackDocumentRef(UUID.randomUUID(), DocumentType.DEFENDANT_RESPONSE, 1, true)),
            UUID.randomUUID());
        ClaimActivityLogEntity row = packRow(party, ClaimActivityType.PACK_FAILED,
            ClaimActivityStatus.FAILURE, objectMapper.writeValueAsString(details));

        assertThat(underTest.sentDocumentKeys(List.of(row))).isEmpty();
    }

    @Test
    @DisplayName("Skips a row with unreadable details without throwing")
    void shouldSkipRowWithUnreadableDetails() {
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();
        ClaimActivityLogEntity malformed = packRow(party, ClaimActivityType.PACK_SENT,
            ClaimActivityStatus.SUCCESS, "{not-valid-json");
        ClaimActivityLogEntity empty = packRow(party, ClaimActivityType.PACK_SENT,
            ClaimActivityStatus.SUCCESS, "{}");

        assertThatCode(() -> assertThat(underTest.sentDocumentKeys(List.of(malformed, empty))).isEmpty())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Builds the composite key as partyId:documentId")
    void shouldBuildCompositeKey() {
        UUID partyId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        assertThat(SentPackDocuments.key(partyId, documentId)).isEqualTo(partyId + ":" + documentId);
    }

    private ClaimActivityLogEntity packRow(PartyEntity party, ClaimActivityType activityType,
                                           ClaimActivityStatus status, String details) {
        return ClaimActivityLogEntity.builder()
            .id(1)
            .party(party)
            .activityType(activityType)
            .status(status)
            .details(details)
            .build();
    }
}
