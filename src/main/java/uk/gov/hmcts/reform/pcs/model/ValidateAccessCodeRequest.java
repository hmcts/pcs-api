package uk.gov.hmcts.reform.pcs.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateAccessCodeRequest {

    @NotBlank(message = "Access code is required")
    private String accessCode;

}
