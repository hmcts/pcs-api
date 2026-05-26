package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class HearingInNext14Days implements CcdPageConfiguration {

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
            .page("hearingInNext14Days")
            .pageLabel("Is the defendant’s court hearing in the next 14 days?")
            .showCondition(fieldEquals("xui_genapp_ApplicationType", GenAppType.ADJOURN))
            .label("hearingInNext14Days-lineSeparator", "---")
            .label("hearingInNext14Days-placeholder", PLACEHOLDER);
    }

}
