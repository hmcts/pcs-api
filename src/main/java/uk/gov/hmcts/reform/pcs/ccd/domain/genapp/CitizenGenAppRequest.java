package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitizenGenAppRequest {

    private GenAppType applicationType;

}
