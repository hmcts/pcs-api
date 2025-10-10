package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.utils.ListValueUtils;
import uk.gov.hmcts.reform.pcs.ccd.utils.YesOrNoToBoolean;

import java.math.BigDecimal;

@Service
public class TenancyLicenceService {

    public TenancyLicence buildTenancyLicence(PCSCase pcsCase) {
        return TenancyLicence.builder()
                .tenancyLicenceType(pcsCase.getTypeOfTenancyLicence() != null
                        ? pcsCase.getTypeOfTenancyLicence().getLabel() : null)
                .tenancyLicenceDate(pcsCase.getTenancyLicenceDate())
                .detailsOfOtherTypeOfTenancyLicence(pcsCase.getDetailsOfOtherTypeOfTenancyLicence())
                .supportingDocuments(ListValueUtils.unwrapListItems(pcsCase.getTenancyLicenceDocuments()))
                .rentStatementDocuments(ListValueUtils.unwrapListItems(pcsCase.getRentStatementDocuments()))
                .noticeDocuments(ListValueUtils.unwrapListItems(pcsCase.getNoticeDocuments()))
                .noticeServed(YesOrNoToBoolean.convert(pcsCase.getNoticeServed()))
                .rentAmount(penceToPounds(pcsCase.getCurrentRent()))
                .rentPaymentFrequency(pcsCase.getRentFrequency())
                .otherRentFrequency(pcsCase.getOtherRentFrequency())
                .dailyRentChargeAmount(getDailyRentAmount(pcsCase))
                .totalRentArrears(penceToPounds(pcsCase.getTotalRentArrears()))
                .thirdPartyPaymentSources(pcsCase.getThirdPartyPaymentSources())
                .thirdPartyPaymentSourceOther(pcsCase.getThirdPartyPaymentSourceOther())
                // Add notice details fields
                .noticeServiceMethod(pcsCase.getNoticeServiceMethod() != null
                                    ? pcsCase.getNoticeServiceMethod().name()
                                    : null)
                .noticePostedDate(pcsCase.getNoticePostedDate())
                .noticeDeliveredDate(pcsCase.getNoticeDeliveredDate())
                .noticeHandedOverDateTime(pcsCase.getNoticeHandedOverDateTime())
                .noticePersonName(pcsCase.getNoticePersonName())
                .noticeEmailSentDateTime(pcsCase.getNoticeEmailSentDateTime())
                .noticeEmailExplanation(pcsCase.getNoticeEmailExplanation())
                .noticeOtherElectronicDateTime(pcsCase.getNoticeOtherElectronicDateTime())
                .noticeOtherDateTime(pcsCase.getNoticeOtherDateTime())
                .noticeOtherExplanation(pcsCase.getNoticeOtherExplanation())
                .arrearsJudgmentWanted(YesOrNoToBoolean.convert(pcsCase.getArrearsJudgmentWanted()))
                // Add Wales Housing Act details
                .registered(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getRegistered() : null)
                .registrationNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getRegistrationNumber() : null)
                .licensed(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getLicensed() : null)
                .licenceNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getLicenceNumber() : null)
                .licensedAgentAppointed(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getLicensedAgentAppointed() : null)
                .agentFirstName(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getAgentFirstName() : null)
                .agentLastName(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getAgentLastName() : null)
                .agentLicenceNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getAgentLicenceNumber() : null)
                .agentAppointmentDate(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getAgentAppointmentDate() : null)
                .build();
    }

    private BigDecimal getDailyRentAmount(PCSCase pcsCase) {
        String[] fieldValues = {
            pcsCase.getAmendedDailyRentChargeAmount(),
            pcsCase.getCalculatedDailyRentChargeAmount(),
            pcsCase.getDailyRentChargeAmount()
        };
        for (String value : fieldValues) {
            if (value != null && !value.trim().isEmpty()) {
                return penceToPounds(value);
            }
        }
        return null;
    }

    private static BigDecimal penceToPounds(String pence) {
        if (pence == null || pence.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(pence).movePointLeft(2);
    }
}
