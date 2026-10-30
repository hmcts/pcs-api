package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;

/** A recipient with name and address already resolved, so the send phase touches no lazy associations. */
public record ResolvedRecipient(PcsCaseEntity pcsCase,
                                PartyEntity recipient,
                                LetterType letterType,
                                List<DocumentEntity> documents,
                                String recipientName,
                                AddressUK address) {
}
