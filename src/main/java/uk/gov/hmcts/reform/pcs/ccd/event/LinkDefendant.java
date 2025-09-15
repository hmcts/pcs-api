package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.AccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.linkdefendant.LinkCode;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeService;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SYSTEM_UPDATE;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.linkDefendant;

@Component
@Slf4j
@AllArgsConstructor
public class LinkDefendant implements CCDConfig<PCSCase, State, UserRole> {

    private final AccessCodeService accessCodeService;
    private final CaseAssignmentApi caseAssignmentApi;
    private final IdamService idamService;
    private final AuthTokenGenerator authTokenGenerator;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(linkDefendant.name(), this::submit)
            .forStates(State.AWAITING_SUBMISSION_TO_HMCTS, State.CASE_ISSUED)
            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Get access to claim")
            .description("Link a defendant to a case")
            .grant(Permission.CRU, PCS_SYSTEM_UPDATE);

        new PageBuilder(eventBuilder)
            .add(new LinkCode());

    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Link code was {}", pcsCase.getLinkCode());

        AccessCodeEntity accessCodeEntity = accessCodeService.findAccessCode(caseReference, pcsCase.getLinkCode())
                .orElseThrow(() -> new RuntimeException("Access code not recogised"));

        // TODO: Assign role
        CaseAssignmentUserRoleWithOrganisation caseAssignmentUserRole = CaseAssignmentUserRoleWithOrganisation.builder()
            .caseDataId(Long.toString(caseReference))
            .userId(pcsCase.getLinkUserId())
            .caseRole(accessCodeEntity.getRole().getRole())
            .build();


        CaseAssignmentUserRolesRequest caseAssignmentRequest = CaseAssignmentUserRolesRequest.builder()
            .caseAssignmentUserRolesWithOrganisation(List.of(caseAssignmentUserRole))
            .build();

        caseAssignmentApi.addCaseUserRoles(idamService.getSystemUserAuthorisation(),
                                           authTokenGenerator.generate(),
                                           caseAssignmentRequest);


        log.info("Deleting used access code");
        accessCodeService.deleteAccessCode(accessCodeEntity);
    }

}
