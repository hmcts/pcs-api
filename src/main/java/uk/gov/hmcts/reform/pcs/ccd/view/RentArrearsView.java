package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;

import java.util.List;
import java.util.Optional;

@Component
public class RentArrearsView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getRentArrears)
            .ifPresent(rentArrearsEntity ->
                           setRentArrearsFields(pcsCase, rentArrearsEntity, pcsCaseEntity));
    }

    private void setRentArrearsFields(PCSCase pcsCase, RentArrearsEntity rentArrearsEntity,
                                      PcsCaseEntity pcsCaseEntity) {
        RentArrearsSection rentArrears = new RentArrearsSection();
        List<ListValue<Document>> documents = getRentStatement(pcsCaseEntity);

        rentArrears.setTotal(rentArrearsEntity.getTotalRentArrears());
        rentArrears.setRecoveryAttempted(rentArrearsEntity.getRecoveryAttempted());
        rentArrears.setRecoveryAttemptDetails(rentArrearsEntity.getRecoveryAttemptDetails());
        rentArrears.setStatementDocuments(documents);

        pcsCase.setRentArrears(rentArrears);

        pcsCase.setArrearsJudgmentWanted(rentArrearsEntity.getArrearsJudgmentWanted());
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

    private static List<ListValue<Document>> getRentStatement(PcsCaseEntity pcsCaseEntity) {
        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return null;
        }

        return pcsCaseEntity.getDocuments().stream()
            .filter(RentArrearsView::isRentStatement)
            .map(RentArrearsView::toDocument)
            .toList();
    }

    private static boolean isRentStatement(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.RENT_STATEMENT;
    }

    private static ListValue<Document> toDocument(DocumentEntity documentEntity) {
        return ListValue.<Document>builder()
            .id(documentEntity.getId().toString())
            .value(
                Document.builder()
                    .url(documentEntity.getUrl())
                    .filename(documentEntity.getFileName())
                    .binaryUrl(documentEntity.getBinaryUrl())
                    .categoryId(documentEntity.getCategoryId())
                    .build()
            ).build();
    }

}
