package uk.gov.hmcts.reform.pcs.document.model;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record DownloadedDocumentResponse(Resource file, String fileName, MediaType mediaType) {
}
