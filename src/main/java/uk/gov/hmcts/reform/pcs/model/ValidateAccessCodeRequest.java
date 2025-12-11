package uk.gov.hmcts.reform.pcs.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateAccessCodeRequest {

    @NotBlank(message = "Access code is required")
    @Size(min = 12, max = 12, message = "Access code must be exactly 12 characters")
    private String accessCode;

}
