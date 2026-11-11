package uk.gov.hmcts.reform.pcs.hearings.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;

/**
 * Fills the hearing fields that come from one person's flags.
*/
@Service
public class IndividualDetailsFlagMapper {

    static final String ACTIVE = "Active";
    static final String VULNERABLE_USER = "PF0002";
    static final String SPOKEN_LANGUAGE_INTERPRETER = "PF0015";
    static final String SIGN_LANGUAGE_INTERPRETER = "RA0042";

    public IndividualDetails map(List<CasePartyFlagEntity> flags) {
        IndividualDetails details = new IndividualDetails();
        CasePartyFlagEntity vulnerableUser = activeVulnerableUser(flags);
        details.setVulnerableFlag(vulnerableUser != null);
        if (vulnerableUser != null && hasText(vulnerableUser.getFlagComment())) {
            details.setVulnerabilityDetails(vulnerableUser.getFlagComment());
        }
        List<String> adjustmentCodes = reasonableAdjustmentCodes(flags);
        if (!adjustmentCodes.isEmpty()) {
            details.setReasonableAdjustments(adjustmentCodes);
        }
        String notes = otherReasonableAdjustmentDetails(flags);
        if (notes != null) {
            details.setOtherReasonableAdjustmentDetails(notes);
        }
        return details;
    }

    private String otherReasonableAdjustmentDetails(List<CasePartyFlagEntity> flags) {
        if (flags == null) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        addGroup(lines, "Reasonable Adjustments", commentedFlags(flags, "RA"));
        addGroup(lines, "Special Measures", commentedFlags(flags, "SM"));
        addGroup(lines, "Party Flags", partyFlagsForNotes(flags));
        if (lines.isEmpty()) {
            return null;
        }
        return String.join("\n", lines);
    }

    private void addGroup(List<String> lines, String heading, List<CasePartyFlagEntity> flags) {
        if (flags.isEmpty()) {
            return;
        }
        String entries = String.join(", ", flags.stream().map(this::noteEntry).toList());
        lines.add(heading + ": " + entries + ".");
    }

    private String noteEntry(CasePartyFlagEntity flag) {
        String name = flag.getFlagRefData().getFlagName();
        if (!hasText(flag.getFlagComment())) {
            return name;
        }
        return name + " (Comment: " + flag.getFlagComment() + ")";
    }

    private List<CasePartyFlagEntity> commentedFlags(List<CasePartyFlagEntity> flags, String prefix) {
        return flags.stream()
            .filter(flag -> hasCodePrefix(flag, prefix))
            .filter(flag -> !SIGN_LANGUAGE_INTERPRETER.equals(flag.getFlagRefData().getFlagCode()))
            .filter(flag -> hasText(flag.getFlagComment()))
            .filter(flag -> hasText(flag.getFlagRefData().getFlagName()))
            .sorted(Comparator.comparing(flag -> flag.getFlagRefData().getFlagCode()))
            .toList();
    }

    private List<CasePartyFlagEntity> partyFlagsForNotes(List<CasePartyFlagEntity> flags) {
        return flags.stream()
            .filter(this::isOtherPartyFlag)
            .filter(flag -> hasText(flag.getFlagRefData().getFlagName()))
            .sorted(Comparator.comparing(flag -> flag.getFlagRefData().getFlagCode()))
            .toList();
    }

    private boolean isOtherPartyFlag(CasePartyFlagEntity flag) {
        if (!hasCodePrefix(flag, "PF")) {
            return false;
        }
        String flagCode = flag.getFlagRefData().getFlagCode();
        return !VULNERABLE_USER.equals(flagCode) && !SPOKEN_LANGUAGE_INTERPRETER.equals(flagCode);
    }

    private boolean hasCodePrefix(CasePartyFlagEntity flag, String prefix) {
        FlagRefDataEntity refData = flag.getFlagRefData();
        return refData != null
            && refData.getFlagCode() != null
            && refData.getFlagCode().startsWith(prefix)
            && ACTIVE.equals(flag.getDefaultStatus());
    }

    private List<String> reasonableAdjustmentCodes(List<CasePartyFlagEntity> flags) {
        if (flags == null) {
            return List.of();
        }
        return flags.stream()
            .filter(this::isReasonableAdjustmentCode)
            .map(flag -> flag.getFlagRefData().getFlagCode())
            .sorted()
            .toList();
    }

    private boolean isReasonableAdjustmentCode(CasePartyFlagEntity flag) {
        if (!hasCodePrefix(flag, "RA") && !hasCodePrefix(flag, "SM")) {
            return false;
        }
        if (SIGN_LANGUAGE_INTERPRETER.equals(flag.getFlagRefData().getFlagCode())) {
            return false;
        }
        return Boolean.TRUE.equals(flag.getFlagRefData().getHearingRelevant())
            && !hasText(flag.getFlagComment());
    }

    private CasePartyFlagEntity activeVulnerableUser(List<CasePartyFlagEntity> flags) {
        if (flags == null) {
            return null;
        }
        return flags.stream()
            .filter(this::isActiveVulnerableUser)
            .findFirst()
            .orElse(null);
    }

    private boolean isActiveVulnerableUser(CasePartyFlagEntity flag) {
        return flag.getFlagRefData() != null
            && VULNERABLE_USER.equals(flag.getFlagRefData().getFlagCode())
            && ACTIVE.equals(flag.getDefaultStatus());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
