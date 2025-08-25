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
public class GenApp {

    private UUID id;
    private String genAppReference;
    private String summary;
    private GenAppState state; // TODO: Rename
    private List<GenAppEventLog> eventLogs;
    private Instant created;

}

