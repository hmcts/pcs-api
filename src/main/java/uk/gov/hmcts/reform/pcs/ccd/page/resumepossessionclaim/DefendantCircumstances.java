package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class DefendantCircumstances implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantCircumstances")
            .pageLabel("Defendants' circumstances")
            .label(
                "defendantCircumstances-info", """
                    ---
                    <p class="govuk-body" tabindex="0">
                     You can use this section to tell us anything relevant about the ${dynamicDefendantText}
                     financial or personal situation.
                    </p>
                    """)
            .mandatory(PCSCase::getHasDefendantCircumstancesInfo)
            .mandatory(PCSCase::getDefendantCircumstancesInfo, "hasDefendantCircumstancesInfo=\"YES\"");
    }


}
