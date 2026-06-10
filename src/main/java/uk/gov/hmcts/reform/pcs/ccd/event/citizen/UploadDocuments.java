package uk.gov.hmcts.reform.pcs.ccd.event.citizen;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.DocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.RelatedApplicationOption;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.uploadDocuments;

@Slf4j
@Component
@AllArgsConstructor
public class UploadDocuments implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final SecurityContextService securityContextService;
    private final DocumentService documentService;
    private final GenAppVisibilityService genAppVisibilityService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(uploadDocuments.name(), this::submit, this::start)
            .forStates(State.CASE_ISSUED)
            .name("Upload additional documents")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(Permission.CRU, UserRole.DEFENDANT);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        if (caseData.getDocumentUploadDetails() == null) {
            caseData.setDocumentUploadDetails(new DocumentUploadDetails());
        }

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        UUID currentUserId = securityContextService.getCurrentUserId();

        List<ListValue<RelatedApplicationOption>> options =
            visibleGenAppsForUser(pcsCaseEntity, currentUserId).stream()
                .map(this::toOption)
                .filter(Objects::nonNull)
                .toList();

        caseData.getDocumentUploadDetails().setRelatedApplicationOptions(new ArrayList<>(options));

        caseData.getDocumentUploadDetails().setShowRelatedApplicationsPage(
            options.isEmpty() ? YesOrNo.NO : YesOrNo.YES);

        return caseData;
    }

    private List<GenAppEntity> visibleGenAppsForUser(PcsCaseEntity pcsCaseEntity, UUID currentUserId) {
        if (pcsCaseEntity == null) {
            return List.of();
        }
        return genAppVisibilityService.getVisibleGenAppsToUser(pcsCaseEntity.getGenApps(), currentUserId);
    }

    private ListValue<RelatedApplicationOption> toOption(GenAppEntity genApp) {
        DocumentUploadCategory category = mapGenAppTypeToCategory(genApp.getType());
        if (category == null) {
            return null;
        }
        RelatedApplicationOption option = RelatedApplicationOption.builder()
            .genAppId(genApp.getId().toString())
            .category(category)
            .submittedDate(genApp.getApplicationSubmittedDate())
            .build();
        return ListValue.<RelatedApplicationOption>builder()
            .id(genApp.getId().toString())
            .value(option)
            .build();
    }

    private DocumentUploadCategory mapGenAppTypeToCategory(GenAppType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case ADJOURN -> DocumentUploadCategory.ADJOURN_HEARING_APPLICATION;
            case SET_ASIDE -> DocumentUploadCategory.SET_ASIDE_ORDER_APPLICATION;
            case SOMETHING_ELSE -> DocumentUploadCategory.GENERAL_APPLICATION;
        };
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        UUID currentUserId = securityContextService.getCurrentUserId();
        PartyEntity uploadingParty = partyService.getPartyEntityByIdamId(currentUserId, caseReference);
        GenAppEntity selectedGenApp = resolveSelectedGenApp(caseData, pcsCaseEntity, currentUserId);

        documentService.linkAdditionalDocumentsToCase(
            caseData.getUploadedAdditionalDocuments(),
            pcsCaseEntity,
            uploadingParty,
            selectedGenApp
        );

        return SubmitResponse.<State>builder().build();
    }

    private GenAppEntity resolveSelectedGenApp(PCSCase caseData, PcsCaseEntity pcsCaseEntity, UUID currentUserId) {
        DocumentUploadDetails details = caseData.getDocumentUploadDetails();
        if (details == null || details.getSelectedRelatedApplicationId() == null) {
            return null;
        }
        UUID selectedId;
        try {
            selectedId = UUID.fromString(details.getSelectedRelatedApplicationId());
        } catch (IllegalArgumentException e) {
            return null;
        }
        return visibleGenAppsForUser(pcsCaseEntity, currentUserId).stream()
            .filter(genApp -> selectedId.equals(genApp.getId()))
            .findFirst()
            .orElse(null);
    }
}
