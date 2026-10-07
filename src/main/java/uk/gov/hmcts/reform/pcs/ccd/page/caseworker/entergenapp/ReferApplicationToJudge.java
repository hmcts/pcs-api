package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

public class ReferApplicationToJudge implements CcdPageConfiguration {

    private static final String MESSAGE_TEXT = """
            <p class="body">You must refer this application to a judge.</p>
        """;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("referApplicationToJudge")
            .pageLabel("Refer without notice application to judge")
            .showCondition(ShowConditions.and(
                fieldEquals("enter_genapp_AllPartiesAgree", VerticalYesNo.NO),
                fieldEquals("enter_genapp_WithoutNotice", VerticalYesNo.YES))
            )
            .label("referApplicationToJudge-lineSeparator", "---")
            .label("referApplicationToJudge-message", MESSAGE_TEXT);
    }

}
