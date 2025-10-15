package uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.*;

import java.util.List;
import java.util.Set;

@Slf4j
public class WelshGroundsForPossession
    implements CcdPageConfiguration {

    @Override

    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
        .page("welshGfpGroundsForPossession", this::midEvent)
        .pageLabel("What are your grounds for possession?")
        .showCondition("legislativeCountry=\"Wales\"")

        .label(
            "WelshGfpGroundsForPossession",
            """
              ---
              <p class="govuk-body" tabindex="0">
              You may have already given the defendants notice of your
              intention to begin possession proceedings. If you have,
              you should have written the grounds you're making your
              claim under. Select those grounds here and add any extra
              grounds if needed.
            </p>
            <p class="govuk-body">
              <a class="govuk-link" target="_blank"
                 rel="noreferrer noopener"
                 href="https://www.gov.uk/">
                More information about possession grounds
                (opens in new tab).
              </a>
            </p>
            """
        )

        // -------- Discretionary --------
        .optional(PCSCase::getWelshDiscretionaryGrounds)
        .optional(
            PCSCase::getWelshEstateManagementGrounds,
            "welshDiscretionaryGrounds CONTAINS "
                + "\"ESTATE_MANAGEMENT_GROUNDS_SECTION_160\""
        )
       .optional(PCSCase::getWelshMandatoryGrounds);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                   CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        log.info("WelshGfpGroundsForPossession page accessed. Legislative Country: {}", caseData.getLegislativeCountry());

        Set<WelshDiscretionaryGround> discretionaryGrounds = caseData.getWelshDiscretionaryGrounds();
        Set<WelshMandatoryGround> mandatoryGrounds = caseData.getWelshMandatoryGrounds();

        if ((discretionaryGrounds == null || discretionaryGrounds.isEmpty())
            && (mandatoryGrounds == null || mandatoryGrounds.isEmpty())) {
            log.warn("WelshGfpGroundsForPossession validation failed: No grounds selected");
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of("Please select at least one ground"))
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
