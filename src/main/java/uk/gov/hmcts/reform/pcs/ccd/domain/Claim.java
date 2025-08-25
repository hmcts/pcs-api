package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    private UUID id;
    private String claimReference;
    private ClaimType type;
    private String summary;
    private CounterClaimState state; // TODO: Rename
    private List<ClaimEventLog> eventLogs;
    private Instant created;
    private String applicantEmail;
    private String respondentEmail;

}

