package uk.gov.hmcts.reform.pcs.testingsupport.bulkprint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.BulkPrintService;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ResolvedRecipient;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.UUID;

/**
 * Posts a test support letter: the uploaded documents, converted for print, to the case's first claimant. It goes
 * through the same coversheet, fetch, conversion and Send Letter path as the real packs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestBulkPrintSender {

    private final PcsCaseRepository pcsCaseRepository;
    private final RecipientAddressResolver recipientAddressResolver;
    private final AddressMapper addressMapper;
    private final BulkPrintService bulkPrintService;

    /**
     * Resolves everything the post needs in one read-only transaction, so the external calls in {@link #post} touch
     * no lazy associations. Call the two through the bean, not from inside it, so the transaction applies.
     */
    @Transactional(readOnly = true)
    public ResolvedRecipient resolveClaimant(TestBulkPrintTaskData request) {
        PcsCaseEntity pcsCase = pcsCaseRepository.findByCaseReference(request.caseReference())
            .orElseThrow(() -> new IllegalStateException("No case " + request.caseReference()));
        PartyEntity claimant = pcsCase.getClaims().stream()
            .map(claim -> PartyDisplayMapper.partiesByRole(claim, PartyRole.CLAIMANT))
            .flatMap(List::stream)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Case " + request.caseReference() + " has no claimant"));
        List<DocumentEntity> documents = request.documents().stream()
            .map(upload -> DocumentEntity.builder()
                .documentId(upload.documentId())
                .fileName(upload.fileName())
                .build())
            .toList();
        return new ResolvedRecipient(pcsCase, claimant, LetterType.TEST_SUPPORT_PACK, documents,
            recipientAddressResolver.resolveDisplayName(claimant),
            addressMapper.toAddressUK(recipientAddressResolver.resolvePostalAddress(
                claimant, PartyRole.CLAIMANT, pcsCase.getPropertyAddress())));
    }

    /** Posts the letter outside any transaction: fetching, converting and sending take a while. */
    public UUID post(ResolvedRecipient claimant) {
        UUID letterId = bulkPrintService.sendPack(claimant.pcsCase(), claimant.recipient(), claimant.letterType(),
            claimant.recipientName(), claimant.address(), claimant.documents());
        log.info("Test bulk print letter {} sent for case {} with {} document(s)",
            letterId, claimant.pcsCase().getCaseReference(), claimant.documents().size());
        return letterId;
    }
}
