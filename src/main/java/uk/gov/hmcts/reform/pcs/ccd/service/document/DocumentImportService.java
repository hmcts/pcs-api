package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.List;
import java.util.UUID;

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

        UUID documentId = documentIdExtractor.extractDocumentId(documentUrl);

        String authorization = systemUpdateUserTokenProvider.getAuthToken();
        String serviceAuthorization = authTokenGenerator.generate();

        Document documentMetadata = caseDocumentClientApi.getMetadataForDocument(
            authorization,
            serviceAuthorization,
            documentId
        );

        DocumentEntity documentEntity = DocumentEntity.builder()
            .fileName(documentMetadata.originalDocumentName)
            .documentId(documentId)
            .url(documentMetadata.links.self.href)
            .binaryUrl(documentMetadata.links.binary.href)
            .categoryId(caseFileCategory.getId())
            .build();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        pcsCaseEntity.addDocuments(List.of(documentEntity));

        return documentEntity;
    }

}
