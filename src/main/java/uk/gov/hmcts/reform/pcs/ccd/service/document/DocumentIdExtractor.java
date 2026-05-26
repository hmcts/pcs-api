package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentIdExtractor {

    public UUID extractDocumentId(String documentUrl) {
        String[] urlParts = documentUrl.split("/");
        return UUID.fromString(urlParts[urlParts.length - 1]);
    }

}
