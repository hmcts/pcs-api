package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PartyFlags {

    private String partyID;

    private String partyName;

    private String flagParentId;

    private String flagId;

    private String flagDescription;

    private String flagStatus;

    private String flagComment;

    private LocalDateTime dateTimeCreated;

    private LocalDateTime dateTimeModified;
}
