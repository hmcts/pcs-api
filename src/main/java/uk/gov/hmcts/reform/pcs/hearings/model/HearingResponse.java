package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HearingResponse {

    @JsonProperty("hearingRequestID")
    private Long hearingRequestId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("timeStamp")
    private LocalDateTime timeStamp;

    @JsonProperty("versionNumber")
    private Integer versionNumber;

    @JsonProperty("responseVersion")
    private Integer responseVersion;

    @JsonProperty("requestVersion")
    private Integer requestVersion;

    @JsonProperty("partiesNotified")
    private LocalDateTime partiesNotifiedDateTime;

    @JsonProperty("serviceData")
    private Map<String, Object> serviceData;

}
