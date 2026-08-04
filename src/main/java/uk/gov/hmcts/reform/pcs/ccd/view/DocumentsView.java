package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CurrentUserCaseRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.CurrentUserCaseRoleService.CurrentUserCaseRoles;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentsView {

    private final CurrentUserCaseRoleService currentUserCaseRoleService;
    private final GenAppVisibilityService genAppVisibilityService;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        pcsCase.setAllDocuments(mapAndWrapDocuments(pcsCaseEntity));
    }

    private List<ListValue<Document>> mapAndWrapDocuments(PcsCaseEntity pcsCaseEntity) {

        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return List.of();
        }

        CurrentUserCaseRoles currentUserCaseRoles =
            currentUserCaseRoleService.getCurrentUserCaseRoles(pcsCaseEntity.getCaseReference());

        return pcsCaseEntity.getDocuments().stream()
            .filter(documentEntity -> this.isDocumentVisibleToUser(
                documentEntity,
                currentUserCaseRoles.userId(),
                currentUserCaseRoles.roles()
            ))
            .filter(this::isNotInCaseDetailsTab)
            .map(entity -> ListValue.<Document>builder()
                .id(entity.getId().toString())
                .value(Document.builder()
                           .filename(entity.getFileName())
                           .url(entity.getUrl())
                           .binaryUrl(entity.getBinaryUrl())
                           .categoryId(entity.getCategoryId())
                           .uploadTimestamp(entity.getSubmittedDate() == null
                                                ? null
                                                : entity.getSubmittedDate()
                               .atZone(java.time.ZoneOffset.UTC).toLocalDateTime())
                           .build())
                .build())
            .collect(Collectors.toList());
    }

    private boolean isDocumentVisibleToUser(DocumentEntity documentEntity,
                                            UUID currentUserId,
                                            Collection<String> currentUserRoles) {
        if (isExcludedFromCaseFile(documentEntity)) {
            return false;
        }

        GenAppEntity genAppEntity = documentEntity.getGeneralApplication();

        if (genAppEntity != null) {
            return genAppVisibilityService.isGenAppDocumentVisibleToUser(genAppEntity, currentUserId, currentUserRoles);
        }

        if (documentEntity.getType() == DocumentType.WITHOUT_NOTICE_ORDER) {
            PartyEntity party = documentEntity.getParty();
            return genAppVisibilityService.isWithoutNoticeVisibleToUser(party, currentUserId, currentUserRoles);
        }

        CounterClaimEntity counterClaim = documentEntity.getCounterClaim();
        if (counterClaim != null) {
            return counterClaim.getStatus() == CounterClaimState.COUNTER_CLAIM_ISSUED;
        }

        return true;
    }

    private boolean isExcludedFromCaseFile(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.DEFENDANT_ACCESS_CODE;
    }

    public static boolean isDescriptionEmpty(DocumentEntity documentEntity) {
        return ObjectUtils.isEmpty(documentEntity.getDescription())
                || documentEntity.getDescription().trim().isEmpty();
    }

    private boolean isNotInCaseDetailsTab(DocumentEntity documentEntity) {
        List<DocumentType> caseDetailsDocuments = List.of(
            DocumentType.TENANCY_AGREEMENT,
            DocumentType.POSSESSION_NOTICE,
            DocumentType.RENT_STATEMENT,
            DocumentType.ENERGY_PERFORMANCE_CERTIFICATE,
            DocumentType.EICR_REPORT,
            DocumentType.GAS_SAFETY_CERTIFICATE,
            DocumentType.OCCUPATION_LICENCE
        );

        DocumentType type = documentEntity.getType();
        if (type == null || !caseDetailsDocuments.contains(type)) {
            return true;
        }

        // Is not an additional document
        return !isDescriptionEmpty(documentEntity);
    }

}
