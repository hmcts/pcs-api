package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;

import java.util.Optional;

@Component
public class HousingActWalesView {

    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        getMainClaim(pcsCaseEntity)
            .map(ClaimEntity::getHousingActWales)
            .ifPresent(housingActWalesEntity -> setHousingActWalesFields(pcsCase, housingActWalesEntity));
    }

    private static Optional<ClaimEntity> getMainClaim(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getClaims().stream()
            .findFirst();
    }

    private static void setHousingActWalesFields(PCSCase pcsCase, HousingActWalesEntity housingActWalesEntity) {
        WalesHousingAct housingActWales = WalesHousingAct.builder()
            .registered(housingActWalesEntity.getRegistered())
            .registrationNumber(housingActWalesEntity.getRegistrationNumber())
            .licensed(housingActWalesEntity.getLicensed())
            .licenceNumber(housingActWalesEntity.getLicenceNumber())
            .licensedAgentAppointed(housingActWalesEntity.getAgentAppointed())
            .agentFirstName(housingActWalesEntity.getAgentFirstName())
            .agentLastName(housingActWalesEntity.getAgentLastName())
            .agentLicenceNumber(housingActWalesEntity.getAgentLicenceNumber())
            .agentAppointmentDate(housingActWalesEntity.getAgentAppointmentDate())
            .build();

        pcsCase.setWalesHousingAct(housingActWales);
    }

}
