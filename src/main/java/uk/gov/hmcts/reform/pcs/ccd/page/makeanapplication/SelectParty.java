package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.fieldEquals;

@Slf4j
@AllArgsConstructor
public class SelectParty implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectParty")
            .pageLabel("Applicant")
            .showCondition(fieldEquals("multipleRepresentedParties", VerticalYesNo.YES))
            .label("selectParty-lineSeparator", "---")
            .readonly(PCSCase::getMultipleRepresentedParties, NEVER_SHOW)
            .mandatoryWithLabel(PCSCase::getRepresentedPartyNames,
                                "Which party is making the application?");
    }

}
