package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HearingDaySchedule {

    private LocalDateTime hearingStartDateTime;

    private LocalDateTime hearingEndDateTime;

    @JsonProperty("listAssistSessionID")
    private String listAssistSessionId;

    private String hearingVenueId;

    private String  hearingRoomId;

    private String hearingJudgeId;

    private List<String> panelMemberIds;

    private List<Attendee> attendees;

}
