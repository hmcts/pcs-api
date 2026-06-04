package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GroundsBuilder {

    private static final String ANTISOCIAL_BEHAVIOUR = "Antisocial behaviour";
    private static final String ESTATE_MANAGEMENT_GROUNDS = "Estate management grounds (section 160)";
    private static final Pattern SECTION_84A_CONDITION_PATTERN =
        Pattern.compile("^Condition ([1-5]) of Section 84A of the Housing Act 1985$");
    private static final Map<String, EstateManagementGroundsWales> ESTATE_MANAGEMENT_GROUND_BY_LABEL =
        Arrays.stream(EstateManagementGroundsWales.values())
            .collect(Collectors.toMap(EstateManagementGroundsWales::getLabel, Function.identity()));

    public String getGrounds(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getClaimGroundSummaries())) {
            return null;
        }

        List<String> grounds = new ArrayList<>(pcsCase.getClaimGroundSummaries().stream()
                                                   .map(ListValue::getValue)
                                                   .map(ClaimGroundSummary::getLabel)
                                                   .toList());

        groupSection84AConditions(grounds);
        groupEstateManagementGrounds(grounds);

        return grounds.stream()
            .reduce((firstGround, secondGround) -> firstGround + "\n" + secondGround)
            .orElse(null);
    }

    private void groupSection84AConditions(List<String> grounds) {
        List<String> section84AConditions = grounds.stream()
            .filter(this::isSection84ACondition)
            .sorted(this::compareSection84AConditions)
            .toList();

        if (section84AConditions.isEmpty()) {
            return;
        }

        int antisocialIndex = grounds.indexOf(ANTISOCIAL_BEHAVIOUR);
        int groupIndex = antisocialIndex >= 0 ? antisocialIndex : grounds.indexOf(section84AConditions.getFirst());
        grounds.set(groupIndex, ANTISOCIAL_BEHAVIOUR + ": " + String.join(", ", section84AConditions));
        grounds.removeAll(section84AConditions);
    }

    private void groupEstateManagementGrounds(List<String> grounds) {
        List<String> estateManagementGrounds = grounds.stream()
            .filter(this::isEstateManagementGround)
            .sorted(this::compareEstateManagementGrounds)
            .toList();

        if (estateManagementGrounds.isEmpty()) {
            return;
        }

        int estateManagementIndex = grounds.indexOf(ESTATE_MANAGEMENT_GROUNDS);
        int groupIndex = estateManagementIndex >= 0 ? estateManagementIndex : grounds.indexOf(
            estateManagementGrounds.getFirst());
        grounds.set(groupIndex, ESTATE_MANAGEMENT_GROUNDS + ": " + String.join(", ", estateManagementGrounds));
        grounds.removeAll(estateManagementGrounds);
    }

    private boolean isSection84ACondition(String label) {
        return SECTION_84A_CONDITION_PATTERN.matcher(label).matches();
    }

    private boolean isEstateManagementGround(String label) {
        return ESTATE_MANAGEMENT_GROUND_BY_LABEL.containsKey(label);
    }

    private int compareSection84AConditions(String firstCondition, String secondCondition) {
        return Integer.compare(
            getSection84AConditionNumber(firstCondition),
            getSection84AConditionNumber(secondCondition)
        );
    }

    private int getSection84AConditionNumber(String label) {
        Matcher matcher = SECTION_84A_CONDITION_PATTERN.matcher(label);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private int compareEstateManagementGrounds(String firstGround, String secondGround) {
        return Integer.compare(
            getEstateManagementGroundNumber(firstGround),
            getEstateManagementGroundNumber(secondGround)
        );
    }

    private int getEstateManagementGroundNumber(String label) {
        EstateManagementGroundsWales ground = ESTATE_MANAGEMENT_GROUND_BY_LABEL.get(label);
        return ground == null ? 0 : ground.getRank();
    }
}
