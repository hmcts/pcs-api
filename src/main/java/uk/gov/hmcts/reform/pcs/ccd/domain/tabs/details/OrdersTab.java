package uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class OrdersTab {


    @CCD(label = "Possession order")
    private Document possessionOrder;

    @CCD(label = "Uploaded")
    private LocalDateTime possessionOrderUploadedDate;

    @CCD(label = "Orders table", searchable = false)
    private String tableMarkup;


}
