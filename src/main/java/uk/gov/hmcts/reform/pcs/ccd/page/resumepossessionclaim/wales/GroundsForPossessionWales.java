package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class GroundsForPossessionWales
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
                  You may have already given the defendants notice of your intention to begin possession proceedings.
                  If you have, you should have written the grounds you’re making your claim under. You should select
                  these grounds here and select any extra grounds you’d like to add to your claim, if you need to.
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
            .mandatory(PCSCase::getDiscretionaryGroundsWales)
            .optional(
                PCSCase::getEstateManagementGroundsWales,
                "discretionaryGroundsWales CONTAINS "
                    + "\"ESTATE_MANAGEMENT_GROUNDS_SECTION_160\""
            )
            .optional(PCSCase::getMandatoryGroundsWales);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(
        CaseDetails<PCSCase, State> details,
        CaseDetails<PCSCase, State> before) {

        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();

        Set<DiscretionaryGroundWales> disc = data.getDiscretionaryGroundsWales();

        // Rule 1: at least one discretionary checkbox
        if (disc == null || disc.isEmpty()) {
            errors.add("Select at least one discretionary ground.");
        }

        // Rule 2: if estate mgmt parent ticked, require sub-selection
        if (disc != null
            && disc.contains(
                DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_SECTION_160)) {

            var estate = data.getEstateManagementGroundsWales();
            if (estate == null || estate.isEmpty()) {
                errors.add(
                    "Select at least one estate management ground when "
                        + "'Estate management grounds (section 160)' is selected."
                );
            }
        }

        if (!errors.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(data)
                .errors(errors)
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .build();
    }
}
