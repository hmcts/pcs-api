package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoToBoolean;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;

import java.math.BigDecimal;

@Service
public class TenancyLicenceService {

    public TenancyLicence buildTenancyLicence(PCSCase pcsCase) {
        TenancyLicenceDetails tenancyDetails = pcsCase.getTenancyLicenceDetails();
        TenancyLicence.TenancyLicenceBuilder tenancyLicenceBuilder = TenancyLicence.builder()
            .tenancyLicenceType(tenancyDetails != null && tenancyDetails.getTypeOfTenancyLicence() != null
                    ? tenancyDetails.getTypeOfTenancyLicence().getLabel() : null)
            .tenancyLicenceDate(tenancyDetails != null ? tenancyDetails.getTenancyLicenceDate() : null)
            .detailsOfOtherTypeOfTenancyLicence(tenancyDetails != null
                    ? tenancyDetails.getDetailsOfOtherTypeOfTenancyLicence() : null)
            .supportingDocuments(ListValueUtils.unwrapListItems(
                    tenancyDetails != null ? tenancyDetails.getTenancyLicenceDocuments() : null))
            .arrearsJudgmentWanted(YesOrNoToBoolean.convert(pcsCase.getArrearsJudgmentWanted()));

        buildRentSection(pcsCase.getRentDetails(), tenancyLicenceBuilder);

        tenancyLicenceBuilder.noticeServed(YesOrNoToBoolean.convert(pcsCase.getNoticeServed()));

        buildRentArrearsSection(pcsCase.getRentArrears(), tenancyLicenceBuilder);

        buildNoticeServedDetails(pcsCase.getNoticeServedDetails(), tenancyLicenceBuilder);

        buildWalesNoticeServedDetails(pcsCase.getWalesNoticeDetails(), tenancyLicenceBuilder);

        buildWalesHousingActDetails(pcsCase.getWalesHousingAct(), tenancyLicenceBuilder);

        buildWalesOccupationContractDetails(pcsCase.getOccupationLicenceDetailsWales(), tenancyLicenceBuilder);

        return tenancyLicenceBuilder.build();
    }

    private void buildRentSection(RentDetails rentDetails,
                                  TenancyLicence.TenancyLicenceBuilder tenancyLicenceBuilder) {
        if (rentDetails != null) {
            tenancyLicenceBuilder
                    .rentAmount(penceToPounds(rentDetails.getCurrentRent()))
                    .rentPaymentFrequency(rentDetails.getFrequency())
                    .otherRentFrequency(rentDetails.getOtherFrequency())
                    .dailyRentChargeAmount(getDailyRentAmount(rentDetails));
        }
    }

    private BigDecimal getDailyRentAmount(RentDetails rentDetails) {
        if (rentDetails == null) {
            return null;
        }
        String[] fieldValues = {
            rentDetails.getAmendedDailyCharge(),
            rentDetails.getCalculatedDailyCharge(),
            rentDetails.getDailyCharge()
        };
        for (String value : fieldValues) {
            if (value != null && !value.trim().isEmpty()) {
                return penceToPounds(value);
            }
        }
        return null;
    }

    private void buildRentArrearsSection(RentArrearsSection rentArrears,
                                         TenancyLicence.TenancyLicenceBuilder tenancyLicenceBuilder) {
        if (rentArrears != null) {
            tenancyLicenceBuilder
                    .rentStatementDocuments(ListValueUtils.unwrapListItems(rentArrears.getStatementDocuments()))
                    .totalRentArrears(penceToPounds(rentArrears.getTotal()))
                    .thirdPartyPaymentSources(rentArrears.getThirdPartyPaymentSources())
                    .thirdPartyPaymentSourceOther(rentArrears.getThirdPartyPaymentSourceOther());
        }
    }

