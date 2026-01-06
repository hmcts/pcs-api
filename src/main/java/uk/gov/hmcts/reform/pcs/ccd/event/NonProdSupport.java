package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.nonprod.NonProdSupportPage;
import uk.gov.hmcts.reform.pcs.ccd.service.nonprod.CaseSupportHelper;
import uk.gov.hmcts.reform.pcs.ccd.service.nonprod.NonProdSupportService;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;

@Component
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(name = "enable.testing.support", havingValue = "true")
public class NonProdSupport implements CCDConfig<PCSCase, State, UserRole> {

    static final String EVENT_NAME = "Test Support Case Creation";

    private final NonProdSupportService nonProdSupportService;
    private final CaseSupportHelper caseSupportHelper;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createTestCase.name(), this::submit, this::start)
                .initialState(AWAITING_SUBMISSION_TO_HMCTS)
                .showSummary()
                .name(EVENT_NAME)
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);

        new PageBuilder(eventBuilder)
            .add(new NonProdSupportPage());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setFeeAmount("123.45");
        DynamicList nonProdFilesList = caseSupportHelper.getNonProdFilesList();
        caseData.setNonProdSupportFileList(nonProdFilesList);
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        nonProdSupportService.caseGenerator(caseReference, eventPayload.caseData());
        return SubmitResponse.<State>builder()
            .state(CASE_ISSUED)
            .build();
    }

}
