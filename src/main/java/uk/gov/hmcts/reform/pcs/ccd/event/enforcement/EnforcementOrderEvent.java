package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AggressiveAnimalsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AdditionalInformationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.MoneyOwedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.PropertyAccessDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.CheckYourAnswersPlaceHolder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EnforcementApplicationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.NameAndAddressForEvictionPage;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EvictionDelayWarningPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EvictionRisksPosedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.PoliceOrSocialServicesRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.ProtestorGroupRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.VerbalOrWrittenThreatsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.ViolentAggressiveRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.FirearmsPossessionRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.CriminalAntisocialRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.VulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.LivingInThePropertyPage;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Slf4j
@Component
@AllArgsConstructor
public class EnforcementOrderEvent implements CCDConfig<PCSCase, State, UserRole> {
    // Business requirements to be agreed on for the conditions when this event can be triggered

    private final AddressFormatter addressFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(enforceTheOrder.name(), this::submit, this::start)
                .forState(AWAITING_SUBMISSION_TO_HMCTS)
                .name("Enforce the order")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);
        configurePages(eventBuilder);
    }

    private void configurePages(Event.EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        PageBuilder pageBuilder = new PageBuilder(eventBuilder);
        pageBuilder
                .add(new EnforcementApplicationPage())
                .add(new NameAndAddressForEvictionPage())
                .add(new LivingInThePropertyPage())
                .add(new EvictionDelayWarningPage())
                .add(new EvictionRisksPosedPage())
                .add(new ViolentAggressiveRiskPage())
                .add(new FirearmsPossessionRiskPage())
                .add(new CriminalAntisocialRiskPage())
                .add(new VerbalOrWrittenThreatsRiskPage())
                .add(new ProtestorGroupRiskPage())
                .add(new PoliceOrSocialServicesRiskPage())
                .add(new AggressiveAnimalsRiskPage())
                .add(new VulnerableAdultsChildrenPage())
                .add(new PropertyAccessDetailsPage())
                .add(new AdditionalInformationPage())
                .add(new MoneyOwedPage())
                .add(new CheckYourAnswersPlaceHolder());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setFormattedPropertyAddress(addressFormatter.getFormattedAddress(caseData));
        if (caseData.getDefendants() != null && !caseData.getDefendants().isEmpty()) {
            caseData.setDefendant1(caseData.getDefendants().getFirst().getValue());
        }
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.defaultResponse();
    }
}
