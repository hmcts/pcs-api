package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.List;

@Service
@AllArgsConstructor
public class LanguageFlagDuplicateValidator {

    static final String SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE = "PF0015";
    static final String SIGN_LANGUAGE_INTERPRETER_FLAG_CODE = "RA0042";
    static final String SPOKEN_LANGUAGE_ERROR = "More than one spoken language cannot be added";
    static final String SIGN_LANGUAGE_ERROR = "More than one language cannot be added";

    private final TextAreaValidationService textAreaValidationService;

    /**
     * Checks that a case flags submission does not give any party more than one spoken language
     * interpreter (PF0015) or sign language interpreter (RA0042) flag.
     *
     * <p>Only Active and Requested flags count; Inactive and Not approved flags do not. A party's internal
     * and external flags are counted together. The same flag on different parties is allowed.
     *
     * @return the error messages to show, or an empty list when the submission can be saved
     */
    public List<String> validatePartyLanguageFlags(List<ListValue<Party>> parties) {
        if (parties == null || parties.isEmpty()) {
            return List.of();
        }

        boolean tooManySpokenLanguageFlags = false;
        boolean tooManySignLanguageFlags = false;
        for (ListValue<Party> partyValue : parties) {
            Party party = partyValue == null ? null : partyValue.getValue();
            if (party == null) {
                continue;
            }
            tooManySpokenLanguageFlags |= countFlags(party, SPOKEN_LANGUAGE_INTERPRETER_FLAG_CODE) > 1;
            tooManySignLanguageFlags |= countFlags(party, SIGN_LANGUAGE_INTERPRETER_FLAG_CODE) > 1;
        }

        if (tooManySpokenLanguageFlags && tooManySignLanguageFlags) {
            return List.of(SPOKEN_LANGUAGE_ERROR, SIGN_LANGUAGE_ERROR);
        }
        if (tooManySpokenLanguageFlags) {
            return List.of(SPOKEN_LANGUAGE_ERROR);
        }
        if (tooManySignLanguageFlags) {
            return List.of(SIGN_LANGUAGE_ERROR);
        }
        return List.of();
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> validateFlagsPage(CaseDetails<PCSCase, State> details,
                                                                          CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        return textAreaValidationService.createValidationResponse(
            caseData, validatePartyLanguageFlags(caseData.getParties()));
    }

    private int countFlags(Party party, String flagCode) {
        return countFlags(party.getDefendantFlags(), flagCode) + countFlags(party.getPartyFlagsExternal(), flagCode);
    }

    private int countFlags(Flags flags, String flagCode) {
        if (flags == null || flags.getDetails() == null) {
            return 0;
        }
        int count = 0;
        for (ListValue<FlagDetail> detail : flags.getDetails()) {
            if (detail != null && isActiveOrRequestedFlag(detail.getValue(), flagCode)) {
                count++;
            }
        }
        return count;
    }

    private boolean isActiveOrRequestedFlag(FlagDetail detail, String flagCode) {
        return detail != null
            && flagCode.equals(detail.getFlagCode())
            && (CaseFlagService.isCaseFlagActive(detail) || CaseFlagService.isCaseFlagRequested(detail));
    }
}
