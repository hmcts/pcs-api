package uk.gov.hmcts.reform.pcs.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class DocumentDeletionIncompleteException extends RuntimeException {

    private final List<UUID> documentIds;
    private final Long caseReference;

    public DocumentDeletionIncompleteException(List<UUID> documentIds, Long caseReference) {
        super();
        this.documentIds = documentIds;
        this.caseReference = caseReference;
    }
}
