package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.UUID;

/**
 * Sends the defence-phase packs for one case in a transaction. Each recipient (the responding defendant, the
 * claimant, or a co-defendant served the counter-claim) is posted one envelope of their unsent documents; a
 * defendant's address is assertion-first, the claimant's is their own. {@link PackSendRecorder} records each
 * document and isolates a failure so one skip never aborts the case.
 */
@Service
public class DefencePackSender {

    private final PcsCaseRepository pcsCaseRepository;
    private final DefencePackSelector defencePackSelector;
    private final DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    private final RecipientAddressResolver recipientAddressResolver;
    private final AddressMapper addressMapper;
    private final BulkPrintService bulkPrintService;
    private final PackSendRecorder packSendRecorder;

    public DefencePackSender(PcsCaseRepository pcsCaseRepository,
                             DefencePackSelector defencePackSelector,
                             DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver,
                             RecipientAddressResolver recipientAddressResolver,
                             AddressMapper addressMapper,
                             BulkPrintService bulkPrintService,
                             PackSendRecorder packSendRecorder) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.defencePackSelector = defencePackSelector;
        this.defenceCorrespondenceAddressResolver = defenceCorrespondenceAddressResolver;
        this.recipientAddressResolver = recipientAddressResolver;
        this.addressMapper = addressMapper;
        this.bulkPrintService = bulkPrintService;
        this.packSendRecorder = packSendRecorder;
    }

    @Transactional
    public void sendDefencePacks(UUID caseId) {
        pcsCaseRepository.findById(caseId).ifPresent(pcsCase ->
            defencePackSelector.findDefencePackCandidates(pcsCase)
                .forEach(candidate -> sendToRecipient(pcsCase, candidate)));
    }

    private void sendToRecipient(PcsCaseEntity pcsCase, DefencePackCandidate candidate) {
        PartyEntity recipient = candidate.recipient();
        PartyRole role = candidate.role();
        List<DocumentEntity> documents = candidate.documents();

        packSendRecorder.send(pcsCase, recipient, LetterType.DEFENCE_PACK, documents, () -> {
            String recipientName = recipientAddressResolver.resolveDisplayName(recipient);
            AddressUK address = resolveAddress(recipient, role, pcsCase.getPropertyAddress());
            return bulkPrintService.sendPack(
                pcsCase, recipient, LetterType.DEFENCE_PACK, recipientName, address, documents);
        });
    }

    private AddressUK resolveAddress(PartyEntity recipient, PartyRole role, AddressEntity propertyAddress) {
        if (role == PartyRole.DEFENDANT) {
            return defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(recipient, propertyAddress);
        }
        AddressEntity postalAddress = recipientAddressResolver.resolvePostalAddress(recipient, role, propertyAddress);
        return postalAddress == null ? null : addressMapper.toAddressUK(postalAddress);
    }
}
