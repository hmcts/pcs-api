package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

@Slf4j
@AllArgsConstructor
public class OtherPartiesAgreed implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">The ‘other party’ is the other side involved in the defendant’s case.
         For example their landlord, housing association, or mortgage lender.</p>
        <p class="govuk-body">Every other party will need to agree to let them make this application, if they
        don’t agree, the defendant may need to pay a higher fee.</p>
        <h2 class="govuk-heading-m">If the other parties do not agree</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">The application fee will increase to ${xui_genapp_MaxFee} if:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">the defendant has already told the other party that they are
           making this application, and</li>
          <li class="govuk-!-font-size-19">the other party did not agree to it
           (this means that they objected to it)</li>
        </ul>
        <p class="govuk-body">You’ll see the final application fee before you pay.</p>

        <h2 class="govuk-heading-m">What you need to do</h2>
        <p class="govuk-body govuk-!-margin-bottom-1">You will need to upload evidence for the court, to show that:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">the defendant has told the other parties, and</li>
          <li class="govuk-!-font-size-19">they agreed to it</li>
        </ul>
        <p class="govuk-body"> For example a copy of a letter, email or a signed consent order.
         We will ask you to upload this evidence later in this application. </p>
        <p class="govuk-body">If the defendant was not able to contact the other parties, or they attempted to
         contact them but the other party did not respond, select ‘No’.</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("otherPartiesAgreed")
            .pageLabel("Has the defendant asked the other parties if they agree to this application?")
            .label("otherPartiesAgreed-lineSeparator", "---")
            .label("otherPartiesAgreed-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getOtherPartiesAgreed)
            .done();
    }

}
