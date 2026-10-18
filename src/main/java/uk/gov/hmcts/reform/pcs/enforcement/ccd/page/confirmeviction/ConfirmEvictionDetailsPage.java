package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

public class ConfirmEvictionDetailsPage implements CcdPageConfiguration, CcdPage {

    public static final String CONFIRM_EVICTION_DETAILS_CONTENT = """
                    <p class="govuk-body">
                        The bailiff has arranged a date for the eviction. They need you to confirm if you can
                        attend on that date.
                    </p>
                    <p class="govuk-body">
                        The bailiff needs this information to carry out the eviction safely. If you do not provide it,
                        they may not be able to complete the eviction.
                    </p>
                    <h2 class="govuk-heading-m">What we will ask you</h2>
                    <p class="govuk-body govuk-!-margin-bottom-0">We will ask:</p>
                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19">if you can attend the eviction on the date the bailiff
                        has arranged</li>
                        <li class="govuk-!-font-size-19">if anyone living at the property poses a risk to the
                        bailiff</li>
                        <li class="govuk-!-font-size-19">if anyone living at the property is vulnerable</li>
                        <li class="govuk-!-font-size-19">how the bailiff can get into the property</li>
                        <li class="govuk-!-font-size-19">if there is anything else that could help with the
                        eviction</li>
                    </ul>
                    <p class="govuk-body">
                        We will show you the answers you gave when you applied for the warrant. Check them and change
                        any that are no longer correct.
                    </p>
                    """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey)
            .pageLabel("Confirm the eviction details")
            .label(pageKey + "-line-separator", "---")
            .label(pageKey + "-content", CONFIRM_EVICTION_DETAILS_CONTENT);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }
}
