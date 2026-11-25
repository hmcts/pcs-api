package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeServiceMethodTabDetails {

    @CCD(label = "Method of service")
    private String method;

    @CCD(label = "Date notice served")
    private String dateServed;

    @CCD(label = "Details")
    private String details;
}
