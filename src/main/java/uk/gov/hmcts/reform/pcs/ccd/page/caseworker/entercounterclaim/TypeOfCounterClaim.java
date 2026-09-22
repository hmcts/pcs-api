package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;

@Component
public class TypeOfCounterClaim implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("typeOfCounterClaim")
            .pageLabel("Type of counterclaim")
            .label("typeOfCounterClaim-lineSeparator", "---")
            .complex(PCSCase::getEnterCounterClaim)
                .mandatory(EnterCounterClaimDetails::getClaimTypeOption)
            .done();
    }
}
