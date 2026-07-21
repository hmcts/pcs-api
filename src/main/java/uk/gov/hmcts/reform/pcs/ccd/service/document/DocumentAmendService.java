package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentListService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.util.List;
import java.util.Objects;
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
    private final CaseworkerDocumentListService caseworkerDocumentListService;

    private static final String PARTY_POSTFIX_PATTERN = " - (Claimant|Defendant) \\d+$";
    private static final String GEN_APP_POSTFIX_PATTERN = " GA\\d+$";
    private static final String ISSUE_DATE_POSTFIX_PATTERN = " \\d{8}$";

    public void initialiseAmendDetails(long caseReference, PCSCase caseData) {
        DocumentAmendDetails details = caseData.getDocumentAmendDetails();
        if (details == null) {
            return;
        }

        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        details.setRelatedParty(caseworkerDocumentListService.buildRelatedPartyList(
            pcsCase,
            details.getRelatedParty()
        ));
        setApplicationOrCounterclaimLists(caseData, details, pcsCase);
        populateSelectedDocumentDetails(details, findSelectedDocument(pcsCase, details.getSelectedDocumentId()));
    }

    public AmendedDocument amendDocument(PCSCase caseData, long caseReference) {
        DocumentAmendDetails amendDetails = caseData.getDocumentAmendDetails();
        final PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        DocumentEntity documentEntity = loadDocument(pcsCaseEntity, amendDetails);
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

    private void populateSelectedDocumentDetails(DocumentAmendDetails details,
                                                 Optional<DocumentEntity> selectedDocumentEntity) {
        if (details.getSelectedDocumentId() == null) {
            clearSelectedDocumentDetails(details);
            return;
        }

        String selectedDocumentFileName = selectedDocumentEntity
            .map(DocumentEntity::getFileName)
            .orElse(details.getSelectedDocumentFileName());
        String selectedDocumentBaseFileName = editableBaseFileName(selectedDocumentFileName);

        details.setSelectedDocumentFileName(selectedDocumentFileName);
        details.setSelectedDocumentBaseFileName(selectedDocumentBaseFileName);
        details.setAmendedFileName(selectedDocumentBaseFileName);
        details.setSelectedDocumentIssueDate(selectedDocumentEntity.map(DocumentEntity::getIssueDate).orElse(null));
        details.setIssueDate(selectedDocumentEntity.map(DocumentEntity::getIssueDate).orElse(null));
        selectedDocumentEntity.ifPresent(document -> preselectRelatedParty(details, document));
    }

    private void clearSelectedDocumentDetails(DocumentAmendDetails details) {
        details.setSelectedDocumentBaseFileName(null);
        details.setAmendedFileName(null);
        details.setSelectedDocumentIssueDate(null);
        details.setIssueDate(null);
        if (details.getRelatedParty() != null) {
            details.getRelatedParty().setValue(null);
        }
    }

    private Optional<DocumentEntity> findSelectedDocument(PcsCaseEntity pcsCaseEntity, String selectedDocumentId) {
        if (selectedDocumentId == null || CollectionUtils.isEmpty(pcsCaseEntity.getDocuments())) {
            return Optional.empty();
        }

        UUID selectedDocumentUuid = UUID.fromString(selectedDocumentId);
        return pcsCaseEntity.getDocuments().stream()
            .filter(Objects::nonNull)
            .filter(document -> selectedDocumentUuid.equals(document.getId()))
            .findFirst();
    }

    private DocumentEntity loadDocument(PcsCaseEntity pcsCaseEntity, DocumentAmendDetails amendDetails) {
        UUID selectedDocumentId = UUID.fromString(amendDetails.getSelectedDocumentId());
        return pcsCaseEntity.getDocuments().stream()
            .filter(Objects::nonNull)
            .filter(document -> selectedDocumentId.equals(document.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No document found for ID: " + amendDetails.getSelectedDocumentId()
            ));
    }

    private void preselectRelatedParty(DocumentAmendDetails details, DocumentEntity selectedDocumentEntity) {
        if (selectedDocumentEntity.getParty() == null
            || selectedDocumentEntity.getParty().getId() == null
            || details.getRelatedParty() == null
            || CollectionUtils.isEmpty(details.getRelatedParty().getListItems())) {
            return;
        }

        String selectedPartyId = selectedDocumentEntity.getParty().getId().toString();
        details.getRelatedParty().getListItems().stream()
            .filter(option -> option.getCode() != null && selectedPartyId.equals(option.getCode().toString()))
            .findFirst()
            .ifPresent(details.getRelatedParty()::setValue);
    }

    private void setApplicationOrCounterclaimLists(PCSCase caseData, DocumentAmendDetails details,
                                                   PcsCaseEntity pcsCase) {
        List<ListValue<GeneralApplication>> genApps = caseData.getGenApps();
        List<CounterClaimEntity> counterClaims = pcsCase.getCounterClaims();
        boolean showRelatedSubmissionsList = caseworkerDocumentListService
            .hasRelatedSubmissions(genApps, counterClaims);

        details.setShowRelatedSubmissionsList(VerticalYesNo.from(showRelatedSubmissionsList));
        details.setRelatedSubmission(showRelatedSubmissionsList
            ? caseworkerDocumentListService.buildRelatedSubmissionsList(
                pcsCase,
                genApps,
                counterClaims,
                details.getRelatedSubmission())
            : null);

        details.setRelatedSubmissionsDocumentType(caseworkerDocumentListService.buildDocumentTypeList(
            caseData.getLegislativeCountry(),
            details.getRelatedSubmissionsDocumentType()));
        details.setStandaloneDocumentType(caseworkerDocumentListService.buildDocumentTypeList(
            caseData.getLegislativeCountry(),
            details.getStandaloneDocumentType()));
    }

    private String editableBaseFileName(String fileName) {
        if (fileName == null) {
            return null;
        }

        String baseFileName = FilenameUtils.getBaseName(fileName);
        return baseFileName
            .replaceFirst(PARTY_POSTFIX_PATTERN, "")
            .replaceFirst(GEN_APP_POSTFIX_PATTERN, "")
            .replaceFirst(ISSUE_DATE_POSTFIX_PATTERN, "");
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
