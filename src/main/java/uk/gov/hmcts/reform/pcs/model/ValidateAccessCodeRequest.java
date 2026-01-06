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

    @NotBlank(message = "Invalid data")
    @Size(min = 12, max = 12, message = "Invalid data")
    private String accessCode;

}
