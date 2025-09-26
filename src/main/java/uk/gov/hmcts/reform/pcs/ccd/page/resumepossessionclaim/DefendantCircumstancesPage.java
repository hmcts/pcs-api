package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class DefendantCircumstancesPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantCircumstances")
            .pageLabel("Defendants' circumstances")
            .complex(PCSCase::getDefendantCircumstances)
            .mandatory(DefendantCircumstances::getDefendantTermPossessive,NEVER_SHOW)
            .readonlyNoSummary(DefendantCircumstances::getDefendantCircumstancesLabel)
            .mandatory(DefendantCircumstances::getHasDefendantCircumstancesInfo)
            .mandatory(DefendantCircumstances::getDefendantCircumstancesInfo,
                       "hasDefendantCircumstancesInfo=\"YES\"")
            .done();
    }

}
