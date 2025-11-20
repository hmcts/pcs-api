package uk.gov.hmcts.reform.pcs.ccd.page.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;

import java.util.List;
import java.util.stream.Collectors;

public class EnforcementApplicationPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
                .page("enforcementApplication", this::midEvent)
                .pageLabel("Your application")
                .label("enforcementApplication-content", "---")
                .complex(PCSCase::getEnforcementOrder)
                .mandatory(EnforcementOrder::getSelectEnforcementType)
                .label("enforcementApplication-clarification",
                    """
                    <details class="govuk-details">
                        <summary class="govuk-details__summary">
                            <span class="govuk-details__summary-text">
                                I do not know if I need a writ or a warrant
                            </span>
                        </summary>
                        <div class="govuk-details__text">
                            ...
                        </div>
                    </details>
                    """);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase pcsCase = before.getData();
        PCSCase data = details.getData();
        setFormattedDefendantNames(pcsCase.getAllDefendants(), data);
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data).build();
    }

    private void setFormattedDefendantNames(List<ListValue<DefendantDetails>> defendants, PCSCase pcsCase) {
        if (defendants != null || !defendants.isEmpty()) {
            pcsCase.setAllDefendants(defendants);
            pcsCase.setFormattedDefendantNames(defendants.stream()
                .map(defendant -> ""
                    + defendant.getValue().getFirstName() + " " + defendant.getValue().getLastName()
                    + "<br>")
                .collect(Collectors.joining("\n")));
        }
    }



}
