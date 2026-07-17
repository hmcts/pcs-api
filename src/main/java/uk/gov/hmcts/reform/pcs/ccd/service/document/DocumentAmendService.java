package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.COUNTERCLAIM_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.NONE_PREFIX;

@Service
@RequiredArgsConstructor
public class DocumentAmendService {

    private final PcsCaseService pcsCaseService;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final DocumentNameService documentNameService;
    private final GenAppService genAppService;
    private final CounterClaimRepository counterClaimRepository;
    private final PartyService partyService;

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
        fileName = applyAssociation(amendDetails, documentEntity, mainClaim, partyId, documentType, fileName);
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

    private String applyAssociation(
        DocumentAmendDetails amendDetails,
        DocumentEntity documentEntity,
        ClaimEntity mainClaim,
        UUID partyId,
        DocumentType documentType,
        String fileName
    ) {
        if (amendDetails.getShowRelatedSubmissionsList() == VerticalYesNo.YES) {
            String code = getSelectedCode(amendDetails.getRelatedSubmission());
            String[] codeParts = code == null ? new String[0] : code.split(":");
            if (codeParts.length > 0 && GEN_APP_ID_PREFIX.equals(codeParts[0])) {
                GenAppEntity genAppEntity = genAppService.loadGenApp(UUID.fromString(codeParts[1]));
                documentEntity.setGeneralApplication(genAppEntity);
                documentEntity.setCounterClaim(null);
                documentEntity.setCategoryId(CaseFileCategory.APPLICATIONS.getId());
                return documentNameService.appendGenAppPostfix(fileName, genAppEntity, mainClaim, partyId);
            }
            if (codeParts.length > 0 && COUNTERCLAIM_ID_PREFIX.equals(codeParts[0])) {
                CounterClaimEntity counterClaimEntity = counterClaimRepository
                    .getReferenceById(UUID.fromString(codeParts[1]));
                documentEntity.setCounterClaim(counterClaimEntity);
                documentEntity.setGeneralApplication(null);
                documentEntity.setCategoryId(CaseFileCategory.STATEMENTS_OF_CASE.getId());
                return documentNameService.appendCounterClaimPostfix(fileName, mainClaim, partyId);
            }
            if (NONE_PREFIX.equals(code)) {
                return applyDocumentTypeAssociation(documentEntity, mainClaim, partyId, documentType, fileName);
            }
        }

        return applyDocumentTypeAssociation(documentEntity, mainClaim, partyId, documentType, fileName);
    }

    private String applyDocumentTypeAssociation(
        DocumentEntity documentEntity,
        ClaimEntity mainClaim,
        UUID partyId,
        DocumentType documentType,
        String fileName
    ) {
        documentEntity.setGeneralApplication(null);
        documentEntity.setCounterClaim(null);
        documentEntity.setCategoryId(documentService.categoryIdForDocumentType(documentType));
        return documentNameService.appendPartyPostfix(fileName, mainClaim, partyId);
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
