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
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.UUID;

/**
 * Sends the claim packs for one case, in a transaction so lazy case data can be read. Each recipient is posted
 * independently; {@link PackSendRecorder} records the outcome and isolates a failure so one skip never aborts the case.
 */
@Service
public class ClaimPackSender {

    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimPackSelector claimPackSelector;
    private final RecipientAddressResolver recipientAddressResolver;
    private final AddressMapper addressMapper;
    private final BulkPrintService bulkPrintService;
    private final PackSendRecorder packSendRecorder;

    public ClaimPackSender(PcsCaseRepository pcsCaseRepository,
                           ClaimPackSelector claimPackSelector,
                           RecipientAddressResolver recipientAddressResolver,
                           AddressMapper addressMapper,
                           BulkPrintService bulkPrintService,
                           PackSendRecorder packSendRecorder) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.claimPackSelector = claimPackSelector;
        this.recipientAddressResolver = recipientAddressResolver;
        this.addressMapper = addressMapper;
        this.bulkPrintService = bulkPrintService;
        this.packSendRecorder = packSendRecorder;
    }

    @Transactional
    public void sendClaimPacks(UUID caseId) {
        pcsCaseRepository.findById(caseId).ifPresent(pcsCase ->
            claimPackSelector.findClaimPackCandidates(pcsCase)
                .forEach(candidate -> sendToRecipient(pcsCase, candidate)));
    }

    private void sendToRecipient(PcsCaseEntity pcsCase, ClaimPackCandidate candidate) {
        PartyEntity recipient = candidate.party();
        PartyRole role = candidate.recipientType();
        LetterType letterType = letterTypeFor(role);
        List<DocumentEntity> documents = candidate.documents();

        packSendRecorder.send(pcsCase, recipient, letterType, documents, () -> {
            String recipientName = recipientAddressResolver.resolveDisplayName(recipient);
            AddressUK address = resolveAddress(recipient, role, pcsCase.getPropertyAddress());
            return bulkPrintService.sendPack(pcsCase, recipient, letterType, recipientName, address, documents);
        });
    }

    private AddressUK resolveAddress(PartyEntity recipient, PartyRole role, AddressEntity propertyAddress) {
        AddressEntity postalAddress = recipientAddressResolver.resolvePostalAddress(recipient, role, propertyAddress);
        return postalAddress == null ? null : addressMapper.toAddressUK(postalAddress);
    }

    private LetterType letterTypeFor(PartyRole role) {
        return role == PartyRole.CLAIMANT ? LetterType.CLAIMANT_CLAIM_PACK : LetterType.DEFENDANT_CLAIM_PACK;
    }
}
