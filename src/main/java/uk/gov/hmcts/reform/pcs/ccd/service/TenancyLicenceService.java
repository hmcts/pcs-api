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
                .walesNoticeServed(YesOrNoToBoolean.convert(pcsCase.getWalesNoticeDetails() != null
                                                                ? pcsCase.getWalesNoticeDetails().getNoticeServed()
                                                                : null))
                .walesTypeOfNoticeServed(pcsCase.getWalesNoticeDetails() != null
                                             ? pcsCase.getWalesNoticeDetails().getTypeOfNoticeServed() : null)
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
                    ? pcsCase.getWalesHousingAct().getRegistered() : null)
                .walesRegistrationNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getRegistrationNumber() : null)
                .walesLicensed(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getLicensed() : null)
                .walesLicenceNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getLicenceNumber() : null)
                .walesLicensedAgentAppointed(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getLicensedAgentAppointed() : null)
                .walesAgentFirstName(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getAgentFirstName() : null)
                .walesAgentLastName(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getAgentLastName() : null)
                .walesAgentLicenceNumber(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getAgentLicenceNumber() : null)
                .walesAgentAppointmentDate(pcsCase.getWalesHousingAct() != null 
                    ? pcsCase.getWalesHousingAct().getAgentAppointmentDate() : null)
                // Wales Occupation Contract/Licence details
                .walesOccupationLicenceType(
                    pcsCase.getOccupationLicenceDetailsWales() != null
                        ? pcsCase.getOccupationLicenceDetailsWales().getLicenceType()
                        : null)
                .walesOtherLicenceTypeDetails(
                    pcsCase.getOccupationLicenceDetailsWales() != null
                        ? pcsCase.getOccupationLicenceDetailsWales().getOtherLicenceTypeDetails()
                        : null)
                .walesLicenceStartDate(
                    pcsCase.getOccupationLicenceDetailsWales() != null
                        ? pcsCase.getOccupationLicenceDetailsWales().getLicenceStartDate()
                        : null)
                .walesLicenceDocuments(
                    pcsCase.getOccupationLicenceDetailsWales() != null
                        ? ListValueUtils.unwrapListItems(
                            pcsCase.getOccupationLicenceDetailsWales().getLicenceDocuments())
                        : null)
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
