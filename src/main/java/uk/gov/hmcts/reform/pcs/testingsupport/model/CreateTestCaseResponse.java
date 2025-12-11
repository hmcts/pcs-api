package uk.gov.hmcts.reform.pcs.testingsupport.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestCaseResponse {

    private UUID caseId;
    private Long caseReference;
    private List<DefendantInfo> defendants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefendantInfo {
        private UUID partyId;
        private UUID idamUserId;
        private String firstName;
        private String lastName;
        private String accessCode;
    }
}
