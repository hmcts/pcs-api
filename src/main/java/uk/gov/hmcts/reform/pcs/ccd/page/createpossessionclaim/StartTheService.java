package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier.UNABLE_TO_RETRIEVE;

/**
 * CCD page configuration for making a housing possession claim online.
 */
public class StartTheService implements CcdPageConfiguration {

    private static final String FEE_AMOUNT_KNOWN = "feeAmount!=\"" + UNABLE_TO_RETRIEVE + "\"";
    private static final String FEE_AMOUNT_UNKNOWN = "feeAmount=\"" + UNABLE_TO_RETRIEVE + "\"";

    private static final String HEADING_L = "govuk-heading-l";
    private static final String HEADING_M = "govuk-heading-m";
    private static final String BODY = "govuk-body";
    private static final String LINK = "govuk-link";
    private static final String LIST_ITEM = "govuk-list govuk-!-font-size-19";
    private static final String PADDING_TOP_0 = "govuk-!-padding-top-0";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("startTheService")
            .readonly(PCSCase::getFeeAmount, NEVER_SHOW, true)
            .label("mainContentTop",
                   "<h1 class=\"" + HEADING_L + "\">Make a housing possession claim online</h1>"
                       + "<p class=\"" + BODY + "\">You can use this online service if you’re a registered provider of "
                       + "social housing or a community landlord and the property you want to claim possession of is in "
                       + "England or Wales.</p>"
                       + "<p class=\"" + BODY + "\">Solicitors or legal representatives must continue to submit claims "
                       + "using Possession Claim Online (PCOL) or by using a claim form with the relevant particulars "
                       + "of claim form.</p>"
                       + "<p class=\"" + BODY + "\">This service is also available "
                       + "<a href=\"javascript:void(0)\" class=\"" + LINK + "\">in Welsh (Cymraeg)</a>.</p>"
            )
            .label("feePaymentWithAmount",
                   "<p class=\"" + BODY + "\">The claim fee is ${feeAmount}. "
                       + "You can pay by card or through Payment By Account (PBA).</p>",
                   FEE_AMOUNT_KNOWN
            )
            .label("feePaymentWithoutAmount",
                   "<p class=\"" + BODY + "\">You can pay by card or through Payment By Account (PBA).</p>",
                   FEE_AMOUNT_UNKNOWN
            )
            .label("mainContentBottom",
                   "<p class=\"" + BODY + "\">Your claim will be saved as you answer the questions, so you’ll be "
                       + "able to close and return to your draft. Before you submit, you must complete a statement of "
                       + "truth. This certifies that you believe the information you’ve provided is true.</p>"
                       + "<h2 class=\"" + HEADING_M + " " + PADDING_TOP_0 + "\">What you’ll need</h2>"
                       + "<p class=\"" + BODY + "\">Before you start, make sure you have the following information:</p>"
                       + "<ul>"
                       + "<li class=\"" + LIST_ITEM + "\">details of the tenancy, contract, licence or mortgage agreement</li>"
                       + "<li class=\"" + LIST_ITEM + "\">the defendants’ details "
                       + "(the people you’re making the claim against)</li>"
                       + "<li class=\"" + LIST_ITEM + "\">your reasons for making a possession claim</li>"
                       + "<li class=\"" + LIST_ITEM + "\">copies of any relevant documents. You can either upload documents "
                       + "now or closer to the hearing date. Any documents you upload now will be included in the pack of "
                       + "documents that a judge will receive before the hearing (the bundle)</li>"
                       + "</ul>"
                       + "<h2 class=\"" + HEADING_M + "\">Completing your claim</h2>"
                       + "<p class=\"" + BODY + "\">Once you’ve finished answering the questions, you can either:</p>"
                       + "<ul>"
                       + "<li class=\"" + LIST_ITEM + "\">sign, submit and pay for your claim now, or</li>"
                       + "<li class=\"" + LIST_ITEM + "\">save it as a draft. You or someone else can then return to "
                       + "sign, submit and pay at a later date</li>"
                       + "</ul>"
            );
    }
}
