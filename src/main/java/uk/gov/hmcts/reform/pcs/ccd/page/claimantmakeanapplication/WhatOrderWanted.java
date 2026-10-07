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
public class WhatOrderWanted implements CcdPageConfiguration {

    private static final String ADJOURN_EXAMPLES = """
        <p class="govuk-body govuk-!-margin-bottom-1">For example, tell the court you want an order adjourning
        the hearing because:</p>

        <ul class="govuk-list govuk-list--bullet">
        <li class="govuk-!-font-size-19">you are unable to attend the hearing due a pre-existing
        medical appointment</li>
        <li class="govuk-!-font-size-19">you are attending a funeral</li>
        </ul>
        """;

    private static final String SET_ASIDE_INFO = """
        <p class="govuk-body">Tell the court why you want an order to set aside a previous decision.</p>
        """;

    private static final String SOMETHING_ELSE_EXAMPLES = """
        <p class="govuk-body govuk-!-margin-bottom-1">For example, tell the court:</p>

        <ul class="govuk-list govuk-list--bullet">
        <li class="govuk-!-font-size-19">why you want to add a party to the case</li>
        <li class="govuk-!-font-size-19">why you should not be sanctioned (punished)</li>
        <li class="govuk-!-font-size-19">why you want to transfer to the High Court for enforcement</li>
        </ul>
        """;

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">Include details of any facts or evidence that you think the court should
        consider when it makes a decision. You can upload the evidence on the next page.</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("whatOrderWanted")
            .pageLabel("What order do you want the court to make, and why?")
            .label("whatOrderWanted-lineSeparator", "---")
            .label("whatOrderWanted-adjourn", ADJOURN_EXAMPLES,
                   fieldEquals("xui_genapp_ApplicationType", GenAppType.ADJOURN))
            .label("whatOrderWanted-setAside", SET_ASIDE_INFO,
                   fieldEquals("xui_genapp_ApplicationType", GenAppType.SET_ASIDE))
            .label("whatOrderWanted-somethingElse", SOMETHING_ELSE_EXAMPLES,
                   fieldEquals("xui_genapp_ApplicationType", GenAppType.SOMETHING_ELSE))
            .label("whatOrderWanted-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getWhatOrderWanted, null, null,
                       "Explain what you want the court to do, and why")
            .done();
    }

}
