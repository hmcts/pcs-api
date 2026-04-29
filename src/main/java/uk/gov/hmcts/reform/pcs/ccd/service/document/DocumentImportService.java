package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DocumentImportService {

    private final PcsCaseService pcsCaseService;
    private final CaseDocumentClientApi caseDocumentClientApi;
    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;

    // TODO: Add tests
    public void addDocumentToCase(long caseReference,
                                  String documentUrl,
                                  CaseFileCategory caseFileCategory) {

        String[] urlParts = documentUrl.split("/");
        UUID documentId = UUID.fromString(urlParts[urlParts.length - 1]); // TODO: Handle malformed

        String authorization = idamService.getSystemUserAuthorisation();
        String serviceAuthorization = authTokenGenerator.generate();

        Document documentMetadata = caseDocumentClientApi.getMetadataForDocument(
            authorization,
            serviceAuthorization,
            documentId
        );

        documentUrl = documentMetadata.links.self.href;
        String documentBinaryUrl = documentMetadata.links.binary.href;

        System.out.println(documentUrl);
        System.out.println(documentBinaryUrl);

        DocumentEntity documentEntity = DocumentEntity.builder()
            .fileName(documentMetadata.originalDocumentName)
            .url(documentMetadata.links.self.href)
            .binaryUrl(documentMetadata.links.binary.href)
            .categoryId(caseFileCategory.getId())
            .type(DocumentType.OTHER) // TODO: Remove?
            .build();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        pcsCaseEntity.addDocuments(List.of(documentEntity));
    }

}
