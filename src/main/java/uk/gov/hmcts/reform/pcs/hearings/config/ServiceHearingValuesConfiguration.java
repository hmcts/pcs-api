package uk.gov.hmcts.reform.pcs.hearings.config;

import lombok.Data;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.reform.pcs.hearings.model.HearingLocation;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.Navigation;
import uk.gov.hmcts.reform.pcs.hearings.model.PanelRequirements;
import uk.gov.hmcts.reform.pcs.hearings.model.PartyDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.ScreenNavigation;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;

@Data
@Component
@ConfigurationProperties("service-hearing-values")
public class ServiceHearingValuesConfiguration {

    static final String SCREEN_NAV_WALES_REGION_ID = "7";
    static final String SCREEN_NAV_REGION_ID_CONDITION_KEY = "regionId";
    static final String SCREEN_NAV_INCLUDE = "INCLUDE";
    static final String SCREEN_NAV_NOT_INCLUDE = "NOT INCLUDE";
    static final String SCREEN_NAV_HEARING_PANEL = "hearing-panel";
    static final String SCREEN_NAV_HEARING_FACILITIES = "hearing-facilities";
    static final String SCREEN_NAV_HEARING_STAGE = "hearing-stage";
    static final String SCREEN_NAV_HEARING_ATTENDANCE = "hearing-attendance";
    static final String SCREEN_NAV_HEARING_VENUE = "hearing-venue";
    static final String SCREEN_NAV_HEARING_WELSH = "hearing-welsh";
    static final String SCREEN_NAV_HEARING_JUDGE = "hearing-judge";
    static final String SCREEN_NAV_HEARING_TIMING = "hearing-timing";
    static final String SCREEN_NAV_HEARING_LINK = "hearing-link";
    static final String SCREEN_NAV_HEARING_ADDITIONAL_INSTRUCTIONS = "hearing-additional-instructions";

    private String hmctsServiceId;
    private boolean caseAdditionalSecurityFlag;
    private boolean caseRestrictedFlag;
    private boolean autoListFlag;
    private boolean hearingInWelshFlag;
    private boolean privateHearingRequiredFlag;
    private boolean hearingIsLinkedFlag;
    private String locationType;
    private List<String> roleTypes;
    private String preferredHearingChannel;
    private int numberOfDaysOffset;
    private int numberOfDaysStartOffset;
    private String hearingType;
    private List<String> hearingChannels;
    private String hearingPriority;

    public ServiceHearingValues getDefaultValues() {
        List<PartyDetails> parties = List.of(
            PartyDetails.builder()
                .individualDetails(IndividualDetails.builder()
                    .preferredHearingChannel(preferredHearingChannel)
                    .build())
                .build()
        );

        return ServiceHearingValues.builder()
            .hmctsServiceID(hmctsServiceId)
            .caseAdditionalSecurityFlag(caseAdditionalSecurityFlag)
            .caseRestrictedFlag(caseRestrictedFlag)
            .autoListFlag(autoListFlag)
            .hearingInWelshFlag(hearingInWelshFlag)
            .privateHearingRequiredFlag(privateHearingRequiredFlag)
            .hearingIsLinkedFlag(hearingIsLinkedFlag)
            .hearingType(hearingType)
            .hearingChannels(hearingChannels)
            .hearingPriorityType(getHearingPriority())
            .hearingLocations(List.of(
                HearingLocation.builder()
                    .locationType(locationType)
                    .build()
            ))
            .panelRequirements(PanelRequirements.builder()
                .roleType(roleTypes)
                .build())
            .parties(parties)
            .screenFlow(screenFlow())
            .build();
    }

    private static List<ScreenNavigation> screenFlow() {
        return List.of(
            nextScreen(SCREEN_NAV_HEARING_PANEL, SCREEN_NAV_HEARING_FACILITIES),
            nextScreen(SCREEN_NAV_HEARING_FACILITIES, SCREEN_NAV_HEARING_STAGE),
            nextScreen(SCREEN_NAV_HEARING_STAGE, SCREEN_NAV_HEARING_ATTENDANCE),
            nextScreen(SCREEN_NAV_HEARING_ATTENDANCE, SCREEN_NAV_HEARING_VENUE),
            ScreenNavigation.builder()
                .screenName(SCREEN_NAV_HEARING_VENUE)
                .conditionKey(SCREEN_NAV_REGION_ID_CONDITION_KEY)
                .navigation(List.of(
                    Navigation.builder()
                        .conditionOperator(SCREEN_NAV_INCLUDE)
                        .conditionValue(SCREEN_NAV_WALES_REGION_ID)
                        .resultValue(SCREEN_NAV_HEARING_WELSH)
                        .build(),
                    Navigation.builder()
                        .conditionOperator(SCREEN_NAV_NOT_INCLUDE)
                        .conditionValue(SCREEN_NAV_WALES_REGION_ID)
                        .resultValue(SCREEN_NAV_HEARING_JUDGE)
                        .build()))
                .build(),
            nextScreen(SCREEN_NAV_HEARING_WELSH, SCREEN_NAV_HEARING_JUDGE),
            nextScreen(SCREEN_NAV_HEARING_JUDGE, SCREEN_NAV_HEARING_TIMING),
            nextScreen(SCREEN_NAV_HEARING_TIMING, SCREEN_NAV_HEARING_LINK),
            nextScreen(SCREEN_NAV_HEARING_LINK, SCREEN_NAV_HEARING_ADDITIONAL_INSTRUCTIONS),
            ScreenNavigation.builder().screenName(SCREEN_NAV_HEARING_ADDITIONAL_INSTRUCTIONS).build()
        );
    }

    private static ScreenNavigation nextScreen(String screenName, String nextScreenName) {
        return ScreenNavigation.builder()
            .screenName(screenName)
            .navigation(List.of(Navigation.builder().resultValue(nextScreenName).build()))
            .build();
    }
}