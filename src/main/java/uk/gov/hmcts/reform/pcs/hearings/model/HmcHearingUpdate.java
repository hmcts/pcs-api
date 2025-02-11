package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HmcHearingUpdate {
    private LocalDateTime hearingResponseReceivedDateTime;

    private LocalDateTime hearingEventBroadcastDateTime;

    @JsonProperty("HMCStatus")
    private String hmcStatus;

    private String hearingListingStatus;

    private LocalDateTime nextHearingDate;

    private String hearingVenueId;

    private String hearingJudgeId;

    @JsonProperty("ListAssistCaseStatus")
    private String listAssistCaseStatus;

    private String hearingRoomId;

}
