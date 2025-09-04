package uk.gov.hmcts.reform.pcs.ccd.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
// Represents the tenancy licence details for a possessions case.
// This class is a placeholder for future fields that may be added as the data model evolves.
public class TenancyLicence {

    private Boolean noticeServed;

    private TenancyLicenceType tenancyLicenceType;

    private String detailsOfOtherTypeOfTenancyLicence;

    private String tenancyLicenceDate;

    private List<TenancyLicenceDocument> documents;

}
