package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.WarrantPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution.WarrantOfRestitutionPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ.WritPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumeEnforcementOrder;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Slf4j
@Component
@AllArgsConstructor
public class ResumeDraftEnforcementOrder implements CCDConfig<PCSCase, State, UserRole> {

    private final EnforcementPageConfigurer enforcementPageConfigurer;
    private final WarrantPageConfigurer warrantPagesConfigurer;
    private final WritPageConfigurer writPageConfigurer;
    private final WarrantOfRestitutionPageConfigurer warrantOfRestitutionPageConfigurer;
    private final SavingPageBuilderFactory savingPageBuilderFactory;
    private final EnforcementSubmitEvent enforcementSubmitEvent;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
                configBuilder
                        .decentralisedEvent(resumeEnforcementOrder.name(), this::submit)
                        .forState(State.CASE_ISSUED)
                        .name("Resume Draft Application")
                        .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                        .grant(Permission.CRUD, UserRole.GA_CLAIMANT_SOLICITOR)
                        .grant(Permission.CRUD, UserRole.DEFENDANT_SOLICITOR)
                        .grant(Permission.CRUD, UserRole.GA_DEFENDANT_SOLICITOR)
                        .showCondition(ShowConditions.and(
                                ShowConditions.featureFlagsEnabled(ENFORCEMENT),
                                "hasUnsubmittedEnforcementData=\"Yes\""))
                        .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                        .showSummary();
        SavingPageBuilder pageBuilder = savingPageBuilderFactory.create(eventBuilder, enforceTheOrder);
        enforcementPageConfigurer.configurePages(pageBuilder);
        warrantPagesConfigurer.configurePages(pageBuilder);
        writPageConfigurer.configurePages(pageBuilder);
        warrantOfRestitutionPageConfigurer.configurePages(pageBuilder);
    }

    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return enforcementSubmitEvent.submit(eventPayload);
    }

}
