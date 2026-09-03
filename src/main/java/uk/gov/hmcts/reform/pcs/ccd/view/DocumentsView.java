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
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentsView {

    private final UserRoleService userRoleService;
    private final GenAppVisibilityService genAppVisibilityService;
    private final UploadTimestampProvider uploadTimestampProvider;

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity, String organisationId) {
        pcsCase.setAllDocuments(mapAndWrapDocuments(pcsCaseEntity, organisationId));
    }

    private List<ListValue<Document>> mapAndWrapDocuments(PcsCaseEntity pcsCaseEntity, String organisationId) {

        if (pcsCaseEntity.getDocuments().isEmpty()) {
            return List.of();
        }

        UserRoles userRoles =
            userRoleService.getCurrentUserCaseRoles(pcsCaseEntity.getCaseReference());

        return pcsCaseEntity.getDocuments().stream()
            .filter(documentEntity -> this.isDocumentVisibleToUser(documentEntity, userRoles,
                                                                   organisationId))
            .filter(this::isNotInCaseDetailsTab)
            .map(entity -> ListValue.<Document>builder()
                .id(entity.getId().toString())
                .value(Document.builder()
                           .filename(entity.getFileName())
                           .url(entity.getUrl())
                           .binaryUrl(entity.getBinaryUrl())
                           .categoryId(entity.getCategoryId())
                           .uploadTimestamp(uploadTimestampProvider.uploadTimestamp(entity))
                           .build())
                .build())
            .collect(Collectors.toList());
    }

    private boolean isDocumentVisibleToUser(DocumentEntity documentEntity, UserRoles userRoles, String organisationId) {
        if (isExcludedFromCaseFile(documentEntity)) {
            return false;
        }

        GenAppEntity genAppEntity = documentEntity.getGeneralApplication();

        if (genAppEntity != null) {
            return genAppVisibilityService.isGenAppDocumentVisibleToUser(
                genAppEntity,
                userRoles.userId(),
                organisationId,
                userRoles.roles()
            );
        }

        if (documentEntity.getType() == DocumentType.WITHOUT_NOTICE_ORDER) {
            PartyEntity party = documentEntity.getParty();
            return genAppVisibilityService
                .isWithoutNoticeVisibleToUser(party, userRoles.userId(), organisationId, userRoles.roles());
        }

        CounterClaimEntity counterClaim = documentEntity.getCounterClaim();
        if (counterClaim != null) {
            return counterClaim.getStatus() == CounterClaimState.COUNTER_CLAIM_ISSUED;
        }

        return true;
    }

    private boolean isExcludedFromCaseFile(DocumentEntity documentEntity) {
        return documentEntity.getType() == DocumentType.DEFENDANT_ACCESS_CODE
            || documentEntity.isRemoved();
    }

    public static boolean isDescriptionEmpty(DocumentEntity documentEntity) {
        return ObjectUtils.isEmpty(documentEntity.getDescription())
                || documentEntity.getDescription().trim().isEmpty();
    }

    public static boolean isNotRemoved(DocumentEntity documentEntity) {
        return !documentEntity.isRemoved();
    }

    public static boolean isNotGenAppDocument(DocumentEntity documentEntity) {
        return documentEntity.getGeneralApplication() == null;
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
