package uk.gov.hmcts.reform.pcs.ccd.service.accesscode;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tells a defendant's English and Welsh access-code letters apart (HDPI-9698 D2). Both have type
 * {@code DEFENDANT_ACCESS_CODE}. The Welsh letter's {@code file_name} starts "Defendant Access Code Welsh"; the
 * English letter keeps {@code file_name} null, as it always has, so any letter not marked Welsh is English.
 */
public final class AccessCodeDocuments {

    static final String WELSH_FILE_NAME_PREFIX = "Defendant Access Code Welsh";

    private AccessCodeDocuments() {
    }

    public static String welshFileName(UUID partyId) {
        return WELSH_FILE_NAME_PREFIX + " " + partyId;
    }

    public static boolean isWelsh(DocumentEntity document) {
        return document.getType() == DocumentType.DEFENDANT_ACCESS_CODE
            && StringUtils.startsWith(document.getFileName(), WELSH_FILE_NAME_PREFIX);
    }

    /**
     * Returns the party's access-code letters keyed by language. If the party has two letters in one language,
     * the first in list order wins, as the old first-match lookup in {@code ClaimPackSelector} did.
     */
    public static Map<AccessCodeLetterLanguage, DocumentEntity> byLanguage(Collection<DocumentEntity> documents,
                                                                           PartyEntity party) {
        return documents.stream()
            .filter(document -> document.getType() == DocumentType.DEFENDANT_ACCESS_CODE)
            .filter(document -> document.getParty() != null && document.getParty().getId().equals(party.getId()))
            .collect(Collectors.toMap(
                AccessCodeDocuments::languageOf,
                Function.identity(),
                (first, second) -> first,
                () -> new EnumMap<>(AccessCodeLetterLanguage.class)));
    }

    private static AccessCodeLetterLanguage languageOf(DocumentEntity document) {
        return isWelsh(document) ? AccessCodeLetterLanguage.WELSH : AccessCodeLetterLanguage.ENGLISH;
    }
}
