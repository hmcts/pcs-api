package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseTabView;

import java.util.ArrayList;
import java.util.List;

@Component
public class NameAndAddressForEvictionPageHelper {

    public static final int MAX_UNKNOWN_DEFENDANTS = 10;
    public static final String PERSON_UNKNOWN_LABEL =
        "<div class=\"govuk-!-margin-bottom-2\">" + CaseTabView.NAME_UNKNOWN + "</div>";

    public void addUnknownDefendantLabels(
        FieldCollection.FieldCollectionBuilder<EnforcementOrder, State, ?> pageBuilder, String suffix) {
        for (int i = 0; i < MAX_UNKNOWN_DEFENDANTS; i++) {
            pageBuilder.label("nameAndAddressForEviction" + suffix + "-defendant-unknown-" + i,
                                         PERSON_UNKNOWN_LABEL, unknownDefendantShowCondition(i));
        }
    }

    public String unknownDefendantShowCondition(int position) {
        List<String> conditions = new ArrayList<>();
        for (int i = position; i < MAX_UNKNOWN_DEFENDANTS; i++) {
            conditions.add("unknownDefendantCount=\"" + (i + 1) + "\"");
        }
        return ShowConditions.or(conditions.toArray(new String[0]));
    }
}
