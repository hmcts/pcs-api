package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
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
 * Resolves each pack recipient (name + postal address) in a read-only transaction, so the senders can post
 * outside any transaction and never hold a DB connection across the external render/fetch/merge/post calls.
 */
@Service
public class PackRecipientResolver {

    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimPackSelector claimPackSelector;
    private final DefencePackSelector defencePackSelector;
    private final RecipientAddressResolver recipientAddressResolver;
    private final DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    private final AddressMapper addressMapper;

    public PackRecipientResolver(PcsCaseRepository pcsCaseRepository,
                                 ClaimPackSelector claimPackSelector,
                                 DefencePackSelector defencePackSelector,
                                 RecipientAddressResolver recipientAddressResolver,
                                 DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver,
                                 AddressMapper addressMapper) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.claimPackSelector = claimPackSelector;
        this.defencePackSelector = defencePackSelector;
        this.recipientAddressResolver = recipientAddressResolver;
        this.defenceCorrespondenceAddressResolver = defenceCorrespondenceAddressResolver;
        this.addressMapper = addressMapper;
    }

    @Transactional(readOnly = true)
    public List<ResolvedRecipient> resolveClaimRecipients(UUID caseId) {
        return pcsCaseRepository.findById(caseId)
            .map(pcsCase -> claimPackSelector.findClaimPackCandidates(pcsCase).stream()
                .map(candidate -> resolveClaimRecipient(pcsCase, candidate))
                .toList())
            .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public List<ResolvedRecipient> resolveDefenceRecipients(UUID caseId) {
        return pcsCaseRepository.findById(caseId)
            .map(pcsCase -> defencePackSelector.findDefencePackCandidates(pcsCase).stream()
                .map(candidate -> resolveDefenceRecipient(pcsCase, candidate))
                .toList())
            .orElseGet(List::of);
    }

    private ResolvedRecipient resolveClaimRecipient(PcsCaseEntity pcsCase, ClaimPackCandidate candidate) {
        PartyEntity recipient = candidate.party();
        PartyRole role = candidate.recipientType();
        return new ResolvedRecipient(pcsCase, recipient, claimLetterType(role), candidate.documents(),
            recipientAddressResolver.resolveDisplayName(recipient),
            postalAddress(recipient, role, pcsCase.getPropertyAddress()));
    }

    private ResolvedRecipient resolveDefenceRecipient(PcsCaseEntity pcsCase, DefencePackCandidate candidate) {
        PartyEntity recipient = candidate.recipient();
        PartyRole role = candidate.role();
        return new ResolvedRecipient(pcsCase, recipient, LetterType.DEFENCE_PACK, candidate.documents(),
            recipientAddressResolver.resolveDisplayName(recipient),
            correspondenceAddress(recipient, role, pcsCase.getPropertyAddress()));
    }

    private AddressUK correspondenceAddress(PartyEntity recipient, PartyRole role, AddressEntity propertyAddress) {
        if (role == PartyRole.DEFENDANT) {
            return defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(recipient, propertyAddress);
        }
        return postalAddress(recipient, role, propertyAddress);
    }

    private AddressUK postalAddress(PartyEntity recipient, PartyRole role, AddressEntity propertyAddress) {
        AddressEntity resolved = recipientAddressResolver.resolvePostalAddress(recipient, role, propertyAddress);
        return resolved == null ? null : addressMapper.toAddressUK(resolved);
    }

    private LetterType claimLetterType(PartyRole role) {
        return role == PartyRole.CLAIMANT ? LetterType.CLAIMANT_CLAIM_PACK : LetterType.DEFENDANT_CLAIM_PACK;
    }
}
