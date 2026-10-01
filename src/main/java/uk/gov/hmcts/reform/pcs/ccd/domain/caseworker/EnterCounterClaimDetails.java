package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class EnterCounterClaimDetails {

    @CCD(label = "Has the court given the party permission to enter a counterclaim?")
    private VerticalYesNo courtPermissionGranted;

    @CCD(label = "Enter the date the order was made granting permission", hint = "For example, 16 4 2021")
    private LocalDate permissionOrderDate;

    @CCD(label = "When was the counterclaim received?", hint = "For example, 16 4 2021")
    private LocalDate claimReceivedDate;

    @CCD(label = "Which type of counterclaim is this?")
    private CounterClaimType claimTypeOption;

}
