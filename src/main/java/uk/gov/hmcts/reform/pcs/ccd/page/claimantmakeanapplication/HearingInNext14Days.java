package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class HearingInNext14Days implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">The ‘other parties’ are the other people or organisations named on the claim form.
        For example a tenant, landlord, housing association, or mortgage lender.</p>
        <p class="govuk-body">Every other party will need to agree to let you make this application, if they
        don’t agree, you may need to pay a higher fee.</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("hearingInNext14Days")
            .pageLabel("Is the court hearing in the next 14 days?")
            .showCondition(fieldEquals("xui_genapp_ApplicationType", GenAppType.ADJOURN))
            .label("cma-hearingInNext14Days-lineSeparator", "---")
            .label("cma-hearingInNext14Days-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getWithin14Days, null, null,
                       "Is the court hearing in the next 14 days?",
                       "This will affect the fee you will pay. You will not need to pay a fee if "
                           + "your court hearing is (at least) 14 days away and all the other parties have agreed "
                           + "to it.")
            .done();
    }

}
