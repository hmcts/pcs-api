package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.clients.ProfessionalOrganisationRetriever;
import uk.gov.hmcts.reform.pcs.dto.OrganisationDto;

@Component
public class ClaimantInformation implements CcdPageConfiguration {

    ProfessionalOrganisationRetriever professionalOrganisationRetriever;

    public ClaimantInformation(ProfessionalOrganisationRetriever professionalOrganisationRetriever){
        this.professionalOrganisationRetriever = professionalOrganisationRetriever;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        OrganisationDto usersOrganisation = professionalOrganisationRetriever.retrieve().getBody();

        pageBuilder
            .page("Organisation Details")
            .pageLabel("Organisation Details")
            .label("OrganisationQuestion", "Are you a part of this Organisation?")
            .label("OrganisationName", usersOrganisation.getName());
    }
}
