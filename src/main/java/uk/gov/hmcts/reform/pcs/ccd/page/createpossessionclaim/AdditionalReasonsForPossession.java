package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.OtherReasonsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo.YES;

public class AdditionalReasonsForPossession implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("additionalReasonsForPossession")
            .pageLabel("Additional reasons for possession")
            .label("additionalReasonsForPossession-separator", "---")
            .complex(PCSCase::getOtherReasonsForPossession)
                .mandatory(OtherReasonsForPossession::getHasOtherReasons)
                .mandatory(OtherReasonsForPossession::getOtherReasons,
                    ShowConditions.fieldEquals("otherReasonsForPossession.hasOtherReasons", YES))
            .done();
    }

}
