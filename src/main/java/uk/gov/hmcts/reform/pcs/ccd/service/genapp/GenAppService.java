package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.HelpWithFeesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;

@Service
@AllArgsConstructor
public class GenAppService {

    private final GenAppRepository genAppRepository;

    public GenAppEntity createGenAppEntity(PCSCase caseData, PcsCaseEntity pcsCaseEntity, PartyEntity applicantParty) {

        CitizenGenAppRequest citizenCreateGenApp = caseData.getCitizenGenAppRequest();

        GenAppEntity genAppEntity = GenAppEntity.builder()
            .type(citizenCreateGenApp.getApplicationType())
            .party(applicantParty)
            .state(GenAppState.SUBMITTED)
            .within14Days(citizenCreateGenApp.getWithin14Days())
            .needHwf(citizenCreateGenApp.getNeedHwf())
            .appliedForHwf(citizenCreateGenApp.getAppliedForHwf())
            .build();

        if (citizenCreateGenApp.getAppliedForHwf() == VerticalYesNo.YES
                && citizenCreateGenApp.getHwfReference() != null) {
            HelpWithFeesEntity helpWithFeesEntity = new HelpWithFeesEntity();
            helpWithFeesEntity.setHwfReference(citizenCreateGenApp.getHwfReference());
            genAppEntity.setHelpWithFeesEntity(helpWithFeesEntity);
        }

        pcsCaseEntity.addGenApp(genAppEntity);

        return genAppRepository.save(genAppEntity);
    }

}
