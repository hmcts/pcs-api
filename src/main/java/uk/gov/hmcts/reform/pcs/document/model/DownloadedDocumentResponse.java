package uk.gov.hmcts.reform.pcs.document.model;

import org.springframework.core.io.Resource;

public record DownloadedDocumentResponse(Resource file, String fileName, String mimeType) {
}
