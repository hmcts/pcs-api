package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
// import uk.gov.hmcts.reform.pcs.clients.ProfessionalOrganisationRetriever;
// import uk.gov.hmcts.reform.pcs.dto.OrganisationDto;

@Component
public class ClaimantInformation implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("Organisation Details")
            .pageLabel("Organisation Details")
            .label("OrganisationQuestion", "Are you a part of this Organisation?");
        // .complex(PCSCase::getOrganisationPolicy)
        // .complex(OrganisationPolicy::getOrganisation)
        // .optional(Organisation::getOrganisationId)
        // .done();
    }
}
