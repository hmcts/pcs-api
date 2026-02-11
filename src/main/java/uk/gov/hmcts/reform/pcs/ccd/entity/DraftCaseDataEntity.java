package uk.gov.hmcts.reform.pcs.ccd.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DraftCaseDataEntity {

    private UUID id;

    private Long caseReference;

    private String caseData;

    private EventId eventId;

    private UUID idamUserId;

}
