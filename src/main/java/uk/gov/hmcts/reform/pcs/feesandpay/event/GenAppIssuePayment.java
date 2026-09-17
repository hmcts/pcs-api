package uk.gov.hmcts.reform.pcs.feesandpay.event;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.genapp.GenAppWaTaskService;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppDocumentGenerator;
import uk.gov.hmcts.reform.pcs.exception.GenAppNotFoundException;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.genAppIssuePayment;

@Component
@AllArgsConstructor
@Slf4j
public class GenAppIssuePayment implements CCDConfig<PCSCase, State, UserRole> {

    private final GenAppRepository genAppRepository;
    private final GenAppDocumentGenerator genAppDocumentGenerator;
    private final GenAppWaTaskService genAppWaTaskService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(genAppIssuePayment.name(), this::submit)
            .forStates(State.values())
            .name("Gen App Payment Confirmation")
            .showCondition(ShowConditions.NEVER_SHOW)
            .fields().mandatory(PCSCase::getPendingGenAppPaymentId).done()
            .grant(Permission.CRU, UserRole.SYSTEM_USER)
            .grant(Permission.R, UserRole.CLAIMANT)
            .grant(Permission.R, UserRole.PCS_SOLICITOR)
            .grant(Permission.R, UserRole.GA_CLAIMANT_SOLICITOR)
            .grant(Permission.R, UserRole.CITIZEN)
            .grant(Permission.R, UserRole.CTSC_ADMIN)
            .grant(Permission.R, UserRole.CTSC_TEAM_LEADER)
            .grant(Permission.R, UserRole.DEFENDANT)
            .grant(Permission.R, UserRole.PCS_CASE_WORKER)
            .grant(Permission.R, UserRole.DEFENDANT_SOLICITOR)
            .grant(Permission.R, UserRole.GA_DEFENDANT_SOLICITOR)
            .grant(Permission.R, UserRole.HEARING_CENTRE_ADMIN)
            .grant(Permission.R, UserRole.HEARING_CENTRE_TEAM_LEADER)
            .grant(Permission.R, UserRole.JUDGE)
            .grant(Permission.R, UserRole.LEADERSHIP_JUDGE)
            .grant(Permission.R, UserRole.WLU_ADMIN)
            .grant(Permission.R, UserRole.WLU_TEAM_LEADER)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        UUID genAppId = UUID.fromString(eventPayload.caseData().getPendingGenAppPaymentId());

        GenAppEntity genAppEntity = genAppRepository.findById(genAppId)
            .orElseThrow(() -> new GenAppNotFoundException("Unable to find gen app with ID " + genAppId));

        if (genAppEntity.getState() == GenAppState.PENDING_GEN_APP_ISSUED) {
            genAppEntity.setState(GenAppState.GEN_APP_ISSUED);
            genAppDocumentGenerator.createSubmissionDocument(caseReference, genAppEntity);
            genAppWaTaskService.createReviewGenAppTask(caseReference, genAppEntity);
            genAppWaTaskService.createTranslationTaskForGenApp(genAppEntity);
        } else {
            log.warn("Gen app {} state {} not valid for this event", genAppId, genAppEntity.getState());
        }

        return SubmitResponse.<State>builder().build();
    }

}
