package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.DiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.MandatoryGrounds;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


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
                   "<p class=\"govuk-body\"><a href=\"javascript:void(0)\" " +
                       "class=\"govuk-link\">More information about possession grounds (opens in new tab)</a>.</p>")
            .optional(PCSCase::getMandatoryGroundsOptionsList)
            .optional(PCSCase::getDiscretionaryGroundsOptionsList);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase pcsCases = details.getData();
        Set<DiscretionaryGrounds> selectedDiscretionaryOptions = pcsCases.getDiscretionaryGroundsOptionsList()
            .getValue()
            .stream()
            .map(i -> DiscretionaryGrounds.fromLabel(i.getLabel()))
            .collect(Collectors.toSet());

        Set<MandatoryGrounds> selectedMandatoryOptions = pcsCases.getMandatoryGroundsOptionsList()
            .getValue()
            .stream()
            .map(i -> MandatoryGrounds.fromLabel(i.getLabel()))
            .collect(Collectors.toSet());

        if(selectedMandatoryOptions.isEmpty() && selectedDiscretionaryOptions.isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of("Please select at least one ground"))
                .build();
        }

        pcsCases.setSelectedMandatoryGrounds(selectedMandatoryOptions);
        pcsCases.setSelectedDiscretionaryGrounds(selectedDiscretionaryOptions);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(pcsCases)
            .build();
    }
}
