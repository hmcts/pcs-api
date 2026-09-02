package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
public class PartyCounterClaimAgainst implements CcdPageConfiguration {

    private static final String AGAINST_PARTY_LIST_LABEL = "Who is the counterclaim being made against?";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("partyCounterClaimAgainst")
            .pageLabel("Party counterclaim is against")
            .label("partyCounterClaimAgainst-lineSeparator", "---")
            .mandatory(PCSCase::getPartyMultiSelectionList, null, null, AGAINST_PARTY_LIST_LABEL);
    }
}
