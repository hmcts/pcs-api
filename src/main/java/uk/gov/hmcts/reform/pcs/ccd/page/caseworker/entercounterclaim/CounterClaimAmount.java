package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Component
public class CounterClaimAmount implements CcdPageConfiguration {

    private static final String CLAIM_TYPE_FIELD = "enter_counterclaim_ClaimTypeOption";

    private static final String AMOUNT_APPLIES = ShowConditions.or(
        fieldEquals(CLAIM_TYPE_FIELD, CounterClaimType.PAYMENT_OR_COMPENSATION),
        fieldEquals(CLAIM_TYPE_FIELD, CounterClaimType.BOTH)
    );

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("counterClaimAmount")
            .pageLabel("Counterclaim amount")
            .showCondition(AMOUNT_APPLIES)
            .label("counterClaimAmount-placeholder", "Placeholder - to be implemented");
    }
}
