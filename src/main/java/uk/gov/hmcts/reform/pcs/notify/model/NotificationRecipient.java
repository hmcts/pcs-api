package uk.gov.hmcts.reform.pcs.notify.model;

import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

public record NotificationRecipient(
    String email,
    PartyEntity party,
    PcsCaseEntity pcsCase,
    ClaimEntity claim,
    PartyRole recipientRole
) { }
