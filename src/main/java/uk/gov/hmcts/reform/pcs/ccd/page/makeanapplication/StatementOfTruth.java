package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;

@Slf4j
public class StatementOfTruth implements CcdPageConfiguration {

    private static final String INFO_MARKDOWN = """
        <p class="govuk-body">
          I understand that proceedings for contempt of court may be brought against anyone who makes, or causes to be
          made, a false statement in a document verified by a statement of truth without an honest belief in its truth.
        </p>
        <p class="govuk-body">
          Defendant: ${currentRepresentedPartyName}
        </p>
        <p class="govuk-body govuk-!-font-weight-bold">
          Completed by the defendant’s legal representative (as defined by CPR 2.3 (1))
        </p>

        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("statementOfTruth")
            .pageLabel("Statement of truth")
            .label("statementOfTruth-lineSeparator", "---")
            .label("statementOfTruth-info", INFO_MARKDOWN)
            .complex(PCSCase::getXuiGenAppRequest)
            .mandatory(XuiGenAppRequest::getAgreementDefendantLegalRep)
            .mandatory(XuiGenAppRequest::getSotFullName)
            .mandatory(XuiGenAppRequest::getSotFirmName)
            .mandatory(XuiGenAppRequest::getSotPositionHeld)
            .done();
    }

}
