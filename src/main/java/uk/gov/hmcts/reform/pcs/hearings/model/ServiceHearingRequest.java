package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceHearingRequest {

    public static final String CASE_REFERENCE_PATTERN = "\\d{16}";

    @NotBlank(message = "caseReference is required")
    @Pattern(regexp = CASE_REFERENCE_PATTERN, message = "caseReference must be a 16-digit case reference")
    @JsonProperty("caseReference")
    private String caseId;

    private String hearingId;
}
