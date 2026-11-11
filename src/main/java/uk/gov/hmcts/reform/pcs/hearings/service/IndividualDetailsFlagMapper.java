package uk.gov.hmcts.reform.pcs.hearings.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;

/**
 * Fills the hearing fields that come from one person's flags.
 */
@Slf4j
@Component
public class IndividualDetailsFlagMapper {

    // HMC rejects the whole hearing request if otherReasonableAdjustmentDetails is longer than this
    static final int OTHER_DETAILS_MAX_LENGTH = 3000;
    static final String TRUNCATION_MARKER = "...";

    static final String ACTIVE = "Active";
    static final String VULNERABLE_USER = "PF0002";
    static final String SPOKEN_LANGUAGE_INTERPRETER = "PF0015";
    static final String SIGN_LANGUAGE_INTERPRETER = "RA0042";

    private static final Comparator<CasePartyFlagEntity> BY_FLAG_CODE =
        Comparator.comparing(IndividualDetailsFlagMapper::code);

    /**
     * Flag categories in the order they appear in otherReasonableAdjustmentDetails.
     */
    enum FlagGroup {
        REASONABLE_ADJUSTMENTS("RA", "Reasonable Adjustments"),
        SPECIAL_MEASURES("SM", "Special Measures"),
        PARTY_FLAGS("PF", "Party Flags");

        private final String codePrefix;
        private final String heading;

        FlagGroup(String codePrefix, String heading) {
            this.codePrefix = codePrefix;
            this.heading = heading;
        }

        // Interpreter flags are reported with party flags, even the sign language one which has an RA code
        static Optional<FlagGroup> of(CasePartyFlagEntity flag) {
            if (isInterpreter(flag)) {
                return Optional.of(PARTY_FLAGS);
            }
            return Arrays.stream(values())
                .filter(group -> code(flag).startsWith(group.codePrefix))
                .findFirst();
        }
    }

    private static final Set<FlagGroup> ADJUSTMENT_GROUPS =
        EnumSet.of(FlagGroup.REASONABLE_ADJUSTMENTS, FlagGroup.SPECIAL_MEASURES);

    public IndividualDetails map(List<CasePartyFlagEntity> flags) {
        List<CasePartyFlagEntity> activeFlags = activeFlags(flags);
        IndividualDetails details = new IndividualDetails();

        Optional<CasePartyFlagEntity> vulnerableUser = activeFlags.stream()
            .filter(flag -> hasCode(flag, VULNERABLE_USER))
            .findFirst();
        details.setVulnerableFlag(vulnerableUser.isPresent());
        vulnerableUser
            .map(CasePartyFlagEntity::getFlagComment)
            .filter(IndividualDetailsFlagMapper::hasText)
            .ifPresent(details::setVulnerabilityDetails);

        details.setInterpreterLanguage(interpreterLanguage(activeFlags));

        List<String> adjustmentCodes = reasonableAdjustmentCodes(activeFlags);
        details.setReasonableAdjustments(adjustmentCodes.isEmpty() ? null : adjustmentCodes);
        details.setOtherReasonableAdjustmentDetails(otherReasonableAdjustmentDetails(activeFlags));
        return details;
    }

    private List<CasePartyFlagEntity> activeFlags(List<CasePartyFlagEntity> flags) {
        if (flags == null) {
            return List.of();
        }
        return flags.stream()
            .filter(flag -> flag.getFlagRefData() != null && flag.getFlagRefData().getFlagCode() != null)
            .filter(flag -> ACTIVE.equals(flag.getDefaultStatus()))
            .toList();
    }

    private String interpreterLanguage(List<CasePartyFlagEntity> activeFlags) {
        return listedLanguageKey(activeFlags, SPOKEN_LANGUAGE_INTERPRETER)
            .or(() -> listedLanguageKey(activeFlags, SIGN_LANGUAGE_INTERPRETER))
            .orElse(null);
    }

