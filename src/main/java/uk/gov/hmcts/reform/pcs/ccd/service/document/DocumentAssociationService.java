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
            String code = getSelectedCode(relatedSubmission);
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

            if (!NONE_PREFIX.equals(code)) {
                log.warn("Unexpected related submission prefix: {}", codeParts.length == 0 ? code : codeParts[0]);
                return fileName;
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
}
