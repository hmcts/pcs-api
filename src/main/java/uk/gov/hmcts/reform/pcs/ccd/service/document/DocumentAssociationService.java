package uk.gov.hmcts.reform.pcs.ccd.service.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.COUNTERCLAIM_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.GEN_APP_ID_PREFIX;
import static uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService.NONE_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAssociationService {

    private final DocumentService documentService;
    private final DocumentNameService documentNameService;
    private final GenAppService genAppService;
    private final CounterClaimRepository counterClaimRepository;

    public String applyAssociation(
        DocumentEntity documentEntity,
        ClaimEntity mainClaim,
        UUID partyId,
        DocumentType documentType,
        String fileName,
        VerticalYesNo showRelatedSubmissionsList,
        DynamicStringList relatedSubmission
    ) {
        if (showRelatedSubmissionsList == VerticalYesNo.YES) {
            RelatedSubmissionId relatedSubmissionId = parseRelatedSubmissionId(getSelectedCode(relatedSubmission));

            if (GEN_APP_ID_PREFIX.equals(relatedSubmissionId.prefix())) {
                GenAppEntity genAppEntity = genAppService.loadGenApp(relatedSubmissionId.id());
                documentEntity.setGeneralApplication(genAppEntity);
                documentEntity.setCounterClaim(null);
                documentEntity.setCategoryId(CaseFileCategory.APPLICATIONS.getId());
                return documentNameService.appendGenAppPostfix(fileName, genAppEntity, mainClaim, partyId);
            }

            if (COUNTERCLAIM_ID_PREFIX.equals(relatedSubmissionId.prefix())) {
                CounterClaimEntity counterClaimEntity = counterClaimRepository
                    .getReferenceById(relatedSubmissionId.id());
                documentEntity.setCounterClaim(counterClaimEntity);
                documentEntity.setGeneralApplication(null);
                documentEntity.setCategoryId(CaseFileCategory.STATEMENTS_OF_CASE.getId());
                return documentNameService.appendCounterClaimPostfix(fileName, mainClaim, partyId);
            }

            if (!NONE_PREFIX.equals(relatedSubmissionId.prefix())) {
                log.warn("Unexpected related submission prefix: {}", relatedSubmissionId.prefix());
                throw new IllegalArgumentException("Unexpected related submission: " + relatedSubmissionId.prefix());
            }
        }

        documentEntity.setGeneralApplication(null);
        documentEntity.setCounterClaim(null);
        documentEntity.setCategoryId(documentService.categoryIdForDocumentType(documentType));
        return documentNameService.appendPartyPostfix(fileName, mainClaim, partyId);
    }

    private static String getSelectedCode(DynamicStringList dynamicStringList) {
        return dynamicStringList == null ? null : dynamicStringList.getValueCode();
    }

    private static RelatedSubmissionId parseRelatedSubmissionId(String code) {
        if (NONE_PREFIX.equals(code)) {
            return new RelatedSubmissionId(NONE_PREFIX, null);
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Related submission must be selected");
        }

        String[] codeParts = code.split(":");
        if (codeParts.length != 2 || codeParts[1].isBlank()) {
            throw new IllegalArgumentException("Invalid related submission: " + code);
        }

        return new RelatedSubmissionId(codeParts[0], UUID.fromString(codeParts[1]));
    }

    private record RelatedSubmissionId(String prefix, UUID id) {
    }
}
