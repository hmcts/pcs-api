package uk.gov.hmcts.reform.pcs.ccd.service.document;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocumentStoreException;
import uk.gov.hmcts.reform.pcs.exception.ErrorCode;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.META_DATA_FOR_DOCUMENT_ERROR;

@Service
public class DocumentImportService {

    private final PcsCaseService pcsCaseService;
    private final CaseDocumentClientApi caseDocumentClientApi;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final AuthTokenGenerator authTokenGenerator;
    private final DocumentIdExtractor documentIdExtractor;

    public DocumentImportService(
        PcsCaseService pcsCaseService,
        CaseDocumentClientApi caseDocumentClientApi,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        AuthTokenGenerator authTokenGenerator,
        DocumentIdExtractor documentIdExtractor
    ) {
        this.pcsCaseService = pcsCaseService;
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.authTokenGenerator = authTokenGenerator;
        this.documentIdExtractor = documentIdExtractor;
    }

    public DocumentEntity addDocumentToCase(long caseReference,
                                            String documentUrl,
                                            CaseFileCategory caseFileCategory) {
        return addDocumentToCase(pcsCaseService.loadCase(caseReference), documentUrl, caseFileCategory);
    }

    public DocumentEntity addDocumentToCase(PcsCaseEntity pcsCaseEntity,
                                            String documentUrl,
                                            CaseFileCategory caseFileCategory) {

        UUID documentId = documentIdExtractor.extractDocumentId(documentUrl);

        String authorization = systemUpdateUserTokenProvider.getAuthToken();
        String serviceAuthorization = authTokenGenerator.generate();

        Document documentMetadata;
        try {
            documentMetadata = caseDocumentClientApi.getMetadataForDocument(
                authorization,
                serviceAuthorization,
                documentId
            );
        } catch (FeignException e) {
            RedactionContext redactionContext = RedactionContext.builder()
                .value("Failed to retrieve document metadata from CDAM for document ", documentId).build();
            throw new DocumentStoreException(META_DATA_FOR_DOCUMENT_ERROR, redactionContext, e);
        }

        DocumentEntity documentEntity = DocumentEntity.builder()
            .fileName(documentMetadata.originalDocumentName)
            .documentId(documentId)
            .url(documentMetadata.links.self.href)
            .binaryUrl(documentMetadata.links.binary.href)
            .categoryId(caseFileCategory.getId())
            .build();

        pcsCaseEntity.addDocuments(List.of(documentEntity));

        return documentEntity;
    }

    public void deleteDocument(String documentUrl) {
        UUID documentId = documentIdExtractor.extractDocumentId(documentUrl);
        caseDocumentClientApi.deleteDocument(
            systemUpdateUserTokenProvider.getAuthToken(),
            authTokenGenerator.generate(),
            documentId,
            true
        );
    }

}
