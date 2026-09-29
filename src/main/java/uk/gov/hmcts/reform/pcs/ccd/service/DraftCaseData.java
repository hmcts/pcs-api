package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.UUID;

@Builder
@Data
public class DraftCaseData {
    private long caseReference;
    private EventId eventId;
    private UUID partyId;
    private UUID userId;
    private String organisationId;
}
