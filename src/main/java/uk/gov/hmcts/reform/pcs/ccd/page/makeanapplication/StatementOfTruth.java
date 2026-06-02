package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

@Slf4j
@AllArgsConstructor
public class StatementOfTruth implements CcdPageConfiguration {

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
            .page("statementOfTruth")
            .pageLabel("Statement of truth")
            .label("statementOfTruth-lineSeparator", "---")
            .label("statementOfTruth-placeholder", PLACEHOLDER);
    }

}
