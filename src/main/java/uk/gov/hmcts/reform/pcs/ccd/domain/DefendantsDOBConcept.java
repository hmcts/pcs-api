package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantsDOBConcept {

    @CCD(showCondition = "firstName1!=\"999\"")
    private String firstName1;

    @CCD(showCondition = "firstName2!=\"999\"")
    private String firstName2;

    @CCD(showCondition = "firstName3!=\"999\"")
    private String firstName3;

    @CCD(showCondition = "firstName4!=\"999\"")
    private String firstName4;

    @CCD(showCondition = "firstName5!=\"999\"")
    private String firstName5;


    @CCD
    private LocalDate dob1;
    @CCD
    private LocalDate dob2;
    @CCD
    private LocalDate dob3;
    @CCD
    private LocalDate dob4;
    @CCD
    private LocalDate dob5;

}

