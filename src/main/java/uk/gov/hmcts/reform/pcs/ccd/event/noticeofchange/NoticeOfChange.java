package uk.gov.hmcts.reform.pcs.ccd.event.noticeofchange;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PostcodeNotAssignedToCourt;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.WarrantPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution.WarrantOfRestitutionPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ.WritPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.noticeofchange.NoticeOfChangePage;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT_OF_RESTITUTION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WRIT;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.noticeOfChange;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;
import static uk.gov.hmcts.reform.pcs.ccd.util.EnforcementTypeUtil.createDynamicStringList;

@Slf4j
@Component
@AllArgsConstructor
public class NoticeOfChange implements CCDConfig<PCSCase, State, UserRole> {

    private final AddressFormatter addressFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(noticeOfChange.name(), this::submit, this::start)
                .forAllStates()
                .name("Notice of change")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(new NoticeOfChangePage());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setFormattedPropertyAddress(addressFormatter
                .formatMediumAddress(pcsCase.getPropertyAddress(), BR_DELIMITER));

        return pcsCase;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.defaultResponse() ;
    }

}
