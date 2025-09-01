package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.ReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Placeholder page configuration for the Grounds for Possession section.
 * Full implementation will be done in another ticket - responses not captured at the moment.
 */
@AllArgsConstructor
@Component
@Slf4j
public class        GroundsForPossessionOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("groundsForPossessionOptions", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            //.label("groundsForPossessionOptions-lineSeparator", "---")
            .label("groundsForPossessionOptions-information", """
                ---
                <p>You may have already given the defendants notice of your intention to begin possession proceedings.
                If you have, you should have written the grounds you’re making your claim under.
                You should select these grounds here and any extra grounds you’d like to add to your claim, if you need to.</p>""")
            .label("groundsForPossessionOptions-information-link",
                   "<p class=\"govuk-body\"><a href=\"javascript:void(0)\" class=\"govuk-link\">More information about possession grounds (opens in new tab)</a>.</p>")
         //   .label("groundsForPossessionOptions-mandatory-grounds", "<h3 class=\"govuk-heading-m\">Mandatory grounds</h3>")
            .optional(PCSCase::getMandatoryGroundsOptionsList)
           // .label("groundsForPossessionOptions-Discretionary-grounds", "<h3 class=\"govuk-heading-m\">Discretionary grounds</h3>")
            .optional(PCSCase::getDiscretionaryGroundsOptionsList)
        ;

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {

        PCSCase pcsCases = details.getData();
        System.out.println(pcsCases.getMandatoryGroundsOptionsList().getListItems().size());
        System.out.println(pcsCases.getMandatoryGroundsOptionsList().getListItems());
        System.out.println("testing selected value"+pcsCases.getMandatoryGroundsOptionsList().getValue());


        List<ReasonForGrounds> mandatoryOptionsSelected = new ArrayList<>();

        pcsCases.getMandatoryGroundsOptionsList().getValue().forEach(i -> {
            mandatoryOptionsSelected.add(new ReasonForGrounds(i.getLabel(), ""));
        });

        System.out.println("this is the inserted "+ mandatoryOptionsSelected);
          //  DynamicListElement::getLabel).toList();
//        pcsCases.getGroundsSelectedList().addAll(mandatoryOptionsSelected);
//

       // pcsCases.setSelectedPossessionGrounds(mandatoryOptionsSelected);

       // System.out.println("manadtory selected "+mandatoryOptionsSelected);
        System.out.println();
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(pcsCases)
            .build();
    }
}
