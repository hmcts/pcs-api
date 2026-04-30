package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;


public record DashboardContext(
    long caseReference,
    PcsCaseEntity caseEntity,
    PartyEntity defendant
) {}
