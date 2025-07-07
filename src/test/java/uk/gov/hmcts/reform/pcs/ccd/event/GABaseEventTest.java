package uk.gov.hmcts.reform.pcs.ccd.event;

import com.google.common.collect.ImmutableSet;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.GACase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Map;

class GABaseEventTest {


    protected ResolvedCCDConfig<GACase, State, UserRole> buildEventConfig(
        CCDConfig<GACase, State, UserRole> eventConfig) {

        ConfigBuilderImpl<GACase, State, UserRole> configBuilder = createConfigBuilder();
        eventConfig.configure(configBuilder);
        return configBuilder.build();
    }

    protected ConfigBuilderImpl<GACase, State, UserRole> createConfigBuilder() {
        ResolvedCCDConfig<GACase, State, UserRole> initialCCDConfig
            = new ResolvedCCDConfig<>(GACase.class, State.class, UserRole.class, Map.of(), ImmutableSet.of());

        return new ConfigBuilderImpl<>(initialCCDConfig);
    }

    protected Event<GACase, UserRole, State> getEvent(EventId eventId,
                                                       ResolvedCCDConfig<GACase, State, UserRole> resolvedCCDConfig) {

        return resolvedCCDConfig.getEvents().get(eventId.name());
    }
}
