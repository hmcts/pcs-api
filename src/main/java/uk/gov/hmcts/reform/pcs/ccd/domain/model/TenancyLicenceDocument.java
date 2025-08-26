package uk.gov.hmcts.reform.pcs.ccd.domain.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenancyLicenceDocument {

    private String id;

    private String url;

    private String filename;

    private String binaryUrl;

    private String categoryId;

}
