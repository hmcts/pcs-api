package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ReasonsForPossessionTabDetails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReasonsForPossessionTabDetailsBuilder {

    private static final Pattern GROUND_REFERENCE_PATTERN =
        Pattern.compile("\\(ground ([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION_REFERENCE_PATTERN =
        Pattern.compile("\\(section ([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final String ANTISOCIAL_BEHAVIOUR = "Antisocial behaviour";
    private static final String SECTION_84A_CONDITION_1_PREFIX = "Condition 1";
    private static final String SECTION_84A_CONDITION_2_PREFIX = "Condition 2";
    private static final String SECTION_84A_CONDITION_3_PREFIX = "Condition 3";
    private static final String SECTION_84A_CONDITION_4_PREFIX = "Condition 4";
    private static final String SECTION_84A_CONDITION_5_PREFIX = "Condition 5";
    private static final String BREACH_OF_THE_TENANCY = "Breach of the tenancy";
    private static final String ABSOLUTE_GROUNDS = "Absolute grounds";
    private static final String OTHER = "Other";
    private static final String OTHER_GROUNDS = "Other grounds";
    private static final String NO_GROUNDS = "No grounds";
    private static final String PARAGRAPH_25B_2_SCHEDULE_12 = "paragraph 25B(2) of Schedule 12";
    private static final String GROUND_1 = "1";
    private static final String GROUND_2 = "2";
    private static final String GROUND_2A = "2A";
    private static final String GROUND_2ZA = "2ZA";
    private static final String GROUND_3 = "3";
    private static final String GROUND_4 = "4";
    private static final String GROUND_5 = "5";
    private static final String GROUND_6 = "6";
    private static final String GROUND_7 = "7";
    private static final String GROUND_7A = "7A";
    private static final String GROUND_7B = "7B";
    private static final String GROUND_8 = "8";
    private static final String GROUND_9 = "9";
    private static final String GROUND_10 = "10";
    private static final String GROUND_10A = "10A";
    private static final String GROUND_11 = "11";
    private static final String GROUND_12 = "12";
    private static final String GROUND_13 = "13";
    private static final String GROUND_14 = "14";
    private static final String GROUND_14A = "14A";
    private static final String GROUND_14ZA = "14ZA";
    private static final String GROUND_15 = "15";
    private static final String GROUND_15A = "15A";
    private static final String GROUND_16 = "16";
    private static final String GROUND_17 = "17";
    private static final String GROUND_A = "A";
    private static final String GROUND_B = "B";
    private static final String GROUND_C = "C";
    private static final String GROUND_D = "D";
    private static final String GROUND_E = "E";
    private static final String GROUND_F = "F";
    private static final String GROUND_G = "G";
    private static final String GROUND_H = "H";
    private static final String GROUND_I = "I";
    private static final String SECTION_157 = "157";
    private static final String SECTION_170 = "170";
    private static final String SECTION_178 = "178";
    private static final String SECTION_181 = "181";
    private static final String SECTION_186 = "186";
    private static final String SECTION_187 = "187";
    private static final String SECTION_191 = "191";
    private static final String SECTION_199 = "199";

    public ReasonsForPossessionTabDetails buildReasonsForPossessionFromGroundSummaries(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getClaimGroundSummaries())) {
            return null;
        }

        ReasonsForPossessionTabDetails reasonsForPossession = ReasonsForPossessionTabDetails.builder().build();
        boolean hasReason = false;

        for (ListValue<ClaimGroundSummary> listValue : pcsCase.getClaimGroundSummaries()) {
            ClaimGroundSummary summary = listValue.getValue();
            if (summary == null || !StringUtils.hasText(summary.getReason())) {
                continue;
            }

            setGroundReason(reasonsForPossession, summary.getLabel(), summary.getReason());
            hasReason = true;
        }

        return hasReason ? reasonsForPossession : null;
    }

    private void setGroundReason(ReasonsForPossessionTabDetails reasonsForPossession,
                                 String groundLabel,
                                 String reason) {
        Matcher groundMatcher = GROUND_REFERENCE_PATTERN.matcher(groundLabel);
        if (groundMatcher.find()) {
            setGroundNumberReason(reasonsForPossession, groundMatcher.group(1), reason);
            return;
        }

        Matcher sectionMatcher = SECTION_REFERENCE_PATTERN.matcher(groundLabel);
        if (sectionMatcher.find()) {
            setSectionReason(reasonsForPossession, sectionMatcher.group(1), reason);
            return;
        }

        if (groundLabel.startsWith(SECTION_84A_CONDITION_1_PREFIX)) {
            reasonsForPossession.setCondition1OfSection84A(reason);
        } else if (groundLabel.startsWith(SECTION_84A_CONDITION_2_PREFIX)) {
            reasonsForPossession.setCondition2OfSection84A(reason);
        } else if (groundLabel.startsWith(SECTION_84A_CONDITION_3_PREFIX)) {
            reasonsForPossession.setCondition3OfSection84A(reason);
        } else if (groundLabel.startsWith(SECTION_84A_CONDITION_4_PREFIX)) {
            reasonsForPossession.setCondition4OfSection84A(reason);
        } else if (groundLabel.startsWith(SECTION_84A_CONDITION_5_PREFIX)) {
            reasonsForPossession.setCondition5OfSection84A(reason);
        } else if (ANTISOCIAL_BEHAVIOUR.equals(groundLabel)) {
            reasonsForPossession.setAntisocialBehaviour(reason);
        } else if (BREACH_OF_THE_TENANCY.equals(groundLabel)) {
            reasonsForPossession.setBreachOfTheTenancy(reason);
        } else if (ABSOLUTE_GROUNDS.equals(groundLabel)) {
            reasonsForPossession.setAbsoluteGrounds(reason);
        } else if (OTHER.equals(groundLabel) || OTHER_GROUNDS.equals(groundLabel)) {
            reasonsForPossession.setOtherGrounds(reason);
        } else if (NO_GROUNDS.equals(groundLabel)) {
            reasonsForPossession.setNoGrounds(reason);
        } else if (groundLabel.contains(PARAGRAPH_25B_2_SCHEDULE_12)) {
            reasonsForPossession.setParagraph25B2Schedule12(reason);
        }
    }

    private void setGroundNumberReason(ReasonsForPossessionTabDetails reasonsForPossession,
                                       String ground,
                                       String reason) {
        switch (ground) {
            case GROUND_1 -> reasonsForPossession.setGround1(reason);
            case GROUND_2 -> reasonsForPossession.setGround2(reason);
            case GROUND_2A -> reasonsForPossession.setGround2A(reason);
            case GROUND_2ZA -> reasonsForPossession.setGround2ZA(reason);
            case GROUND_3 -> reasonsForPossession.setGround3(reason);
            case GROUND_4 -> reasonsForPossession.setGround4(reason);
            case GROUND_5 -> reasonsForPossession.setGround5(reason);
            case GROUND_6 -> reasonsForPossession.setGround6(reason);
            case GROUND_7 -> reasonsForPossession.setGround7(reason);
            case GROUND_7A -> reasonsForPossession.setGround7A(reason);
            case GROUND_7B -> reasonsForPossession.setGround7B(reason);
            case GROUND_8 -> reasonsForPossession.setGround8(reason);
            case GROUND_9 -> reasonsForPossession.setGround9(reason);
            case GROUND_10 -> reasonsForPossession.setGround10(reason);
            case GROUND_10A -> reasonsForPossession.setGround10A(reason);
            case GROUND_11 -> reasonsForPossession.setGround11(reason);
            case GROUND_12 -> reasonsForPossession.setGround12(reason);
            case GROUND_13 -> reasonsForPossession.setGround13(reason);
            case GROUND_14 -> reasonsForPossession.setGround14(reason);
            case GROUND_14A -> reasonsForPossession.setGround14A(reason);
            case GROUND_14ZA -> reasonsForPossession.setGround14ZA(reason);
            case GROUND_15 -> reasonsForPossession.setGround15(reason);
            case GROUND_15A -> reasonsForPossession.setGround15A(reason);
            case GROUND_16 -> reasonsForPossession.setGround16(reason);
            case GROUND_17 -> reasonsForPossession.setGround17(reason);
            case GROUND_A -> reasonsForPossession.setGroundA(reason);
            case GROUND_B -> reasonsForPossession.setGroundB(reason);
            case GROUND_C -> reasonsForPossession.setGroundC(reason);
            case GROUND_D -> reasonsForPossession.setGroundD(reason);
            case GROUND_E -> reasonsForPossession.setGroundE(reason);
            case GROUND_F -> reasonsForPossession.setGroundF(reason);
            case GROUND_G -> reasonsForPossession.setGroundG(reason);
            case GROUND_H -> reasonsForPossession.setGroundH(reason);
            case GROUND_I -> reasonsForPossession.setGroundI(reason);
            default -> {
            }
        }
    }

    private void setSectionReason(ReasonsForPossessionTabDetails reasonsForPossession,
                                  String section,
                                  String reason) {
        switch (section) {
            case SECTION_157 -> reasonsForPossession.setSection157(reason);
            case SECTION_170 -> reasonsForPossession.setSection170(reason);
            case SECTION_178 -> reasonsForPossession.setSection178(reason);
            case SECTION_181 -> reasonsForPossession.setSection181(reason);
            case SECTION_186 -> reasonsForPossession.setSection186(reason);
            case SECTION_187 -> reasonsForPossession.setSection187(reason);
            case SECTION_191 -> reasonsForPossession.setSection191(reason);
            case SECTION_199 -> reasonsForPossession.setSection199(reason);
            default -> {
            }
        }
    }
}
