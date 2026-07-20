package uk.gov.hmcts.reform.pcs.ccd.service.caseworker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAssociationService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseworkerDocumentService {

    public static final String GEN_APP_ID_PREFIX = "GEN_APP";
    public static final String COUNTERCLAIM_ID_PREFIX = "COUNTERCLAIM";
    public static final String NONE_PREFIX = "NONE";

    private final PcsCaseService pcsCaseService;
    private final DocumentService documentService;
    private final DocumentIdExtractor documentIdExtractor;
    private final DocumentRepository documentRepository;
    private final DocumentNameService documentNameService;
    private final PartyRepository partyRepository;
    private final DocumentAssociationService documentAssociationService;

    public DocumentEntity saveNewDocument(CaseworkerDocument caseworkerDocument, long caseReference) {
        Document document = caseworkerDocument.getDocument();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();

        CaseworkerDocumentType caseworkerDocumentType = getCaseworkerDocumentType(caseworkerDocument);

        UUID partyId = caseworkerDocument.getRelatedParty().getValue().getCode();

        DocumentEntity documentEntity = createDocumentEntity(document, caseworkerDocumentType);
        documentEntity.setIssueDate(caseworkerDocument.getIssueDate());

        String documentFilename = document.getFilename();
        if (caseworkerDocument.getIssueDate() != null) {
            documentFilename = documentNameService.appendDate(documentFilename, caseworkerDocument.getIssueDate());
        }

        documentFilename = documentAssociationService.applyAssociation(
            documentEntity,
            mainClaim,
            partyId,
            documentEntity.getType(),
            documentFilename,
            caseworkerDocument.getShowRelatedSubmissionsList(),
            caseworkerDocument.getRelatedSubmission()
        );

        documentEntity.setFileName(documentFilename);

        PartyEntity partyEntity = partyRepository.getReferenceById(partyId);
        documentEntity.setParty(partyEntity);

        pcsCaseEntity.addDocument(documentEntity);

        return documentRepository.save(documentEntity);
    }

    private static CaseworkerDocumentType getCaseworkerDocumentType(CaseworkerDocument caseworkerDocument) {

        if (caseworkerDocument.getShowRelatedSubmissionsList() == VerticalYesNo.YES
            && !getSelectedCode(caseworkerDocument.getRelatedSubmission()).equals(NONE_PREFIX)) {
            return null;
        }

        DynamicStringList documentTypeList;
        if (caseworkerDocument.getShowRelatedSubmissionsList() == VerticalYesNo.YES) {
            documentTypeList = caseworkerDocument.getRelatedSubmissionsDocumentType();
        } else {
            documentTypeList = caseworkerDocument.getStandaloneDocumentType();
        }

        DynamicStringListElement selectedItem = documentTypeList.getValue();
        if (selectedItem != null) {
            String code = selectedItem.getCode();
            return CaseworkerDocumentType.valueOf(code);
        } else {
            return null;
        }
    }

    private DocumentEntity createDocumentEntity(Document document, CaseworkerDocumentType caseworkerDocumentType) {
        DocumentType documentType = mapToDocumentType(caseworkerDocumentType);

        return DocumentEntity.builder()
            .url(document.getUrl())
            .binaryUrl(document.getBinaryUrl())
            .documentId(documentIdExtractor.extractDocumentId(document.getUrl()))
            .type(documentType)
            .build();
    }

    private static String getSelectedCode(DynamicStringList relatedSubmission) {
        return Optional.ofNullable(relatedSubmission)
            .map(DynamicStringList::getValue)
            .map(DynamicStringListElement::getCode)
            .orElse(null);
    }

    public DocumentType mapToDocumentType(CaseworkerDocumentType caseworkerDocumentType) {
        return documentService.mapCaseworkerDocumentTypeToDocumentType(caseworkerDocumentType);
    }

}
