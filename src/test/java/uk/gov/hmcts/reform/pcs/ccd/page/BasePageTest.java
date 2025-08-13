package uk.gov.hmcts.reform.pcs.ccd.page;

import com.google.common.collect.ImmutableSet;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BasePageTest {

    private static final String TEST_EVENT_ID = "test-event";

    protected Event<PCSCase, UserRole, State> buildPageInTestEvent(CcdPageConfiguration page) {
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

    @SuppressWarnings("unchecked")
    protected static MidEvent<PCSCase, State> getMidEventForPage(Event<PCSCase, UserRole, State> event,
                                                                 String pageId) {
        MidEvent<PCSCase, State> midEvent = event.getFields().getPagesToMidEvent().get(pageId);

        assertThat(midEvent)
            .withFailMessage("No mid event found for page with ID %s", pageId)
            .isNotNull();

        return midEvent;
    }

}
