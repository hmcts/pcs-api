package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Placeholder page configuration for the Grounds for Possession section.
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
@AllArgsConstructor
@Component
@Slf4j
public class GroundsForPossessionOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundsForPossessionOptions", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("groundsForPossession=\"No\"")
            .label("groundsForPossessionOptions-information", """
                ---
                <p>You may have already given the defendants notice of your intention to begin possession proceedings.
                If you have, you should have written the grounds you’re making your claim under.
                You should select these grounds here and any extra grounds you’d like to add to your claim,
                if you need to.</p>""")
            .label("groundsForPossessionOptions-information-link",
                   "<p class=\"govuk-body\"><a href=\"javascript:void(0)\" "
                       + "class=\"govuk-link\">More information about possession grounds (opens in new tab)</a>.</p>")
            .optional(PCSCase::getMandatoryGroundsOptionsList)
            .optional(PCSCase::getDiscretionaryGroundsOptionsList);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase pcsCases = details.getData();

        if (pcsCases.getMandatoryGroundsOptionsList().isEmpty() && pcsCases.getDiscretionaryGroundsOptionsList().isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .errors(List.of("Please select at least one ground"))
                    .build();
        }

        Set<NoRentArrearsMandatoryGrounds> selectedNoRentArrearsMandatoryGrounds = new HashSet<>
                (pcsCases.getMandatoryGroundsOptionsList());


        Set<NoRentArrearsDiscretionaryGrounds> selectedNoRentArrearsDiscretionaryGrounds = new HashSet<>
                (pcsCases.getDiscretionaryGroundsOptionsList());


        pcsCases.setSelectedNoRentArrearsMandatoryGrounds(selectedNoRentArrearsMandatoryGrounds);
        pcsCases.setSelectedNoRentArrearsDiscretionaryGrounds(selectedNoRentArrearsDiscretionaryGrounds);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(pcsCases)
            .build();
    }
}
