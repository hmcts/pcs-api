package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class StartSetAside implements CcdPageConfiguration {

    private static final String PLACEHOLDER = """
      <div class="govuk-notification-banner" role="region" aria-labelledby="placeholder-banner">
        <div class="govuk-notification-banner__content">
          <p class="govuk-notification-banner__heading" id="placeholder-banner">
            Placeholder
          </p>
        </div>
      </div>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("startSetAside")
            .pageLabel("Ask the court to set aside (cancel) an order on behalf of a defendant")
            .showCondition(fieldEquals("xui_genapp_ApplicationType", GenAppType.SET_ASIDE))
            .label("startSetAside-lineSeparator", "---")
            .label("startSetAside-placeholder", PLACEHOLDER);
    }

}
