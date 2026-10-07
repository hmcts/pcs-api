package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeTabDetails {

    @CCD(label = "Date notice was served")
    private String noticeServedDate;
}
