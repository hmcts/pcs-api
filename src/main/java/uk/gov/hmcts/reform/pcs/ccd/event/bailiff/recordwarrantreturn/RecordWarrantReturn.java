package uk.gov.hmcts.reform.pcs.ccd.event.bailiff.recordwarrantreturn;

import lombok.AllArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.bailiff.WarrantReturn;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventStates;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.WarrantReturnService;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@AllArgsConstructor
@Component
public class RecordWarrantReturn implements CCDConfig<PCSCase, State, UserRole> {

    private WarrantReturnService warrantReturnService;
    private PcsCaseService pcsCaseService;
    private EnforcementOrderMediator enforcementOrderMediator;
    private static final String NO_WARRANT_ERROR_MESSAGE = "There is no warrant on this case";

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("ext:recordWarrantReturn", this::submit)
            .forStates(EventStates.recordWarrantOutcome())
            .name("Record warrant return")
            .showCondition(ShowConditions.NEVER_SHOW)
            .grant(
                Permission.CRU,
                UserRole.HEARING_CENTRE_ADMIN,
                UserRole.HEARING_CENTRE_TEAM_LEADER,
                UserRole.BAILIFF_ADMIN
            ) // TODO - REMOVE Hearing center roles
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        Optional<EnforcementOrderEntity> enforcementOrderEntity = enforcementOrderMediator
            .getEnforcementOrder(pcsCaseEntity);

        if (enforcementOrderEntity.isEmpty()) {
            return createErrorMessage();
        }

        WarrantEntity warrantEntity = enforcementOrderEntity.get().getWarrantDetails();
        if (warrantEntity == null) {
            return createErrorMessage();
        }

        PCSCase caseData = eventPayload.caseData();
        WarrantReturn warrantReturn = caseData.getWarrantReturn();

        warrantReturnService.addWarrantReturn(warrantEntity, warrantReturn);
        return SubmitResponse.<State>builder().build();
    }

    private SubmitResponse<State> createErrorMessage() {
        return SubmitResponse.<State>builder()
            .errors(List.of(NO_WARRANT_ERROR_MESSAGE))
            .build();
    }
}