    private void buildNoticeServedDetails(NoticeServedDetails noticeServedDetails,
                                                    TenancyLicence.TenancyLicenceBuilder tenancyLicenceBuilder) {
        // Add notice served details
        if (noticeServedDetails != null) {
            tenancyLicenceBuilder
                .noticeServiceMethod(noticeServedDetails.getNoticeServiceMethod() != null
                        ? noticeServedDetails.getNoticeServiceMethod().name()
                        : null)
                .noticePostedDate(noticeServedDetails.getNoticePostedDate())
                .noticeDeliveredDate(noticeServedDetails.getNoticeDeliveredDate())
                .noticeHandedOverDateTime(noticeServedDetails.getNoticeHandedOverDateTime())
                .noticePersonName(noticeServedDetails.getNoticePersonName())
                .noticeEmailSentDateTime(noticeServedDetails.getNoticeEmailSentDateTime())
                .noticeEmailExplanation(noticeServedDetails.getNoticeEmailExplanation())
                .noticeOtherElectronicDateTime(noticeServedDetails.getNoticeOtherElectronicDateTime())
                .noticeOtherDateTime(noticeServedDetails.getNoticeOtherDateTime())
                .noticeOtherExplanation(noticeServedDetails.getNoticeOtherExplanation())
                .noticeDocuments(ListValueUtils.unwrapListItems(noticeServedDetails.getNoticeDocuments()));
        }
    }

    private void buildWalesNoticeServedDetails(WalesNoticeDetails walesNoticeDetails,
                                               TenancyLicence.TenancyLicenceBuilder tenancyLicence) {
        // Add notice served details for Wales
        if (walesNoticeDetails != null) {
            tenancyLicence.walesNoticeServed(walesNoticeDetails.getNoticeServed() != null
                ? YesOrNoToBoolean.convert(walesNoticeDetails.getNoticeServed()) : null);
            tenancyLicence.walesTypeOfNoticeServed(walesNoticeDetails.getTypeOfNoticeServed());
        }
    }

    private void buildWalesHousingActDetails(WalesHousingAct walesHousingAct,
                                             TenancyLicence.TenancyLicenceBuilder tenancyLicence) {
        // Add Wales Housing Act details
        if (walesHousingAct != null) {
            tenancyLicence.walesRegistered(walesHousingAct.getRegistered());
            tenancyLicence.walesRegistrationNumber(walesHousingAct.getRegistrationNumber());
            tenancyLicence.walesLicensed(walesHousingAct.getLicensed());
            tenancyLicence.walesLicenceNumber(walesHousingAct.getLicenceNumber());
            tenancyLicence.walesLicensedAgentAppointed(walesHousingAct.getLicensedAgentAppointed());
            tenancyLicence.walesAgentFirstName(walesHousingAct.getAgentFirstName());
            tenancyLicence.walesAgentLastName(walesHousingAct.getAgentLastName());
            tenancyLicence.walesAgentLicenceNumber(walesHousingAct.getAgentLicenceNumber());
            tenancyLicence.walesAgentAppointmentDate(walesHousingAct.getAgentAppointmentDate());
        }
    }

    private void buildWalesOccupationContractDetails(OccupationLicenceDetailsWales occupationLicenceDetailsWales,
                                                     TenancyLicence.TenancyLicenceBuilder tenancyLicence) {
        // Add Wales Occupation Contract/Licence details
        if (occupationLicenceDetailsWales != null) {
            tenancyLicence.occupationLicenceTypeWales(
                occupationLicenceDetailsWales.getOccupationLicenceTypeWales());
            tenancyLicence.walesOtherLicenceTypeDetails(
                occupationLicenceDetailsWales.getOtherLicenceTypeDetails());
            tenancyLicence.walesLicenceStartDate(
                occupationLicenceDetailsWales.getLicenceStartDate());
            tenancyLicence.walesLicenceDocuments(
                ListValueUtils.unwrapListItems(
                    occupationLicenceDetailsWales.getLicenceDocuments()));
        }
    }

    private static BigDecimal penceToPounds(String pence) {
        if (pence == null || pence.isBlank()) {
            return null;
        }
        return new BigDecimal(pence).movePointLeft(2);
    }
}
