package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

import java.math.BigDecimal;

@Service
public class TenancyLicenceService {

    public TenancyLicenceEntity buildTenancyLicenceEntity(PCSCase pcsCase) {

        TenancyLicenceEntity tenancyLicenceEntity = new TenancyLicenceEntity();

        TenancyLicenceDetails tenancyLicenceDetails = pcsCase.getTenancyLicenceDetails();
        TenancyLicenceType tenancyLicenceType = tenancyLicenceDetails.getTypeOfTenancyLicence();
        tenancyLicenceEntity.setType(tenancyLicenceType);
        if (tenancyLicenceType == TenancyLicenceType.OTHER) {
            tenancyLicenceEntity.setOtherTypeDetails(tenancyLicenceDetails.getDetailsOfOtherTypeOfTenancyLicence());
        }

        tenancyLicenceEntity.setStartDate(tenancyLicenceDetails.getTenancyLicenceDate());

        RentDetails rentDetails = pcsCase.getRentDetails();

        if (rentDetails != null && rentDetails.getFrequency() != null) {
            tenancyLicenceEntity.setRentAmount(rentDetails.getCurrentRent());
            tenancyLicenceEntity.setRentPerDay(getDailyRentAmount(rentDetails));
            tenancyLicenceEntity.setRentFrequency(rentDetails.getFrequency());

            if (rentDetails.getFrequency() == RentPaymentFrequency.OTHER) {
                tenancyLicenceEntity.setOtherRentFrequency(rentDetails.getOtherFrequency());
            }
        }

        return tenancyLicenceEntity;
    }

    public TenancyLicence buildTenancyLicence(PCSCase pcsCase) {
        TenancyLicenceDetails tenancyDetails = pcsCase.getTenancyLicenceDetails();
        TenancyLicence.TenancyLicenceBuilder tenancyLicenceBuilder = TenancyLicence.builder()
            .supportingDocuments(ListValueUtils.unwrapListItems(
                    tenancyDetails != null ? tenancyDetails.getTenancyLicenceDocuments() : null))
            .arrearsJudgmentWanted(YesOrNoConverter.toBoolean(pcsCase.getArrearsJudgmentWanted()));

        tenancyLicenceBuilder.noticeServed(YesOrNoConverter.toBoolean(pcsCase.getNoticeServed()));

        buildRentArrearsSection(pcsCase.getRentArrears(), tenancyLicenceBuilder);

        buildNoticeServedDetails(pcsCase.getNoticeServedDetails(), tenancyLicenceBuilder);

        buildWalesNoticeServedDetails(pcsCase.getWalesNoticeDetails(), tenancyLicenceBuilder);

        buildWalesOccupationContractDetails(pcsCase.getOccupationLicenceDetailsWales(), tenancyLicenceBuilder);

        return tenancyLicenceBuilder.build();
    }

    private BigDecimal getDailyRentAmount(RentDetails rentDetails) {
        BigDecimal[] fieldValues = {
            rentDetails.getAmendedDailyCharge(),
            rentDetails.getCalculatedDailyCharge(),
            rentDetails.getDailyCharge()
        };
        for (BigDecimal value : fieldValues) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private void buildRentArrearsSection(RentArrearsSection rentArrears,
                                         TenancyLicence.TenancyLicenceBuilder tenancyLicenceBuilder) {
        if (rentArrears != null) {
            tenancyLicenceBuilder
                    .rentStatementDocuments(ListValueUtils.unwrapListItems(rentArrears.getStatementDocuments()))
                    .totalRentArrears(rentArrears.getTotal())
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
                ? YesOrNoConverter.toBoolean(walesNoticeDetails.getNoticeServed()) : null);
            tenancyLicence.walesTypeOfNoticeServed(walesNoticeDetails.getTypeOfNoticeServed());
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
}
