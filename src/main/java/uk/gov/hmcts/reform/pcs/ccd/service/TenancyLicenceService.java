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
        TenancyLicence.TenancyLicenceBuilder tenancyLicenceBuilder = TenancyLicence.builder()
            .tenancyLicenceType(pcsCase.getTypeOfTenancyLicence() != null
                    ? pcsCase.getTypeOfTenancyLicence().getLabel() : null)
            .tenancyLicenceDate(pcsCase.getTenancyLicenceDate())
            .detailsOfOtherTypeOfTenancyLicence(pcsCase.getDetailsOfOtherTypeOfTenancyLicence())
            .supportingDocuments(ListValueUtils.unwrapListItems(pcsCase.getTenancyLicenceDocuments()))
            .rentStatementDocuments(ListValueUtils.unwrapListItems(pcsCase.getRentStatementDocuments()))
            .noticeServed(YesOrNoToBoolean.convert(pcsCase.getHasNoticeBeenServed()))
            .rentAmount(penceToPounds(pcsCase.getCurrentRent()))
            .rentPaymentFrequency(pcsCase.getRentFrequency())
            .otherRentFrequency(pcsCase.getOtherRentFrequency())
            .dailyRentChargeAmount(getDailyRentAmount(pcsCase))
            .totalRentArrears(penceToPounds(pcsCase.getTotalRentArrears()))
            .thirdPartyPaymentSources(pcsCase.getThirdPartyPaymentSources())
            .thirdPartyPaymentSourceOther(pcsCase.getThirdPartyPaymentSourceOther())
            .arrearsJudgmentWanted(YesOrNoToBoolean.convert(pcsCase.getArrearsJudgmentWanted()));

        buildNoticeServedDetails(pcsCase, tenancyLicenceBuilder);

        buildWalesNoticeServedDetails(pcsCase, tenancyLicenceBuilder);

        buildWalesHousingActDetails(pcsCase, tenancyLicenceBuilder);

        buildWalesOccupationContractDetails(pcsCase, tenancyLicenceBuilder);

        return tenancyLicenceBuilder.build();
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

    private void buildNoticeServedDetails(PCSCase pcsCase,
                                                    TenancyLicence.TenancyLicenceBuilder tenancyLicenceBuilder) {
        // Add notice served details
        if (pcsCase.getNoticeServedDetails() != null) {
            tenancyLicenceBuilder.noticeServed(YesOrNoToBoolean.convert(pcsCase.getHasNoticeBeenServed()));
        }

        if (pcsCase.getNoticeServedDetails() != null) {
            tenancyLicenceBuilder
                .noticeServiceMethod(pcsCase.getNoticeServedDetails().getNoticeServiceMethod() != null
                        ? pcsCase.getNoticeServedDetails().getNoticeServiceMethod().name()
                        : null)
                .noticePostedDate(pcsCase.getNoticeServedDetails().getNoticePostedDate())
                .noticeDeliveredDate(pcsCase.getNoticeServedDetails().getNoticeDeliveredDate())
                .noticeHandedOverDateTime(pcsCase.getNoticeServedDetails().getNoticeHandedOverDateTime())
                .noticePersonName(pcsCase.getNoticeServedDetails().getNoticePersonName())
                .noticeEmailSentDateTime(pcsCase.getNoticeServedDetails().getNoticeEmailSentDateTime())
                .noticeEmailExplanation(pcsCase.getNoticeServedDetails().getNoticeEmailExplanation())
                .noticeOtherElectronicDateTime(pcsCase.getNoticeServedDetails().getNoticeOtherElectronicDateTime())
                .noticeOtherDateTime(pcsCase.getNoticeServedDetails().getNoticeOtherDateTime())
                .noticeOtherExplanation(pcsCase.getNoticeServedDetails().getNoticeOtherExplanation())
                .noticeDocuments(ListValueUtils.unwrapListItems(pcsCase.getNoticeServedDetails().getNoticeDocuments()))
                .build();
        }
    }

    private void buildWalesNoticeServedDetails(PCSCase pcsCase,
                                               TenancyLicence.TenancyLicenceBuilder tenancyLicence) {
        // Add notice served details for Wales
        if (pcsCase.getWalesNoticeDetails() != null) {
            tenancyLicence.walesNoticeServed(pcsCase.getWalesNoticeDetails().getNoticeServed() != null
                ? YesOrNoToBoolean.convert(pcsCase.getWalesNoticeDetails().getNoticeServed()) : null);
            tenancyLicence.walesTypeOfNoticeServed(pcsCase.getWalesNoticeDetails().getTypeOfNoticeServed());
        }
    }

    private void buildWalesHousingActDetails(PCSCase pcsCase,
                                             TenancyLicence.TenancyLicenceBuilder tenancyLicence) {
        // Add Wales Housing Act details
        if (pcsCase.getWalesHousingAct() != null) {
            tenancyLicence.walesRegistered(pcsCase.getWalesHousingAct().getRegistered());
            tenancyLicence.walesRegistrationNumber(pcsCase.getWalesHousingAct().getRegistrationNumber());
            tenancyLicence.walesLicensed(pcsCase.getWalesHousingAct().getLicensed());
            tenancyLicence.walesLicenceNumber(pcsCase.getWalesHousingAct().getLicenceNumber());
            tenancyLicence.walesLicensedAgentAppointed(pcsCase.getWalesHousingAct().getLicensedAgentAppointed());
            tenancyLicence.walesAgentFirstName(pcsCase.getWalesHousingAct().getAgentFirstName());
            tenancyLicence.walesAgentLastName(pcsCase.getWalesHousingAct().getAgentLastName());
            tenancyLicence.walesAgentLicenceNumber(pcsCase.getWalesHousingAct().getAgentLicenceNumber());
            tenancyLicence.walesAgentAppointmentDate(pcsCase.getWalesHousingAct().getAgentAppointmentDate());
        }
    }

    private void buildWalesOccupationContractDetails(PCSCase pcsCase,
                                                     TenancyLicence.TenancyLicenceBuilder tenancyLicence) {
        // Add Wales Occupation Contract/Licence details
        if (pcsCase.getOccupationLicenceDetailsWales() != null) {
            tenancyLicence.occupationLicenceTypeWales(
                pcsCase.getOccupationLicenceDetailsWales().getOccupationLicenceTypeWales());
            tenancyLicence.walesOtherLicenceTypeDetails(
                pcsCase.getOccupationLicenceDetailsWales().getOtherLicenceTypeDetails());
            tenancyLicence.walesLicenceStartDate(
                pcsCase.getOccupationLicenceDetailsWales().getLicenceStartDate());
            tenancyLicence.walesLicenceDocuments(
                ListValueUtils.unwrapListItems(
                    pcsCase.getOccupationLicenceDetailsWales().getLicenceDocuments()));
        }
    }

    private static BigDecimal penceToPounds(String pence) {
        if (pence == null || pence.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(pence).movePointLeft(2);
    }
}
