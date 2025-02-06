package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CaseHearing {

    @JsonProperty("hearingID")
    private Long hearingId;

    private LocalDateTime hearingRequestDateTime;

    private String hearingType;

    private String hmcStatus;

    private LocalDateTime lastResponseReceivedDateTime;

    private Integer requestVersion;

    private String hearingListingStatus;

    private String listAssistCaseStatus;

    private List<HearingDaySchedule> hearingDaySchedule;

    private String hearingGroupRequestId;

    private Boolean hearingIsLinkedFlag;

    private List<String> hearingChannels;

}
