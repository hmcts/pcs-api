package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;

/**
 * CCD page configuration for making a housing possession claim online.
 */
public class StartTheService implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("startTheService")
            .label("mainContent",
                   "<h1 class=\"govuk-heading-l\">Make a housing possession claim online</h1>"
                       + "<p class=\"govuk-body\">You can use this online service if you're a registered "
                       + "provider of social housing or a community landlord and the property "
                       + "you want to claim possession of is in England or Wales.</p>"
                       + "<p class=\"govuk-body\">This service is also available "
                       + "<a href=\"javascript:void(0)\" class=\"govuk-link\">in Welsh (Cymraeg)</a>.</p>"
                       + "<p class=\"govuk-body\">The claim fee is £404. You can pay by card or through Payment By "
                       + "Account (PBA).</p>"
                       + "<p class=\"govuk-body\">Your claim will be saved as you answer the questions, so you'll be "
                       + "able to close and return to your draft.</p>"
                       + "<h2 class=\"govuk-heading-m\">What you'll need</h2>"
                       + "<p class=\"govuk-body\">Before you start, make sure you have the following information:</p>"
                       + "<ul>"
                       + "<li class=\"govuk-list govuk-!-font-size-19\">details of the tenancy, contract, "
                       + "licence or mortgage agreement</li>"
                       + "<li class=\"govuk-list govuk-!-font-size-19\">the defendants' details (the people "
                       + "you're making the claim against)</li>"
                       + "<li class=\"govuk-list govuk-!-font-size-19\">your reasons for making a possession "
                       + "claim</li>"
                       + "<li class=\"govuk-list govuk-!-font-size-19\">copies of any relevant documents. "
                       + "You can either upload documents now or closer to the hearing date. "
                       + "Any documents you upload now will be included in the pack of documents "
                       + "that a judge will receive before the hearing (the bundle)</li>"
                       + "</ul>"
                       + "<p class=\"govuk-body\">Once you've finished answering the questions, you can either:</p>"
                       + "<ul >"
                       + "<li class=\"govuk-list govuk-!-font-size-19\">sign, submit and pay for your claim "
                       + "now, or</li>"
                       + "<li class=\"govuk-list govuk-!-font-size-19\">save it as a draft. You or someone else "
                       + "can then return to sign, submit and pay at a later date</li>"
                       + "</ul>"
            );
    }
}
