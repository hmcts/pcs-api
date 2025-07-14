package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

public class PartyPage implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Party Page") //only seen in json, not on page
            .pageLabel("Select your Party.") //only seen in json, not on page
//            .label("lineSeparator2", "---") //has to be unique id to be seen.
            .mandatory(PCSCase::getPartyType);
    }
}
