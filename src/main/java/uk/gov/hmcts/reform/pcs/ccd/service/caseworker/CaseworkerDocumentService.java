package uk.gov.hmcts.reform.pcs.ccd.service.caseworker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseworkerDocumentService {

    public static final String GEN_APP_ID_PREFIX = "GEN_APP";
    public static final String COUNTERCLAIM_ID_PREFIX = "COUNTERCLAIM";
    public static final String NONE_PREFIX = "NONE";

    private final PcsCaseService pcsCaseService;
    private final DocumentService documentService;
    private final GenAppService genAppService;
    private final DocumentIdExtractor documentIdExtractor;
    private final DocumentRepository documentRepository;
    private final DocumentNameService documentNameService;
    private final CounterClaimRepository counterClaimRepository;
    private final PartyRepository partyRepository;

    public DocumentEntity saveNewDocument(CaseworkerDocument caseworkerDocument, long caseReference) {
        Document document = caseworkerDocument.getDocument();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity mainClaim = pcsCaseEntity.getMainClaim();

        CaseworkerDocumentType caseworkerDocumentType = getCaseworkerDocumentType(caseworkerDocument);

        UUID partyId = caseworkerDocument.getRelatedParty().getValue().getCode();

        DocumentEntity documentEntity = createDocumentEntity(document, caseworkerDocumentType);
        documentEntity.setIssueDate(caseworkerDocument.getIssueDate());

        CaseFileCategory caseFileCategory = null;
        String documentFilename = document.getFilename();
        if (caseworkerDocument.getIssueDate() != null) {
            documentFilename = documentNameService.appendDate(documentFilename, caseworkerDocument.getIssueDate());
        }

        if (caseworkerDocument.getShowRelatedSubmissionsList() == VerticalYesNo.YES) {
            String code = getSelectedCode(caseworkerDocument.getRelatedSubmission());
            String[] codeParts = code.split(":");
            switch (codeParts[0]) {
                case GEN_APP_ID_PREFIX: {
                    UUID genAppId = UUID.fromString(codeParts[1]);
                    GenAppEntity genAppEntity = genAppService.loadGenApp(genAppId);
                    documentEntity.setGeneralApplication(genAppEntity);
                    documentFilename = documentNameService
                        .appendGenAppPostfix(documentFilename, genAppEntity, mainClaim, partyId);
                    caseFileCategory = CaseFileCategory.APPLICATIONS;
                    break;
                }
                case COUNTERCLAIM_ID_PREFIX: {
                    UUID counterClaimId = UUID.fromString(codeParts[1]);
                    CounterClaimEntity counterClaimEntity = counterClaimRepository.getReferenceById(counterClaimId);
                    documentEntity.setCounterClaim(counterClaimEntity);
                    documentFilename = documentNameService
                        .appendCounterClaimPostfix(documentFilename, mainClaim, partyId);
                    caseFileCategory = CaseFileCategory.STATEMENTS_OF_CASE;
                    break;
                }
                case NONE_PREFIX: {
                    documentFilename = documentNameService.appendPartyPostfix(documentFilename, mainClaim, partyId);
                    caseFileCategory = documentService.mapDocumentTypeToCategory(documentEntity.getType()).orElse(null);
                    break;
                }
                default: {
                    log.warn("Unexpected related submission prefix: " + codeParts[0]);
                }
            }
        } else {
            // No Gen App / Counterclaims in this case that could be linked to
            documentFilename = documentNameService.appendPartyPostfix(documentFilename, mainClaim, partyId);
            caseFileCategory = documentService.mapDocumentTypeToCategory(documentEntity.getType()).orElse(null);
        }

        documentEntity.setFileName(documentFilename);
        documentEntity.setCategoryId(caseFileCategory != null ? caseFileCategory.getId() : null);

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
        return Optional.ofNullable(relatedSubmission.getValue())
            .map(DynamicStringListElement::getCode)
            .orElse(null);
    }

    public DocumentType mapToDocumentType(CaseworkerDocumentType caseworkerDocumentType) {
        if (caseworkerDocumentType == null) {
            return null;
        }

        return switch (caseworkerDocumentType) {
            case WITNESS_STATEMENT -> DocumentType.WITNESS_STATEMENT;
            case RENT_STATEMENT -> DocumentType.RENT_STATEMENT;
            case TENANCY_AGREEMENT -> DocumentType.TENANCY_AGREEMENT;
            case OCCUPATION_LICENCE -> DocumentType.OCCUPATION_LICENCE;
            case CERTIFICATE_OF_SERVICE -> DocumentType.CERTIFICATE_OF_SERVICE;
            case ENERGY_PERFORMANCE_CERTIFICATE -> DocumentType.ENERGY_PERFORMANCE_CERTIFICATE;
            case GAS_SAFETY_CERTIFICATE -> DocumentType.GAS_SAFETY_CERTIFICATE;
            case EICR_REPORT -> DocumentType.EICR_REPORT;
            case CORRESPONDENCE_BETWEEN_PARTIES -> DocumentType.CORRESPONDENCE_BETWEEN_PARTIES;
            case CORRESPONDENCE_FROM_CLAIMANT -> DocumentType.CORRESPONDENCE_FROM_CLAIMANT;
            case CORRESPONDENCE_FROM_DEFENDANT -> DocumentType.CORRESPONDENCE_FROM_DEFENDANT;
            case POSSESSION_NOTICE -> DocumentType.POSSESSION_NOTICE;
            case NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION -> DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION;
            case PHOTOGRAPHIC_EVIDENCE -> DocumentType.PHOTOGRAPHIC_EVIDENCE;
            case INSPECTION_OR_REPORT -> DocumentType.INSPECTION_OR_REPORT;
            case AMENDED_CLAIM_FORM -> DocumentType.AMENDED_CLAIM_FORM;
            case PART_20_COUNTERCLAIM -> DocumentType.PART_20_COUNTERCLAIM;
            case CERTIFICATE_OF_SUITABILITY_AS_LF -> DocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF;
            case LEGAL_AID_CERTIFICATE -> DocumentType.LEGAL_AID_CERTIFICATE;
            case NOTICE_OF_HEARING -> DocumentType.NOTICE_OF_HEARING;
            case WITH_NOTICE_ORDER -> DocumentType.WITH_NOTICE_ORDER;
            case WITHOUT_NOTICE_ORDER -> DocumentType.WITHOUT_NOTICE_ORDER;
            case NOTICE_OF_ALLOCATION_TO_TRACK -> DocumentType.NOTICE_OF_ALLOCATION_TO_TRACK;
            case OTHER -> DocumentType.OTHER;
        };
    }

}
