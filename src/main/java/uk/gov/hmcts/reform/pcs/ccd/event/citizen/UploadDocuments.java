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
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.uploadDocuments;

@Slf4j
@Component
@AllArgsConstructor
public class UploadDocuments implements CCDConfig<PCSCase, State, UserRole> {

    private static final Set<GenAppState> VISIBLE_GEN_APP_STATES =
        EnumSet.of(GenAppState.PENDING_SUBMISSION, GenAppState.SUBMITTED);

    private final PcsCaseService pcsCaseService;
    private final PartyService partyService;
    private final SecurityContextService securityContextService;
    private final DocumentService documentService;

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

        List<ListValue<RelatedApplicationOption>> options = Arrays.stream(DocumentUploadCategory.values())
            .map(category -> buildOption(category, pcsCaseEntity))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(
                (ListValue<RelatedApplicationOption> listValue) -> listValue.getValue().getSubmittedDate())
                .reversed())
            .toList();

        caseData.getDocumentUploadDetails().setRelatedApplicationOptions(new ArrayList<>(options));

        caseData.getDocumentUploadDetails().setShowRelatedApplicationsPage(
            options.isEmpty() ? YesOrNo.NO : YesOrNo.YES);

        return caseData;
    }

    private ListValue<RelatedApplicationOption> buildOption(DocumentUploadCategory category,
                                                            PcsCaseEntity pcsCaseEntity) {
        LocalDateTime submittedDate = findLatestGenAppDateForCategory(pcsCaseEntity, category);
        if (submittedDate == null) {
            return null;
        }
        return wrap(RelatedApplicationOption.builder()
            .category(category)
            .submittedDate(submittedDate)
            .build());
    }

    private ListValue<RelatedApplicationOption> wrap(RelatedApplicationOption option) {
        return ListValue.<RelatedApplicationOption>builder()
            .id(option.getCategory().name())
            .value(option)
            .build();
    }

    private LocalDateTime findLatestGenAppDateForCategory(PcsCaseEntity pcsCaseEntity,
                                                          DocumentUploadCategory category) {
        if (pcsCaseEntity == null || pcsCaseEntity.getGenApps() == null) {
            return null;
        }

        GenAppType mapped = mapCategoryToGenAppType(category);
        if (mapped == null) {
            return null;
        }

        return pcsCaseEntity.getGenApps().stream()
            .filter(genApp -> genApp.getType() == mapped)
            .filter(genApp -> VISIBLE_GEN_APP_STATES.contains(genApp.getState()))
            .map(GenAppEntity::getApplicationSubmittedDate)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    private GenAppType mapCategoryToGenAppType(DocumentUploadCategory category) {
        return switch (category) {
            case ADJOURN_HEARING_APPLICATION -> GenAppType.ADJOURN;
            // SUSPEND was removed from GenAppType 
            case SUSPEND_EVICTION_APPLICATION -> null;
            case SET_ASIDE_ORDER_APPLICATION -> GenAppType.SET_ASIDE;
            case GENERAL_APPLICATION -> GenAppType.SOMETHING_ELSE;
        };
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        PartyEntity uploadingParty = getCurrentPartyEntity(caseReference);

        documentService.createAdditionalDocumentsForParty(
            caseData.getUploadedAdditionalDocuments(),
            pcsCaseEntity,
            uploadingParty
        );

        return SubmitResponse.<State>builder().build();
    }

    private PartyEntity getCurrentPartyEntity(long caseReference) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        return partyService.getPartyEntityByIdamId(currentUserId, caseReference);
    }
}
