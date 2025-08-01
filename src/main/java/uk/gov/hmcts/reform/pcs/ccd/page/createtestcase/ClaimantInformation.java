package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Organisation;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
public class ClaimantInformation implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Organisation Details")
            .pageLabel("Organisation Details")
            .label("OrganisationQuestion", "Are you a part of this Organisation?")
            .complex(PCSCase::getOrganisationPolicy)
            .complex(OrganisationPolicy::getOrganisation)
            .mandatory(Organisation::getOrganisationId)
            .done();
    }
}
