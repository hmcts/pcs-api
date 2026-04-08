package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.confirmeviction;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;

public class ConfirmEvictionDetailsPage implements CcdPageConfiguration, CcdPage {

    public static final String CONFIRM_EVICTION_DETAILS_CONTENT  = """
                    <p class="govuk-body">
                        The bailiff has arranged a date for the eviction and they need you to confirm if you are
                        available.
                    </p>
                    <p class="govuk-body">
                        They will also ask you to confirm if the person being evicted poses any risk.
                    </p>
                    <p class="govuk-body">
                        The bailiff needs this information to carry out the eviction safely. If you do not provide it,
                        they may not be able to complete the eviction.
                    </p>
                    <p class="govuk-body govuk-!-font-weight-bold govuk-!-font-size-24">What you’ll need
                    </p>
                    <p class="govuk-body govuk-!-margin-bottom-0">You’ll need to know:</p>
                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19">who will attend the eviction (you, or someone else)</li>
                        <li class="govuk-!-font-size-19">if you (or they) can attend the eviction on the date suggested
                        by the bailiff</li>
                    </ul>
                    <p class="govuk-body govuk-!-margin-bottom-0">We will also ask you to:</p>
                    <ul class="govuk-list govuk-list--bullet">
                        <li class="govuk-!-font-size-19">describe the person who will be evicted</li>
                        <li class="govuk-!-font-size-19">tell us how to access the property</li>
                        <li class="govuk-!-font-size-19">book a locksmith (this is to make sure that the person being
                        evicted cannot return to the property)</li>
                    </ul>
                    <p class="govuk-body">
                       Once you have confirmed the eviction date, we’ll send you an email reminding you to book a
                       locksmith.
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
