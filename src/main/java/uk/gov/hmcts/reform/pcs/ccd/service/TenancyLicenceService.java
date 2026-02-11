package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseDataValidationException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TenancyLicenceService {

    public TenancyLicenceEntity createTenancyLicenceEntity(PCSCase pcsCase) {

        TenancyLicenceEntity tenancyLicenceEntity = new TenancyLicenceEntity();

        if (!hasTenancyTypeSet(pcsCase)) {
            return null;
        }

        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            OccupationLicenceDetailsWales occupationLicenceDetailsWales = pcsCase.getOccupationLicenceDetailsWales();
            if (occupationLicenceDetailsWales.getOccupationLicenceTypeWales() == null) {
                return null;
            }
            setWalesLicence(occupationLicenceDetailsWales, tenancyLicenceEntity);
        } else {
            TenancyLicenceDetails tenancyLicenceDetails = pcsCase.getTenancyLicenceDetails();
            if (tenancyLicenceDetails.getTypeOfTenancyLicence() == null) {
                return null;
            }
            setNonWalesLicence(tenancyLicenceDetails, tenancyLicenceEntity);
        }

        setRentDetails(pcsCase, tenancyLicenceEntity);

        return tenancyLicenceEntity;
    }

    private boolean hasTenancyTypeSet(PCSCase pcsCase) {
        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            return Optional.ofNullable(pcsCase.getOccupationLicenceDetailsWales())
                .map(OccupationLicenceDetailsWales::getOccupationLicenceTypeWales)
                .isPresent();
        } else {
            return Optional.ofNullable(pcsCase.getTenancyLicenceDetails())
                .map(TenancyLicenceDetails::getTypeOfTenancyLicence)
                .isPresent();
        }
    }

    private void setWalesLicence(OccupationLicenceDetailsWales occupationLicenceDetails,
                                 TenancyLicenceEntity tenancyLicenceEntity) {

        OccupationLicenceTypeWales occupationLicenceType = occupationLicenceDetails.getOccupationLicenceTypeWales();

        CombinedLicenceType combinedLicenceType = getCombinedLicenceType(occupationLicenceType);
        tenancyLicenceEntity.setType(combinedLicenceType);
        if (combinedLicenceType == CombinedLicenceType.OTHER) {
            tenancyLicenceEntity.setOtherTypeDetails(occupationLicenceDetails.getOtherLicenceTypeDetails());
        }

        tenancyLicenceEntity.setStartDate(occupationLicenceDetails.getLicenceStartDate());
    }

    private void setNonWalesLicence(TenancyLicenceDetails tenancyLicenceDetails,
                                    TenancyLicenceEntity tenancyLicenceEntity) {

        TenancyLicenceType tenancyLicenceType = tenancyLicenceDetails.getTypeOfTenancyLicence();

        CombinedLicenceType combinedLicenceType = getCombinedLicenceType(tenancyLicenceType);
        tenancyLicenceEntity.setType(combinedLicenceType);
        if (combinedLicenceType == CombinedLicenceType.OTHER) {
            tenancyLicenceEntity.setOtherTypeDetails(tenancyLicenceDetails.getDetailsOfOtherTypeOfTenancyLicence());
        }

        tenancyLicenceEntity.setStartDate(tenancyLicenceDetails.getTenancyLicenceDate());
    }

    private void setRentDetails(PCSCase pcsCase, TenancyLicenceEntity tenancyLicenceEntity) {
        RentDetails rentDetails = pcsCase.getRentDetails();

        if (rentDetails != null && rentDetails.getFrequency() != null) {
            tenancyLicenceEntity.setRentAmount(rentDetails.getCurrentRent());
            tenancyLicenceEntity.setRentPerDay(getDailyRentAmount(rentDetails));

            tenancyLicenceEntity.setRentFrequency(rentDetails.getFrequency());
            if (rentDetails.getFrequency() == RentPaymentFrequency.OTHER) {
                tenancyLicenceEntity.setOtherRentFrequency(rentDetails.getOtherFrequency());
            } else {
                tenancyLicenceEntity.setCalculatedDailyRentCorrect(rentDetails.getPerDayCorrect());
            }

        }
    }

    private CombinedLicenceType getCombinedLicenceType(TenancyLicenceType tenancyLicenceType) {
        return tenancyLicenceType != null ? tenancyLicenceType.getCombinedLicenceType() : null;
    }

    private CombinedLicenceType getCombinedLicenceType(OccupationLicenceTypeWales occupationLicenceType) {
        return occupationLicenceType != null ? occupationLicenceType.getCombinedLicenceType() : null;
    }

    private BigDecimal getDailyRentAmount(RentDetails rentDetails) {
        if (rentDetails.getFrequency() == RentPaymentFrequency.OTHER) {
            return rentDetails.getDailyCharge();
        } else if (rentDetails.getPerDayCorrect() == VerticalYesNo.YES){
            return rentDetails.getCalculatedDailyCharge();
        } else if (rentDetails.getPerDayCorrect() == VerticalYesNo.NO) {
            return rentDetails.getAmendedDailyCharge();
        } else {
            throw new CaseDataValidationException("Invalid rent details: " + rentDetails);
        }

    }

}
