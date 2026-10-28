package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ServiceHearingValues {

    private String caseId;
    private String hmctsServiceID;
    private String hmctsInternalCaseName;
    private String publicCaseName;
    private Boolean caseAdditionalSecurityFlag = false;
    private List<CaseCategory> caseCategories;
    private String caseDeepLink;
    @JsonProperty("caserestrictedFlag")
    private Boolean caseRestrictedFlag;
    private String caseManagementLocationCode;
    @JsonProperty("caseSLAStartDate")
    private LocalDate caseSlaStartDate;
    private boolean autoListFlag;
    private String hearingType;
    private HearingWindow hearingWindow;
    private List<String> hearingChannels;
    private Integer duration;
    private String hearingPriorityType;
    private Integer numberOfPhysicalAttendees;
    private boolean hearingInWelshFlag;
    private List<HearingLocation> hearingLocations;
    private String listingComments;
    private boolean privateHearingRequiredFlag;
    private Boolean caseInterpreterRequiredFlag;
    private PanelRequirements panelRequirements;
    private boolean hearingIsLinkedFlag;
    private List<PartyDetails> parties;  // check PartyDetails.individualDetails.relatedParties
    private CaseFlags caseFlags;
    private List<ScreenNavigation> screenFlow;

}
