package uk.gov.hmcts.reform.pcs.ccd.event;

import com.google.common.collect.ImmutableSet;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEventTest {

    protected static final long TEST_CASE_REFERENCE = 1234L;

    protected Event<PCSCase, UserRole, State> configuredEvent;

    protected void setEventUnderTest(CCDConfig<PCSCase, State, UserRole> eventUnderTest) {
        configuredEvent = getEvent(buildEventConfig(eventUnderTest));
    }

    protected PCSCase callStartHandler(PCSCase caseData) {
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);
        Start<PCSCase, State> startHandler = getConfiguredEvent().getStartHandler();
        return startHandler.start(eventPayload);
    }

    protected void callSubmitHandler(PCSCase caseData) {
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(TEST_CASE_REFERENCE, caseData, null);
        Submit<PCSCase, State> submitHandler = getConfiguredEvent().getSubmitHandler();
        submitHandler.submit(eventPayload);
    }

    private ResolvedCCDConfig<PCSCase, State, UserRole> buildEventConfig(
        CCDConfig<PCSCase, State, UserRole> eventConfig) {

        ConfigBuilderImpl<PCSCase, State, UserRole> configBuilder = createConfigBuilder();
        eventConfig.configure(configBuilder);
        return configBuilder.build();
    }

    private ConfigBuilderImpl<PCSCase, State, UserRole> createConfigBuilder() {
        ResolvedCCDConfig<PCSCase, State, UserRole> initialCCDConfig
            = new ResolvedCCDConfig<>(PCSCase.class, State.class, UserRole.class, Map.of(), ImmutableSet.of());

        return new ConfigBuilderImpl<>(initialCCDConfig);
    }

    private Event<PCSCase, UserRole, State> getEvent(ResolvedCCDConfig<PCSCase, State, UserRole> resolvedCCDConfig) {
        Collection<Event<PCSCase, UserRole, State>> events = resolvedCCDConfig.getEvents().values();

        assertThat(events)
            .withFailMessage("There should be exactly 1 event configured")
            .hasSize(1);

        return events.iterator().next();
    }

    private Event<PCSCase, UserRole, State> getConfiguredEvent() {
        assertThat(configuredEvent)
            .withFailMessage("Call setEventUnderTest() before running any tests")
            .isNotNull();

        return configuredEvent;
    }

}
