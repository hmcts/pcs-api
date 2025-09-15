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

import java.util.List;


@AllArgsConstructor
@Component
@Slf4j
public class NoRentArrearsGroundsForPossessionOptions implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("noRentArrearsGroundsForPossessionOptions", this::midEvent)
            .pageLabel("What are your grounds for possession?")
            .showCondition("groundsForPossession=\"No\" AND typeOfTenancyLicence=\"ASSURED_TENANCY\"")
            .label(
                "NoRentArrearsGroundsForPossessionOptions-information", """
                    ---
                    <p>You may have already given the defendants notice of your intention to begin possession
                    proceedings. If you have, you should have written the grounds you’re making your claim under.
                    You should select these grounds here and any extra grounds you’d like to add to your claim,
                    if you need to.</p>"""
            )
            .optional(PCSCase::getNoRentArrearsMandatoryGroundsOptions)
            .optional(PCSCase::getNoRentArrearsDiscretionaryGroundsOptions);

    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        if (caseData.getNoRentArrearsMandatoryGroundsOptions().isEmpty()
            &&
            caseData.getNoRentArrearsDiscretionaryGroundsOptions().isEmpty()) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .errors(List.of("Please select at least one ground"))
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }
}
