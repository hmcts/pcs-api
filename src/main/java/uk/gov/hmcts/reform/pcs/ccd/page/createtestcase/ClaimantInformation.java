package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
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
//        OrganisationDto usersOrganisation = professionalOrganisationRetriever.retrieve().getBody();
        String orgDetails = professionalOrganisationRetriever.retrieve();

//        assert usersOrganisation != null;
        pageBuilder
            .page("Organisation Details")
            .pageLabel("Organisation Details")
            .label("OrganisationQuestion", "Are you a part of this Organisation?")
            .complex(PCSCase::getOrganisationPolicy)
                .mandatory(OrganisationPolicy::getPreviousOrganisations) //returns a set
//            .mandatory(PrepopulateToUsersOrganisation )
//                .mandatory(OrganisationPosurelicy::getPrepopulateToUsersOrganisation) //yes or no to set the field
//                .mandatory(OrganisationPolicy::setPrepopulateToUsersOrganisation("Y"))
                .done();


//            .label("lol", OrganisationPolicy::getPreviousOrganisations);
//            .label("OrganisationName", usersOrganisation.getName());
    }
}
