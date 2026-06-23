package uk.gov.hmcts.reform.pcs.ccd.domain.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType; 
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import java.time.LocalDateTime;

@Data
@Builder
@ComplexType(generate = true)
@NoArgsConstructor
@AllArgsConstructor
public class RelatedApplication {
    private String id;
    private GenAppType type;
    private LocalDateTime applicationSubmittedDate;
}
