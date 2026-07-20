package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.NONE_PREFIX;

@Service
@RequiredArgsConstructor
public class DocumentAmendService {

    private final PcsCaseService pcsCaseService;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final DocumentNameService documentNameService;
    private final PartyService partyService;
    private final DocumentAssociationService documentAssociationService;

    public AmendedDocument amendDocument(PCSCase caseData, long caseReference) {
        DocumentAmendDetails amendDetails = caseData.getDocumentAmendDetails();
        DocumentEntity documentEntity = loadDocument(amendDetails);
        final PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        UUID partyId = selectedPartyId(amendDetails.getRelatedParty());
        PartyEntity partyEntity = partyService.getPartyEntityById(partyId, caseReference);

        DocumentType documentType = resolveDocumentType(amendDetails);
        documentEntity.setType(documentType);
        documentEntity.setIssueDate(amendDetails.getIssueDate());
        documentEntity.setParty(partyEntity);

        String fileName = fileNameWithOriginalExtension(amendDetails, documentEntity);
        if (amendDetails.getIssueDate() != null) {
            fileName = documentNameService.appendDate(fileName, amendDetails.getIssueDate());
        }
        final String confirmationFileName = FilenameUtils.getBaseName(fileName);

        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();
        fileName = documentAssociationService.applyAssociation(
            documentEntity,
            mainClaim,
            partyId,
            documentType,
            fileName,
            amendDetails.getShowRelatedSubmissionsList(),
            amendDetails.getRelatedSubmission()
        );
        documentEntity.setFileName(fileName);

        documentRepository.save(documentEntity);

        return new AmendedDocument(confirmationFileName, partyService.getPartyName(partyEntity));
    }

    private DocumentEntity loadDocument(DocumentAmendDetails amendDetails) {
        return documentRepository.findById(UUID.fromString(amendDetails.getSelectedDocumentId()))
            .orElseThrow(() -> new IllegalStateException(
                "No document found for ID: " + amendDetails.getSelectedDocumentId()
            ));
    }

    private DocumentType resolveDocumentType(DocumentAmendDetails amendDetails) {
        if (amendDetails.getShowRelatedSubmissionsList() == VerticalYesNo.YES
            && !NONE_PREFIX.equals(getSelectedCode(amendDetails.getRelatedSubmission()))) {
            return null;
        }

        DynamicStringList documentTypeList = amendDetails.getShowRelatedSubmissionsList() == VerticalYesNo.YES
            ? amendDetails.getRelatedSubmissionsDocumentType()
            : amendDetails.getStandaloneDocumentType();

        String selectedCode = getSelectedCode(documentTypeList);
        return Optional.ofNullable(selectedCode)
            .map(CaseworkerDocumentType::valueOf)
            .map(documentService::mapCaseworkerDocumentTypeToDocumentType)
            .orElse(null);
    }

    private static UUID selectedPartyId(DynamicList relatedParty) {
        return Optional.ofNullable(relatedParty)
            .map(DynamicList::getValue)
            .map(DynamicListElement::getCode)
            .orElseThrow(() -> new IllegalStateException("No related party selected"));
    }

    private static String fileNameWithOriginalExtension(
        DocumentAmendDetails amendDetails,
        DocumentEntity documentEntity
    ) {
        String baseName = FilenameUtils.getBaseName(amendDetails.getAmendedFileName());
        String originalFileName = Optional.ofNullable(documentEntity.getFileName())
            .orElse(amendDetails.getSelectedDocumentFileName());
        String extension = FilenameUtils.getExtension(originalFileName);
        return extension.isBlank() ? baseName : baseName + "." + extension;
    }

    private static String getSelectedCode(DynamicStringList dynamicStringList) {
        return dynamicStringList == null ? null : dynamicStringList.getValueCode();
    }

    public record AmendedDocument(String fileName, String partyName) {
    }
}
