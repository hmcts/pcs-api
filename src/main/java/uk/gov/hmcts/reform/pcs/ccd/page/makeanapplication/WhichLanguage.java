package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

@Slf4j
@AllArgsConstructor
public class WhichLanguage implements CcdPageConfiguration {

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
            .page("whichLanguage", this::midEvent)
            .pageLabel("Which language did you use to complete this service?")
            .label("whichLanguage-lineSeparator", "---")
            .label("whichLanguage-placeholder", PLACEHOLDER)
            .readonly(PCSCase::getCurrentRepresentedPartyId, NEVER_SHOW, true);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase caseData = details.getData();

        if (caseData.getCurrentRepresentedPartyId() == null) {
            DynamicListElement selectedPartyElement = caseData.getRepresentedPartyNames().getValue();
            caseData.setCurrentRepresentedPartyId(selectedPartyElement.getCode().toString());
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }


}
