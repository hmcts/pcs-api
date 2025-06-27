package uk.gov.hmcts.reform.pcs.ccd.event;

import com.google.common.collect.ImmutableSet;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Map;

class BaseEventTest {

    protected ResolvedCCDConfig<PCSCase, State, UserRole> buildEventConfig(
        CCDConfig<PCSCase, State, UserRole> eventConfig) {

        ConfigBuilderImpl<PCSCase, State, UserRole> configBuilder = createConfigBuilder();
        eventConfig.configure(configBuilder);
        return configBuilder.build();
    }

    protected ConfigBuilderImpl<PCSCase, State, UserRole> createConfigBuilder() {
        ResolvedCCDConfig<PCSCase, State, UserRole> initialCCDConfig
            = new ResolvedCCDConfig<>(PCSCase.class, State.class, UserRole.class, Map.of(), ImmutableSet.of());

        return new ConfigBuilderImpl<>(initialCCDConfig);
    }

    protected Event<PCSCase, UserRole, State> getEvent(EventId eventId,
                                                       ResolvedCCDConfig<PCSCase, State, UserRole> resolvedCCDConfig) {

        return resolvedCCDConfig.getEvents().get(eventId.name());
    }

}
