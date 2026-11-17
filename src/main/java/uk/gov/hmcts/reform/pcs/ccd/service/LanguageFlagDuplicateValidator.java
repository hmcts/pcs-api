package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.util.FlagVisibilityConverter.toFlagVisibility;

@Service
public class LanguageFlagDuplicateValidator {

    static final String SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE = "PF0015";
    static final String SIGN_LANGUAGE_INTERPRETER_FLAG_CODE = "RA0042";
    static final String SPOKEN_LANGUAGE_ERROR = "more than one spoken language cannot be added";
    static final String SIGN_LANGUAGE_ERROR = "more than one language cannot be added";
    static final Set<String> ACTIVE_OR_REQUESTED_STATUSES = Set.of("Active", "Requested");

    // checks each person on this save against the flags already on the case
    public List<String> validatePartyLanguageFlags(List<ListValue<Party>> incomingParties,
                                                   Set<PartyEntity> existingParties) {
        if (incomingParties == null || incomingParties.isEmpty()) {
            return List.of();
        }

        boolean spokenLanguageDuplicateAdded = false;
        boolean signLanguageDuplicateAdded = false;
        for (ListValue<Party> incomingParty : incomingParties) {
            PartyEntity existingParty = findExistingParty(incomingParty, existingParties);
            if (existingParty == null) {
                continue;
            }
            spokenLanguageDuplicateAdded |= addsDuplicateFlag(
                existingParty, incomingParty.getValue(), SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE);
            signLanguageDuplicateAdded |= addsDuplicateFlag(
                existingParty, incomingParty.getValue(), SIGN_LANGUAGE_INTERPRETER_FLAG_CODE);
        }

        if (spokenLanguageDuplicateAdded && signLanguageDuplicateAdded) {
            return List.of(SPOKEN_LANGUAGE_ERROR, SIGN_LANGUAGE_ERROR);
        }
        if (spokenLanguageDuplicateAdded) {
            return List.of(SPOKEN_LANGUAGE_ERROR);
        }
        if (signLanguageDuplicateAdded) {
            return List.of(SIGN_LANGUAGE_ERROR);
        }
        return List.of();
    }

    // finds that person on the case so we can see the flags they already have
    private PartyEntity findExistingParty(ListValue<Party> incomingParty, Set<PartyEntity> existingParties) {
        if (incomingParty == null || incomingParty.getId() == null || existingParties == null) {
            return null;
        }
        UUID partyId;
        try {
            partyId = UUID.fromString(incomingParty.getId());
        } catch (IllegalArgumentException ex) {
            return null;
        }
        for (PartyEntity existingParty : existingParties) {
            if (existingParty != null && partyId.equals(existingParty.getId())) {
                return existingParty;
            }
        }
        return null;
    }

    // true if this save leaves more than one of this flag and more than they had before
    private boolean addsDuplicateFlag(PartyEntity existingParty, Party incomingParty, String flagCode) {
        int before = countExistingFlags(existingParty, null, flagCode);
        int after = countFlagsAfterSave(existingParty, incomingParty, FlagVisibility.INTERNAL, flagCode)
            + countFlagsAfterSave(existingParty, incomingParty, FlagVisibility.EXTERNAL, flagCode);
        return after > 1 && after > before;
    }

    // flags this save would leave, or the saved ones if the caseworker or citizen list wasnt sent
    private int countFlagsAfterSave(PartyEntity existingParty, Party incomingParty,
                                    FlagVisibility visibility, String flagCode) {
        Flags incomingFlags = getIncomingFlags(incomingParty, visibility);
        if (hasNoFlagDetails(incomingFlags)) {
            return countExistingFlags(existingParty, visibility, flagCode);
        }
        int count = 0;
        for (ListValue<FlagDetail> detail : incomingFlags.getDetails()) {
            if (detail != null && isActiveOrRequestedFlag(detail.getValue(), flagCode)) {
                count++;
            }
        }
        return count;
    }

    // flags already saved, from both the caseworker and citizen lists when no visibility is passed
    private int countExistingFlags(PartyEntity existingParty, FlagVisibility visibility, String flagCode) {
        List<CasePartyFlagEntity> existingFlags = existingParty.getDefendantFlags();
        if (existingFlags == null) {
            return 0;
        }
        int count = 0;
        for (CasePartyFlagEntity existingFlag : existingFlags) {
            if (existingFlag == null) {
                continue;
            }
            if (visibility != null && visibility != toFlagVisibility(existingFlag.getVisibility())) {
                continue;
            }
            if (flagCode.equals(existingFlagCode(existingFlag))
                && isActiveOrRequested(existingFlag.getDefaultStatus())) {
                count++;
            }
        }
        return count;
    }

    private static String existingFlagCode(CasePartyFlagEntity existingFlag) {
        return existingFlag.getFlagRefData() == null ? null : existingFlag.getFlagRefData().getFlagCode();
    }

    // caseworker flags or citizen flags
    private Flags getIncomingFlags(Party incomingParty, FlagVisibility visibility) {
        if (incomingParty == null) {
            return null;
        }
        return visibility == FlagVisibility.EXTERNAL
            ? incomingParty.getPartyFlagsExternal()
            : incomingParty.getDefendantFlags();
    }

    private boolean hasNoFlagDetails(Flags flags) {
        return flags == null || flags.getDetails() == null || flags.getDetails().isEmpty();
    }

    // this flag code, and active or requested
    private boolean isActiveOrRequestedFlag(FlagDetail detail, String flagCode) {
        return detail != null
            && flagCode.equals(detail.getFlagCode())
            && isActiveOrRequested(detail.getStatus());
    }

    private static boolean isActiveOrRequested(String status) {
        return status != null && ACTIVE_OR_REQUESTED_STATUSES.contains(status);
    }
}
