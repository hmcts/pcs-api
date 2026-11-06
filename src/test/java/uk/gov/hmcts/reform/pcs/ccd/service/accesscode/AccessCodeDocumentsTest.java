package uk.gov.hmcts.reform.pcs.ccd.service.accesscode;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage.ENGLISH;
import static uk.gov.hmcts.reform.pcs.ccd.service.accesscode.AccessCodeLetterLanguage.WELSH;

class AccessCodeDocumentsTest {

    private final PartyEntity partyA = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final PartyEntity partyB = PartyEntity.builder().id(UUID.randomUUID()).build();

    @Test
    void welshFileNameIsThePrefixAndThePartyId() {
        UUID partyId = UUID.randomUUID();

        assertThat(AccessCodeDocuments.welshFileName(partyId)).isEqualTo("Defendant Access Code Welsh " + partyId);
    }

    @Test
    void onlyAWelshNamedAccessCodeLetterIsWelsh() {
        UUID partyId = partyA.getId();

        assertThat(AccessCodeDocuments.isWelsh(document(DocumentType.DEFENDANT_ACCESS_CODE, partyA, null)))
            .isFalse();
        assertThat(AccessCodeDocuments.isWelsh(
            document(DocumentType.DEFENDANT_ACCESS_CODE, partyA, "Defendant Access Code " + partyId))).isFalse();
        assertThat(AccessCodeDocuments.isWelsh(
            document(DocumentType.DEFENDANT_ACCESS_CODE, partyA, AccessCodeDocuments.welshFileName(partyId))))
            .isTrue();
        assertThat(AccessCodeDocuments.isWelsh(
            document(DocumentType.CLAIM, partyA, AccessCodeDocuments.welshFileName(partyId)))).isFalse();
    }

    @Test
    void byLanguageKeysEachPartysLettersByLanguage() {
        DocumentEntity englishPinA = englishPin(partyA);
        DocumentEntity welshPinA = welshPin(partyA);
        DocumentEntity englishPinB = englishPin(partyB);
        List<DocumentEntity> documents = List.of(
            document(DocumentType.CLAIM, partyA, null),
            welshPinA,
            englishPinB,
            document(DocumentType.DEFENDANT_ACCESS_CODE, null, null),
            englishPinA);

        assertThat(AccessCodeDocuments.byLanguage(documents, partyA))
            .containsExactlyInAnyOrderEntriesOf(Map.of(ENGLISH, englishPinA, WELSH, welshPinA));
        assertThat(AccessCodeDocuments.byLanguage(documents, partyB))
            .containsExactlyInAnyOrderEntriesOf(Map.of(ENGLISH, englishPinB));
    }

    @Test
    void byLanguageKeepsTheFirstLetterWhenALanguageRepeats() {
        DocumentEntity firstEnglishPin = englishPin(partyA);
        DocumentEntity secondEnglishPin = englishPin(partyA);

        Map<AccessCodeLetterLanguage, DocumentEntity> letters =
            AccessCodeDocuments.byLanguage(List.of(firstEnglishPin, secondEnglishPin), partyA);

        assertThat(letters).hasSize(1);
        assertThat(letters.get(ENGLISH)).isSameAs(firstEnglishPin);
    }

    @Test
    void byLanguageIsEmptyForAPartyWithNoLetter() {
        assertThat(AccessCodeDocuments.byLanguage(List.of(englishPin(partyB)), partyA)).isEmpty();
    }

    private DocumentEntity englishPin(PartyEntity party) {
        return document(DocumentType.DEFENDANT_ACCESS_CODE, party, null);
    }

    private DocumentEntity welshPin(PartyEntity party) {
        return document(DocumentType.DEFENDANT_ACCESS_CODE, party, AccessCodeDocuments.welshFileName(party.getId()));
    }

    private DocumentEntity document(DocumentType type, PartyEntity party, String fileName) {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .type(type)
            .party(party)
            .fileName(fileName)
            .build();
    }
}
