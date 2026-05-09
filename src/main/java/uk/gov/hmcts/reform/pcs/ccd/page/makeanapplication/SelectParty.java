package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Slf4j
@AllArgsConstructor
public class SelectParty implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("selectParty")
            .pageLabel("Which defendant are you making this application for")
            .label("selectParty-lineSeparator", "---")
            .mandatoryWithLabel(PCSCase::getRepresentedPartyNames,
                                "Which defendant are you making this application for?");
    }

}
