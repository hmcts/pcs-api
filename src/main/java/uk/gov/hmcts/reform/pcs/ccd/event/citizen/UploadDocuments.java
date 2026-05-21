package uk.gov.hmcts.reform.pcs.ccd.event.citizen;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

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

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(uploadDocuments.name(), this::submit)
            .forStates(State.CASE_ISSUED)
            .name("Upload additional documents")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(Permission.CR, UserRole.DEFENDANT);
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
