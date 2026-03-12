package uk.gov.hmcts.reform.pcs.ccd.event.confirmevictiondetails;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.confirmevictiondetails.ConfirmEvictionDetailsPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.confirmEvictionDetails;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;

@Slf4j
@Component
@AllArgsConstructor
public class ConfirmEvictionDetails implements CCDConfig<PCSCase, State, UserRole> {

    private final AddressFormatter addressFormatter;
    private final SavingPageBuilderFactory savingPageBuilderFactory;
    private final ConfirmEvictionDetailsPageConfigurer confirmEvictionDetailsPageConfigurer;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(confirmEvictionDetails.name(), this::submit, this::start)
                .forAllStates()
                .name("Confirm the eviction details")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();
        SavingPageBuilder pageBuilder = savingPageBuilderFactory.create(eventBuilder, confirmEvictionDetails);
        confirmEvictionDetailsPageConfigurer.configurePages(pageBuilder);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
//        pcsCase.setFormattedPropertyAddress(addressFormatter
//                .formatMediumAddress(pcsCase.getPropertyAddress(), BR_DELIMITER));

        return pcsCase;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
    return SubmitResponse.defaultResponse();
    }


}
