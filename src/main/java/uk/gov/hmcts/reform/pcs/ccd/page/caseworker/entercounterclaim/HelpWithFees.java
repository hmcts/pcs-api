package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Component
public class HelpWithFees implements CcdPageConfiguration {

    private static final String CLAIM_TYPE_FIELD = "enter_counterclaim_ClaimTypeOption";

    private static final String SOMETHING_ELSE_SELECTED =
        fieldEquals(CLAIM_TYPE_FIELD, CounterClaimType.SOMETHING_ELSE);

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("helpWithFees")
            .pageLabel("Help with fees")
            .showCondition(SOMETHING_ELSE_SELECTED)
            .label("helpWithFees-placeholder", "Placeholder - to be implemented");
    }
}