    private Optional<String> listedLanguageKey(List<CasePartyFlagEntity> activeFlags, String flagCode) {
        return activeFlags.stream()
            .filter(flag -> hasCode(flag, flagCode))
            .filter(IndividualDetailsFlagMapper::isLanguageFromList)
            .filter(flag -> !hasText(flag.getFlagComment()))
            .map(CasePartyFlagEntity::getSubTypeKey)
            .findFirst();
    }

    private List<String> reasonableAdjustmentCodes(List<CasePartyFlagEntity> activeFlags) {
        return activeFlags.stream()
            .filter(flag -> FlagGroup.of(flag).filter(ADJUSTMENT_GROUPS::contains).isPresent())
            .filter(flag -> Boolean.TRUE.equals(flag.getFlagRefData().getHearingRelevant()))
            .filter(flag -> !hasText(flag.getFlagComment()))
            .sorted(BY_FLAG_CODE)
            .map(IndividualDetailsFlagMapper::code)
            .toList();
    }

    private String otherReasonableAdjustmentDetails(List<CasePartyFlagEntity> activeFlags) {
        String details = Arrays.stream(FlagGroup.values())
            .map(group -> groupLine(group, activeFlags))
            .flatMap(Optional::stream)
            .collect(Collectors.joining("\n"));
        if (details.isEmpty()) {
            return null;
        }
        return truncateToHmcLimit(details);
    }

    private String truncateToHmcLimit(String details) {
        if (details.length() <= OTHER_DETAILS_MAX_LENGTH) {
            return details;
        }
        log.warn("otherReasonableAdjustmentDetails truncated from {} to {} characters for HMC",
                 details.length(), OTHER_DETAILS_MAX_LENGTH);
        return details.substring(0, OTHER_DETAILS_MAX_LENGTH - TRUNCATION_MARKER.length()) + TRUNCATION_MARKER;
    }

    private Optional<String> groupLine(FlagGroup group, List<CasePartyFlagEntity> activeFlags) {
        List<String> entries = activeFlags.stream()
            .filter(flag -> FlagGroup.of(flag).filter(group::equals).isPresent())
            .filter(flag -> belongsInOtherDetails(group, flag))
            .filter(flag -> hasText(flag.getFlagRefData().getFlagName()))
            .sorted(BY_FLAG_CODE)
            .map(this::entry)
            .toList();
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(group.heading + ": " + String.join(", ", entries) + ".");
    }

    private boolean belongsInOtherDetails(FlagGroup group, CasePartyFlagEntity flag) {
        return switch (group) {
            case REASONABLE_ADJUSTMENTS, SPECIAL_MEASURES -> hasText(flag.getFlagComment());
            case PARTY_FLAGS -> isInterpreter(flag)
                ? !isLanguageFromList(flag) || hasText(flag.getFlagComment())
                : !hasCode(flag, VULNERABLE_USER);
        };
    }

    private String entry(CasePartyFlagEntity flag) {
        List<String> extras = new ArrayList<>();
        if (isInterpreter(flag) && hasText(flag.getSubTypeValue())) {
            String label = isLanguageFromList(flag) ? "Language selected: " : "Language entered: ";
            extras.add(label + flag.getSubTypeValue());
        }
        if (hasText(flag.getFlagComment())) {
            extras.add("Comment: " + flag.getFlagComment());
        }
        String name = flag.getFlagRefData().getFlagName();
        return extras.isEmpty() ? name : name + " (" + String.join(", ", extras) + ")";
    }

    private static String code(CasePartyFlagEntity flag) {
        return flag.getFlagRefData().getFlagCode();
    }

    private static boolean isInterpreter(CasePartyFlagEntity flag) {
        return hasCode(flag, SPOKEN_LANGUAGE_INTERPRETER) || hasCode(flag, SIGN_LANGUAGE_INTERPRETER);
    }

    // A language picked from the reference data list has a key; a manually entered one does not
    private static boolean isLanguageFromList(CasePartyFlagEntity flag) {
        return hasText(flag.getSubTypeKey());
    }

    private static boolean hasCode(CasePartyFlagEntity flag, String flagCode) {
        return flagCode.equals(code(flag));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
