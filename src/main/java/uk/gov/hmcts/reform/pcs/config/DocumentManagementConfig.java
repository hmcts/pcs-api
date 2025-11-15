package uk.gov.hmcts.reform.pcs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class DocumentManagementConfig {

    private final CaseDocumentClientApi caseDocumentClientApi;

    public DocumentManagementConfig(CaseDocumentClientApi caseDocumentClientApi) {
        this.caseDocumentClientApi = caseDocumentClientApi;
    }

    @PostConstruct
    public void init() {
        log.info("=== DocumentManagementConfig initialized ===");
        log.info("CaseDocumentClientApi bean: {}", caseDocumentClientApi != null ? "AVAILABLE" : "NULL");
    }
}
