package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoToBoolean;

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
                .walesRegistered(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesRegistered() : null)
                .walesRegistrationNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesRegistrationNumber() : null)
                .walesLicensed(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesLicensed() : null)
                .walesLicenceNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesLicenceNumber() : null)
                .walesLicensedAgentAppointed(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesLicensedAgentAppointed() : null)
                .walesAgentFirstName(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesAgentFirstName() : null)
                .walesAgentLastName(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesAgentLastName() : null)
                .walesAgentLicenceNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesAgentLicenceNumber() : null)
                .walesAgentAppointmentDate(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getWalesAgentAppointmentDate() : null)
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
