package uk.gov.hmcts.reform.pcs.ccd.domain.bailiff;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WarrantReturn {

    private WarrantReturnType returnType;

    private WarrantReturnName returnName;

    private EvictionOutcome evictionOutcome;

    private String additionalDetails;

    private LocalDateTime outcomeDate;

}
