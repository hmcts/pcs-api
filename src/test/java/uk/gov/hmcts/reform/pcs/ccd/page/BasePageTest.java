package uk.gov.hmcts.reform.pcs.ccd.page;

import com.google.common.collect.ImmutableSet;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BasePageTest {

    protected static final long TEST_CASE_REFERENCE = 1234L;

    private static final String TEST_EVENT_ID = "test-event";
    private static final String TEST_FIELD_PREFIX = "tst";
    protected Event<PCSCase, UserRole, State> event;

    protected void setPageUnderTest(CcdPageConfiguration pageUnderTest) {
        event = buildPageInTestEvent(pageUnderTest);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <D> void setDtoPageUnderTest(Class<D> dtoClass,
        Consumer<Event.EventBuilder<D, UserRole, State>> pageSetup) {
        ConfigBuilderImpl<PCSCase, State, UserRole> configBuilder = createConfigBuilder();
        Event.EventBuilder eventBuilder = (Event.EventBuilder) configBuilder
            .decentralisedEvent(TEST_EVENT_ID, dtoClass, TEST_FIELD_PREFIX, null)
            .forAllStates();
        pageSetup.accept(eventBuilder);
        ResolvedCCDConfig<PCSCase, State, UserRole> resolvedCCDConfig = configBuilder.build();
        event = resolvedCCDConfig.getEvents().get(TEST_EVENT_ID);
        assertThat(event).isNotNull();
    }

    protected AboutToStartOrSubmitResponse<PCSCase, State> callMidEventHandler(PCSCase caseData) {
        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .id(TEST_CASE_REFERENCE)
            .data(caseData)
            .build();

        return getMidEventForPage().handle(caseDetails, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <D> AboutToStartOrSubmitResponse<D, State> callDtoMidEventHandler(D caseData) {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(TEST_CASE_REFERENCE)
            .data(caseData)
            .build();

        return (AboutToStartOrSubmitResponse<D, State>) getMidEventForPage().handle(caseDetails, null);
    }

    private Event<PCSCase, UserRole, State> buildPageInTestEvent(CcdPageConfiguration page) {
        ConfigBuilderImpl<PCSCase, State, UserRole> configBuilder = createConfigBuilder();
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = createEventBuilder(configBuilder);

        PageBuilder pageBuilder = new PageBuilder(eventBuilder);
        page.addTo(pageBuilder);

        ResolvedCCDConfig<PCSCase, State, UserRole> resolvedCCDConfig = configBuilder.build();
        Event<PCSCase, UserRole, State> event = resolvedCCDConfig.getEvents().get(TEST_EVENT_ID);
        assertThat(event).isNotNull();

        return event;
    }

    private ConfigBuilderImpl<PCSCase, State, UserRole> createConfigBuilder() {
        ResolvedCCDConfig<PCSCase, State, UserRole> initialCCDConfig
            = new ResolvedCCDConfig<>(PCSCase.class, State.class, UserRole.class, Map.of(), ImmutableSet.of());

        return new ConfigBuilderImpl<>(initialCCDConfig);
    }

    private static Event.EventBuilder<PCSCase, UserRole, State> createEventBuilder(
        ConfigBuilderImpl<PCSCase, State, UserRole> configBuilder) {

        return configBuilder
            .decentralisedEvent(TEST_EVENT_ID, null)
            .forAllStates();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private MidEvent<PCSCase, State> getMidEventForPage() {
        Collection<MidEvent> midEventHandlers = getEvent().getFields().getPagesToMidEvent().values();

        assertThat(midEventHandlers)
            .withFailMessage("There should be exactly 1 page mid event handler configured")
            .hasSize(1);

        return midEventHandlers.iterator().next();
    }

    private Event<PCSCase, UserRole, State> getEvent() {
        assertThat(event)
            .withFailMessage("Call setPageUnderTest() before running any tests")
            .isNotNull();

        return event;
    }

}
