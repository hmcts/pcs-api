package uk.gov.hmcts.reform.pcs.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class DocumentDeletionIncompleteException extends RuntimeException {

    private final List<String> documentUrls;
    private final Long caseReference;

    public DocumentDeletionIncompleteException(List<String> documentUrls, Long caseReference) {
        super();
        this.documentUrls = documentUrls;
        this.caseReference = caseReference;
    }
}
