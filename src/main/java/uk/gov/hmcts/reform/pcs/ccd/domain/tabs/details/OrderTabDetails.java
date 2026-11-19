package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTabDetails {

    @CCD(label = "Type")
    private String type;

    @CCD(label = "Uploaded on")
    private LocalDateTime uploadedOn;

    @CCD(label = "Document URL")
    private Document document;

}
