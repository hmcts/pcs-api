package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class EvictionDatePage implements CcdPageConfiguration, CcdPage {

    static final String EVICTION_DATE_CONTENT = """
        <p class="govuk-body">The bailiff has given you an eviction date of
        <strong>${formattedEvictionDate}</strong>.</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("The eviction date")
            .label(pageKey + "-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .readonly(EnforcementOrder::getFormattedEvictionDate, NEVER_SHOW, true)
            // Set by the start callback: the warrant risk pages that follow only show in the warrant flow.
            .readonly(EnforcementOrder::getChooseEnforcementType, NEVER_SHOW, true)
            .label(pageKey + "-eviction-date", EVICTION_DATE_CONTENT)
            .mandatory(EnforcementOrder::getClaimantCanAttendEviction)
            .done();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

}
