package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class ReasonsNotToShare implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <h2 class="govuk-heading-m">If the other parties do not agree</h2>
        <p class="govuk-body">We usually send a copy of the application to the other parties
         (the defendant’s landlord, housing association or mortgage lender). This gives the other party
         the opportunity to respond to it.
        </p>
        <p class="govuk-body">In some exceptional circumstances, the judge will consider the application
         without telling the other party first.</p>
        <p class="govuk-body govuk-!-margin-bottom-1">For example, if:</p>
        <ul class="govuk-list govuk-list--bullet">
          <li class="govuk-!-font-size-19">
            it is so urgent that there is not enough time for the defendant to give notice
          </li>
          <li class="govuk-!-font-size-19">
            giving someone notice could undermine the order that the defendant wants the court to grant
          </li>
          <li class="govuk-!-font-size-19">
            the defendant believes that they are at risk from the other party
            </li>
        </ul>
        </ul>
        <div class="govuk-warning-text">
          <span class="govuk-warning-text__icon" aria-hidden="true">!</span>
          <strong class="govuk-warning-text__text">
            <span class="govuk-visually-hidden">Warning</span>
            We will ask you to provide the reason. The court will consider the reason, and the
            defendant may not be successful.
          </strong>
        </div>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("reasonsNotToShare")
            .pageLabel("Are there any reasons that this application should not be shared with other parties?")
            .showCondition(fieldEquals("xui_genapp_OtherPartiesAgreed", VerticalYesNo.NO))
            .label("reasonsNotToShare-lineSeparator", "---")
            .label("reasonsNotToShare-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getWithoutNotice)
            .mandatory(XuiGenAppRequest::getWithoutNoticeReason,
                       fieldEquals("xui_genapp_WithoutNotice", VerticalYesNo.YES))
            .done();
    }

}
