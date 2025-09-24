package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HearingDetails {

    @JsonProperty("autolistFlag")
    private Boolean autoListFlag;

    private String listingAutoChangeReasonCode;

    private String hearingType;

    private HearingWindow hearingWindow;

    private Integer duration;

    private List<String> nonStandardHearingDurationReasons;

    private String hearingPriorityType;

    private Integer numberOfPhysicalAttendees;

    private Boolean hearingInWelshFlag;

    private List<HearingLocation> hearingLocations;

    private List<String> facilitiesRequired;

    private String listingComments;

    private String hearingRequester;

    private Boolean privateHearingRequiredFlag;

    private String leadJudgeContractType;

    private PanelRequirements panelRequirements;

    private Boolean hearingIsLinkedFlag;

    private List<String> amendReasonCodes;

    private List<String> hearingChannels;

    private boolean multiDayHearing;
}
