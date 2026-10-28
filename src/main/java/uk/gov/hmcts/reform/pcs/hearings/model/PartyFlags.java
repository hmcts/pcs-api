package uk.gov.hmcts.reform.pcs.hearings.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartyFlags {

    private String partyID;
    private String partyName;
    private String flagId;
    private String flagDescription;
    private String flagStatus;
    private LocalDateTime dateTimeCreated;
    private LocalDateTime dateTimeModified;
}
