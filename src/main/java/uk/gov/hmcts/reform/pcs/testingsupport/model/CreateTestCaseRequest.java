package uk.gov.hmcts.reform.pcs.testingsupport.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestCaseRequest {

    private Long caseReference;

    @NotNull(message = "Property address is required")
    @Valid
    private AddressUK propertyAddress;

    @NotNull(message = "Legislative country is required")
    private LegislativeCountry legislativeCountry;

    @NotNull(message = "Defendants list is required")
    @Size(min = 1, message = "At least 1 defendant is required")
    @Valid
    private List<DefendantRequest> defendants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefendantRequest {
        private UUID idamUserId;
        private String firstName;
        private String lastName;
    }
}
