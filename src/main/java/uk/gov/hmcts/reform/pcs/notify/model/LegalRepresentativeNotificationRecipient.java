package uk.gov.hmcts.reform.pcs.notify.model;

import uk.gov.hmcts.reform.pcs.LegalRepresentative;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

public record LegalRepresentativeNotificationRecipient(
                                                      String email,
                                                      PartyEntity party,
                                                      PcsCaseEntity pcsCase,
                                                      ClaimEntity claim
) { }
