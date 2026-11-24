package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimAdditionalClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimDefendant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNotice;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimOtherInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimWalesGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimWhoIsClaiming;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimantKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Final-submit checks for a citizen claim. Errors are stable codes the frontend routes on.
 */
@Component
public class CitizenPossessionClaimValidator {

    public static final String CLAIMANT_TYPE_NOT_ELIGIBLE = "CLAIMANT_TYPE_NOT_ELIGIBLE";
    public static final String CLAIM_AGAINST_TRESPASSERS_NOT_ELIGIBLE = "CLAIM_AGAINST_TRESPASSERS_NOT_ELIGIBLE";
    public static final String WHO_IS_CLAIMING_NOT_ELIGIBLE = "WHO_IS_CLAIMING_NOT_ELIGIBLE";
    public static final String RENT_ARREARS_GROUND_NOT_ALLOWED = "RENT_ARREARS_GROUND_NOT_ALLOWED";
    public static final String OTHER_GROUND_NOT_ALLOWED = "OTHER_GROUND_NOT_ALLOWED";

    private static final int NAME_MAX = 60;
    private static final int SHORT_TEXT_MAX = 250;
    private static final int EMAIL_MAX = 254;
    private static final int REASON_MAX = 500;
    private static final int CIRCUMSTANCES_MAX = 950;
    private static final int OTHER_REASONS_MAX = 6400;
    private static final int ADDRESS_LINE_MAX = 100;
    private static final int POSTCODE_MAX = 14;
    private static final Set<String> WALES_RENT_ARREARS_GROUNDS = Set.of(
        DiscretionaryGroundWales.RENT_ARREARS_S157.name(),
        MandatoryGroundWales.SERIOUS_ARREARS_PERIODIC_S181.name(),
        MandatoryGroundWales.SERIOUS_ARREARS_FIXED_TERM_S187.name());

    public List<String> validateEligibility(CitizenClaimEligibility eligibility) {
        List<String> errors = new ArrayList<>();
        if (eligibility == null || eligibility.getClaimantType() != ClaimantType.PRIVATE_LANDLORD) {
            errors.add(CLAIMANT_TYPE_NOT_ELIGIBLE);
        }
        if (eligibility != null && eligibility.getIsTrespass() == VerticalYesNo.YES) {
            errors.add(CLAIM_AGAINST_TRESPASSERS_NOT_ELIGIBLE);
        }
        if (eligibility != null && eligibility.getWhoIsClaiming() != null
            && eligibility.getWhoIsClaiming() != CitizenClaimWhoIsClaiming.MYSELF) {
            errors.add(WHO_IS_CLAIMING_NOT_ELIGIBLE);
        }
        return errors;
    }

    /**
     * Checks a claim already reduced to the answers that apply (see
     * {@link CitizenPossessionClaimApplicableAnswers}), so nothing on a branch the citizen did not
     * take is required or length-checked.
     */
    public List<String> validateForSubmit(CitizenPossessionClaim claim) {
        List<String> errors = new ArrayList<>(validateEligibility(claim.getEligibility()));

        validateClaimant(claim.getClaimant(), errors);
        validateAdditionalClaimants(claim, errors);
        validateDefendants(claim, errors);
        validateTenancy(claim.getTenancy(), CitizenPossessionClaimApplicableAnswers.isWales(claim), errors);
        validateGrounds(claim, errors);
        validateNotice(claim.getNotice(), errors);
        validateRentArrears(claim.getRentArrears(), errors);
        validateCircumstances(claim.getCircumstances(), errors);
        validateOtherInformation(claim.getOtherInformation(), errors);
        validateSubmission(claim.getSubmission(), errors);

        return errors;
    }

    /**
     * Whether a Wales ground that is claimed for rent arrears is chosen: section 157 rent arrears, or the
     * mandatory section 181 or 187 grounds.
     */
    public static boolean hasWalesRentArrearsGround(CitizenClaimGrounds grounds) {
        if (grounds == null) {
            return false;
        }
        return walesGroundCodes(grounds).stream().anyMatch(CitizenPossessionClaimValidator::isWalesRentArrearsGround);
    }

    public static boolean isWalesRentArrearsGround(String ground) {
        return WALES_RENT_ARREARS_GROUNDS.contains(ground);
    }

    private static <T> Set<T> setOrEmpty(Set<T> set) {
        return set != null ? set : Set.of();
    }

    public static Set<PrivateLandlordAssuredGround> allGrounds(CitizenClaimGrounds grounds) {
        Set<PrivateLandlordAssuredGround> all = new LinkedHashSet<>();
        if (grounds != null) {
            Stream.of(grounds.getRentArrearsGrounds(), grounds.getOtherGrounds())
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .filter(Objects::nonNull)
                .forEach(all::add);
        }
        return all;
    }

    private static void validateClaimant(CitizenClaimClaimant claimant, List<String> errors) {
        if (claimant == null) {
            errors.add(required("claimant"));
            return;
        }
        CitizenClaimantKind kind = claimant.getIndividualOrOrganisation();
        if (kind == null) {
            errors.add(required("claimant.individualOrOrganisation"));
        } else if (kind == CitizenClaimantKind.INDIVIDUAL) {
            requireText(errors, "claimant.firstName", claimant.getFirstName(), NAME_MAX);
            requireText(errors, "claimant.lastName", claimant.getLastName(), NAME_MAX);
            if (fitsIn(claimant.getFirstName(), NAME_MAX) && fitsIn(claimant.getLastName(), NAME_MAX)) {
                // The claimant is named "First Last" in the party's 60-character org_name column
                maxLength(errors, "claimant.name", claimant.getFirstName() + " " + claimant.getLastName(), NAME_MAX);
            }
        } else {
            requireText(errors, "claimant.organisationName", claimant.getOrganisationName(), NAME_MAX);
        }
        errors.addAll(requireAddress("claimant.address", claimant.getAddress()));
        errors.addAll(validateAddressLengths("claimant.address", claimant.getAddress()));
        // contactByEmail YES means the account email, NO a different one given in emailAddress
        if (claimant.getContactByEmail() != null) {
            requireText(errors, "claimant.emailAddress", claimant.getEmailAddress(), EMAIL_MAX);
        } else {
            maxLength(errors, "claimant.emailAddress", claimant.getEmailAddress(), EMAIL_MAX);
        }
        if (claimant.getContactByPhone() == VerticalYesNo.YES) {
            requireText(errors, "claimant.phoneNumber", claimant.getPhoneNumber(), NAME_MAX);
        } else {
            maxLength(errors, "claimant.phoneNumber", claimant.getPhoneNumber(), NAME_MAX);
        }
        maxLength(errors, "claimant.textMessageNumber", claimant.getTextMessageNumber(), NAME_MAX);
        // With calls=YES a missing number is already reported as the phone number
        if (claimant.getContactByText() == VerticalYesNo.YES
            && claimant.getContactByPhone() != VerticalYesNo.YES
            && CitizenPossessionClaimApplicableAnswers.effectiveTextNumber(claimant) == null) {
            errors.add(required("claimant.textMessageNumber"));
        }
    }

    private static void validateAdditionalClaimants(CitizenPossessionClaim claim, List<String> errors) {
        if (claim.getClaimant() == null || claim.getClaimant().getMultipleClaimants() != VerticalYesNo.YES) {
            return;
        }
        if (CollectionUtils.isEmpty(claim.getAdditionalClaimants())) {
            errors.add(required("additionalClaimants"));
            return;
        }
        List<CitizenClaimAdditionalClaimant> claimants = claim.getAdditionalClaimants().stream()
            .map(listValue -> listValue == null ? null : listValue.getValue())
            .toList();
        for (int i = 0; i < claimants.size(); i++) {
            String path = "additionalClaimants[" + i + "]";
            CitizenClaimAdditionalClaimant claimant = claimants.get(i);
            if (claimant == null) {
                errors.add(required(path));
                continue;
            }
            validateAdditionalClaimant(path, claimant, errors);
        }
    }

    private static void validateAdditionalClaimant(String path, CitizenClaimAdditionalClaimant claimant,
                                                   List<String> errors) {
        CitizenClaimantKind kind = claimant.getIndividualOrOrganisation();
        if (kind == null) {
            errors.add(required(path + ".individualOrOrganisation"));
        } else if (kind == CitizenClaimantKind.INDIVIDUAL) {
            requireText(errors, path + ".firstName", claimant.getFirstName(), NAME_MAX);
            requireText(errors, path + ".lastName", claimant.getLastName(), NAME_MAX);
            if (fitsIn(claimant.getFirstName(), NAME_MAX) && fitsIn(claimant.getLastName(), NAME_MAX)) {
                maxLength(errors, path + ".name", claimant.getFirstName() + " " + claimant.getLastName(), NAME_MAX);
            }
        } else {
            requireText(errors, path + ".organisationName", claimant.getOrganisationName(), NAME_MAX);
        }
        errors.addAll(requireAddress(path + ".address", claimant.getAddress()));
        errors.addAll(validateAddressLengths(path + ".address", claimant.getAddress()));
        maxLength(errors, path + ".emailAddress", claimant.getEmailAddress(), EMAIL_MAX);
        if (claimant.getContactByPhone() == null) {
            errors.add(required(path + ".contactByPhone"));
        } else if (claimant.getContactByPhone() == VerticalYesNo.YES) {
            requireText(errors, path + ".phoneNumber", claimant.getPhoneNumber(), NAME_MAX);
        }
        if (claimant.getContactByText() == null) {
            errors.add(required(path + ".contactByText"));
        } else if (claimant.getContactByText() == VerticalYesNo.YES) {
            if (StringUtils.isBlank(claimant.getTextMessageNumber())
                && claimant.getContactByPhone() != VerticalYesNo.YES) {
                errors.add(required(path + ".textMessageNumber"));
            }
            maxLength(errors, path + ".textMessageNumber", claimant.getTextMessageNumber(), NAME_MAX);
        }
    }

    private static void validateDefendants(CitizenPossessionClaim claim, List<String> errors) {
        if (CollectionUtils.isEmpty(claim.getDefendants())) {
            errors.add(required("defendants"));
            return;
        }
        List<CitizenClaimDefendant> defendants = claim.getDefendants().stream()
            .map(listValue -> listValue == null ? null : listValue.getValue())
            .toList();
        for (int i = 0; i < defendants.size(); i++) {
            String path = "defendants[" + i + "]";
            CitizenClaimDefendant defendant = defendants.get(i);
            if (defendant == null) {
                errors.add(required(path));
                continue;
            }
            if (defendant.getNameKnown() == null) {
                errors.add(required(path + ".nameKnown"));
            } else if (defendant.getNameKnown() == VerticalYesNo.YES) {
                requireText(errors, path + ".firstName", defendant.getFirstName(), NAME_MAX);
                requireText(errors, path + ".lastName", defendant.getLastName(), NAME_MAX);
            }
            if (defendant.getAddressKnown() == null) {
                errors.add(required(path + ".addressKnown"));
            } else if (defendant.getAddressKnown() == VerticalYesNo.YES) {
                if (defendant.getAddressSameAsProperty() == null) {
                    errors.add(required(path + ".addressSameAsProperty"));
                } else if (defendant.getAddressSameAsProperty() == VerticalYesNo.NO) {
                    errors.addAll(requireAddress(path + ".address", defendant.getAddress()));
                    errors.addAll(validateAddressLengths(path + ".address", defendant.getAddress()));
                }
            }
        }
    }

    private static void validateTenancy(CitizenClaimTenancy tenancy, boolean wales, List<String> errors) {
        if (wales) {
            validateOccupationContract(tenancy, errors);
            return;
        }
        if (tenancy == null || tenancy.getTenancyType() == null) {
            errors.add(required("tenancy.tenancyType"));
            return;
        }
        if (tenancy.getTenancyType() == CitizenClaimTenancyType.OTHER) {
            requireText(errors, "tenancy.otherTenancyTypeDetails", tenancy.getOtherTenancyTypeDetails(), REASON_MAX);
        }
        maxLength(errors, "tenancy.noTenancyAgreementReason", tenancy.getNoTenancyAgreementReason(), REASON_MAX);
    }

    private static void validateOccupationContract(CitizenClaimTenancy tenancy, List<String> errors) {
        if (tenancy == null || tenancy.getOccupationContractType() == null) {
            errors.add(required("tenancy.occupationContractType"));
            return;
        }
        if (tenancy.getOccupationContractType() == OccupationLicenceTypeWales.OTHER) {
            requireText(errors, "tenancy.otherOccupationContractDetails", tenancy.getOtherOccupationContractDetails(),
                        REASON_MAX);
        }
        maxLength(errors, "tenancy.noTenancyAgreementReason", tenancy.getNoTenancyAgreementReason(), REASON_MAX);
        validateRequiredDocument(errors, "tenancy.hasEnergyPerformanceCertificate",
                                 tenancy.getHasEnergyPerformanceCertificate(),
                                 "tenancy.noEnergyPerformanceCertificateReason",
                                 tenancy.getNoEnergyPerformanceCertificateReason());
        validateRequiredDocument(errors, "tenancy.hasGasSafetyReport", tenancy.getHasGasSafetyReport(),
                                 "tenancy.noGasSafetyReportReason", tenancy.getNoGasSafetyReportReason());
        validateRequiredDocument(errors, "tenancy.hasElectricalInstallationConditionReport",
                                 tenancy.getHasElectricalInstallationConditionReport(),
                                 "tenancy.noElectricalInstallationConditionReason",
                                 tenancy.getNoElectricalInstallationConditionReason());
    }

    private static void validateRequiredDocument(List<String> errors, String answerPath, VerticalYesNo answer,
                                                 String reasonPath, String reason) {
        if (answer == null) {
            errors.add(required(answerPath));
        } else if (answer == VerticalYesNo.NO) {
            requireText(errors, reasonPath, reason, REASON_MAX);
        }
    }

    private static void validateGrounds(CitizenPossessionClaim claim, List<String> errors) {
        if (CitizenPossessionClaimApplicableAnswers.isWales(claim)) {
            validateWalesGrounds(claim, errors);
            return;
        }
        CitizenClaimGrounds grounds = claim.getGrounds();
        if (CitizenPossessionClaimApplicableAnswers.isExcludedTenancy(claim)) {
            validateExcludedGrounds(claim, errors);
            return;
        }
        Set<PrivateLandlordAssuredGround> allGrounds = allGrounds(grounds);
        if (grounds == null || allGrounds.isEmpty()) {
            errors.add(required("grounds"));
            return;
        }

        Set<PrivateLandlordAssuredGround> rentArrearsGrounds = grounds.getRentArrearsGrounds();
        if (rentArrearsGrounds != null) {
            if (rentArrearsGrounds.stream().anyMatch(Objects::isNull)) {
                errors.add(required("grounds.rentArrearsGrounds"));
            }
            if (rentArrearsGrounds.stream().filter(Objects::nonNull).anyMatch(ground -> !ground.isRentArrears())) {
                errors.add(RENT_ARREARS_GROUND_NOT_ALLOWED);
            }
        }
        Set<PrivateLandlordAssuredGround> otherGrounds = grounds.getOtherGrounds();
        if (otherGrounds != null) {
            if (otherGrounds.stream().anyMatch(Objects::isNull)) {
                errors.add(required("grounds.otherGrounds"));
            }
            if (otherGrounds.stream().filter(Objects::nonNull).anyMatch(PrivateLandlordAssuredGround::isRentArrears)) {
                errors.add(OTHER_GROUND_NOT_ALLOWED);
            }
        }

        if (allGrounds.stream().anyMatch(PrivateLandlordAssuredGround::isRentArrears)) {
            CitizenClaimRentArrears rentArrears = claim.getRentArrears();
            if (rentArrears == null || rentArrears.getCurrentArrears() == null) {
                errors.add(required("rentArrears.currentArrears"));
            }
        }

        if (allGrounds.contains(PrivateLandlordAssuredGround.OTHER)) {
            requireText(errors, "grounds.otherGroundsDetails", grounds.getOtherGroundsDetails(), REASON_MAX);
        }

        Map<PrivateLandlordAssuredGround, String> reasons = groundReasons(grounds);
        allGrounds.stream()
            .filter(ground -> !ground.isRentArrears() && ground != PrivateLandlordAssuredGround.OTHER)
            .forEach(ground -> requireText(errors, "grounds.groundReasons." + ground.name(), reasons.get(ground),
                                           REASON_MAX));
    }

    private static void validateExcludedGrounds(CitizenPossessionClaim claim, List<String> errors) {
        CitizenClaimGrounds grounds = claim.getGrounds();
        if (grounds == null || grounds.getHasExcludedGrounds() == null) {
            errors.add(required("grounds.hasExcludedGrounds"));
            return;
        }
        if (grounds.getHasExcludedGrounds() == VerticalYesNo.YES) {
            Set<IntroductoryDemotedOrOtherGrounds> chosen = grounds.getExcludedGrounds();
            if (chosen == null || chosen.isEmpty() || chosen.stream().anyMatch(Objects::isNull)) {
                errors.add(required("grounds.excludedGrounds"));
            } else {
                if (chosen.contains(IntroductoryDemotedOrOtherGrounds.OTHER)) {
                    requireText(errors, "grounds.excludedOtherGroundDetails", grounds.getExcludedOtherGroundDetails(),
                                REASON_MAX);
                }
                if (chosen.contains(IntroductoryDemotedOrOtherGrounds.RENT_ARREARS)) {
                    CitizenClaimRentArrears rentArrears = claim.getRentArrears();
                    if (rentArrears == null || rentArrears.getCurrentArrears() == null) {
                        errors.add(required("rentArrears.currentArrears"));
                    }
                }
            }
        }
        requireText(errors, "grounds.excludedReason", grounds.getExcludedReason(), REASON_MAX);
    }

    private static void validateWalesGrounds(CitizenPossessionClaim claim, List<String> errors) {
        CitizenClaimGrounds grounds = claim.getGrounds();
        Set<String> chosen = walesGroundCodes(grounds);
        boolean other = grounds != null && grounds.getOtherGrounds() != null
            && grounds.getOtherGrounds().contains(PrivateLandlordAssuredGround.OTHER);
        if (chosen.isEmpty() && !other) {
            errors.add(required("grounds"));
            return;
        }

        if (hasWalesRentArrearsGround(grounds)) {
            CitizenClaimRentArrears rentArrears = claim.getRentArrears();
            if (rentArrears == null || rentArrears.getCurrentArrears() == null) {
                errors.add(required("rentArrears.currentArrears"));
            }
        }
        if (other) {
            requireText(errors, "grounds.otherGroundsDetails", grounds.getOtherGroundsDetails(), REASON_MAX);
        }

        Map<String, String> reasons = walesGroundReasons(grounds);
        chosen.stream()
            .filter(code -> !isWalesRentArrearsGround(code))
            .forEach(code -> requireText(errors, "grounds.walesGroundReasons." + code, reasons.get(code),
                                         REASON_MAX));
    }

    /**
     * The names of the chosen Wales grounds, discretionary before mandatory.
     */
    public static Set<String> walesGroundCodes(CitizenClaimGrounds grounds) {
        Set<String> codes = new LinkedHashSet<>();
        if (grounds != null) {
            setOrEmpty(grounds.getWalesDiscretionaryGrounds()).stream()
                .filter(Objects::nonNull).sorted().forEach(ground -> codes.add(ground.name()));
            setOrEmpty(grounds.getWalesMandatoryGrounds()).stream()
                .filter(Objects::nonNull).sorted().forEach(ground -> codes.add(ground.name()));
        }
        return codes;
    }

    /**
     * The reason given for each Wales ground; the last one wins if a ground is listed twice.
     */
    public static Map<String, String> walesGroundReasons(CitizenClaimGrounds grounds) {
        Map<String, String> reasons = new LinkedHashMap<>();
        if (grounds == null || grounds.getWalesGroundReasons() == null) {
            return reasons;
        }
        grounds.getWalesGroundReasons().stream()
            .filter(Objects::nonNull)
            .map(ListValue::getValue)
            .filter(reason -> reason != null && StringUtils.isNotBlank(reason.getGround()))
            .filter(reason -> StringUtils.isNotBlank(reason.getReason()))
            .forEach((CitizenClaimWalesGroundReason reason) -> reasons.put(reason.getGround(), reason.getReason()));
        return reasons;
    }

    /**
     * The reason given for each ground; the last one wins if a ground is listed twice.
     */
    public static Map<PrivateLandlordAssuredGround, String> groundReasons(CitizenClaimGrounds grounds) {
        Map<PrivateLandlordAssuredGround, String> reasons = new EnumMap<>(PrivateLandlordAssuredGround.class);
        if (grounds == null || grounds.getGroundReasons() == null) {
            return reasons;
        }
        grounds.getGroundReasons().stream()
            .filter(Objects::nonNull)
            .map(ListValue::getValue)
            .filter(reason -> reason != null && reason.getGround() != null)
            .filter(reason -> StringUtils.isNotBlank(reason.getReason()))
            .forEach(reason -> reasons.put(reason.getGround(), reason.getReason()));
        return reasons;
    }

    private static void validateNotice(CitizenClaimNotice notice, List<String> errors) {
        if (notice == null || notice.getNoticeServed() == null) {
            errors.add(required("notice.noticeServed"));
            return;
        }
        if (notice.getNoticeServed() == VerticalYesNo.YES) {
            validateServedNotice(notice, errors);
        } else {
            maxLength(errors, "notice.noNoticeReason", notice.getNoNoticeReason(), REASON_MAX);
        }
        maxLength(errors, "notice.noticeUnavailableReason", notice.getNoticeUnavailableReason(), REASON_MAX);
        maxLength(errors, "notice.handedToName", notice.getHandedToName(), SHORT_TEXT_MAX);
        maxLength(errors, "notice.emailedTo", notice.getEmailedTo(), EMAIL_MAX);
        maxLength(errors, "notice.otherElectronicDetails", notice.getOtherElectronicDetails(), SHORT_TEXT_MAX);
        maxLength(errors, "notice.otherMethodDetails", notice.getOtherMethodDetails(), SHORT_TEXT_MAX);
        if (notice.getNoticeType() == CitizenClaimNoticeType.OTHER) {
            requireText(errors, "notice.noticeTypeOther", notice.getNoticeTypeOther(), NAME_MAX);
        } else {
            maxLength(errors, "notice.noticeTypeOther", notice.getNoticeTypeOther(), NAME_MAX);
        }
    }

    private static void validateServedNotice(CitizenClaimNotice notice, List<String> errors) {
        if (notice.getNoticeType() == null) {
            errors.add(required("notice.noticeType"));
        }
        Set<CitizenClaimNoticeMethod> methods = notice.getNoticeMethods();
        if (methods == null || methods.isEmpty() || methods.stream().anyMatch(Objects::isNull)) {
            errors.add(required("notice.noticeMethods"));
        }
        if (methods == null) {
            return;
        }
        methods.stream().filter(Objects::nonNull).sorted().forEach(method -> {
            switch (method) {
                case FIRST_CLASS_POST -> requireDate(errors, "notice.postedDate", notice.getPostedDate());
                case PERMITTED_PLACE -> requireDate(errors, "notice.deliveredDate", notice.getDeliveredDate());
                case PERSONALLY_HANDED -> {
                    requirePresent(errors, "notice.handedToName", notice.getHandedToName());
                    requireDate(errors, "notice.handedDate", notice.getHandedDate());
                }
                case EMAIL -> {
                    requirePresent(errors, "notice.emailedTo", notice.getEmailedTo());
                    requireDate(errors, "notice.emailedDate", notice.getEmailedDate());
                }
                case OTHER_ELECTRONIC -> {
                    requirePresent(errors, "notice.otherElectronicDetails", notice.getOtherElectronicDetails());
                    requireDate(errors, "notice.otherElectronicDate", notice.getOtherElectronicDate());
                }
                case OTHER -> {
                    requirePresent(errors, "notice.otherMethodDetails", notice.getOtherMethodDetails());
                    requireDate(errors, "notice.otherMethodDate", notice.getOtherMethodDate());
                }
            }
        });
    }

    // Length is checked for every notice field in validateNotice, served or not
    private static void requirePresent(List<String> errors, String path, String value) {
        if (StringUtils.isBlank(value)) {
            errors.add(required(path));
        }
    }

    private static boolean fitsIn(String value, int max) {
        return StringUtils.isNotBlank(value) && value.length() <= max;
    }

    private static void requireDate(List<String> errors, String path, LocalDate date) {
        if (date == null) {
            errors.add(required(path));
        }
    }

    private static void validateRentArrears(CitizenClaimRentArrears rentArrears, List<String> errors) {
        if (rentArrears == null) {
            return;
        }
        maxLength(errors, "rentArrears.otherFrequencyDetails", rentArrears.getOtherFrequencyDetails(), NAME_MAX);
        maxLength(errors, "rentArrears.recoverySteps", rentArrears.getRecoverySteps(), REASON_MAX);
    }

    private static void validateCircumstances(CitizenClaimCircumstances circumstances, List<String> errors) {
        if (circumstances == null) {
            return;
        }
        maxLength(errors, "circumstances.defendantCircumstances",
                  circumstances.getDefendantCircumstances(), CIRCUMSTANCES_MAX);
        maxLength(errors, "circumstances.claimantCircumstances",
                  circumstances.getClaimantCircumstances(), CIRCUMSTANCES_MAX);
    }

    private static void validateOtherInformation(CitizenClaimOtherInformation otherInformation, List<String> errors) {
        if (otherInformation == null || otherInformation.getLanguageUsed() == null) {
            errors.add(required("otherInformation.languageUsed"));
            return;
        }
        maxLength(errors, "otherInformation.otherReasonsDetail",
                  otherInformation.getOtherReasonsDetail(), OTHER_REASONS_MAX);
    }

    private static void validateSubmission(CitizenClaimSubmission submission, List<String> errors) {
        if (submission == null || submission.getStatementOfTruthAgreed() != VerticalYesNo.YES) {
            errors.add(required("submission.statementOfTruthAgreed"));
            return;
        }
        requireText(errors, "submission.statementOfTruthFullName", submission.getStatementOfTruthFullName(), 100);
        maxLength(errors, "submission.statementOfTruthPosition", submission.getStatementOfTruthPosition(), 100);
    }

    private static void requireText(List<String> errors, String path, String value, int max) {
        if (StringUtils.isBlank(value)) {
            errors.add(required(path));
        } else {
            maxLength(errors, path, value, max);
        }
    }

    private static void maxLength(List<String> errors, String path, String value, int max) {
        if (value != null && value.length() > max) {
            errors.add("TOO_LONG:" + path);
        }
    }

    /**
     * Address fields longer than the address table's columns, as TOO_LONG errors.
     */
    public static List<String> validateAddressLengths(String path, AddressUK address) {
        List<String> errors = new ArrayList<>();
        if (address == null) {
            return errors;
        }
        maxLength(errors, path + ".AddressLine1", address.getAddressLine1(), ADDRESS_LINE_MAX);
        maxLength(errors, path + ".AddressLine2", address.getAddressLine2(), ADDRESS_LINE_MAX);
        maxLength(errors, path + ".AddressLine3", address.getAddressLine3(), ADDRESS_LINE_MAX);
        maxLength(errors, path + ".PostTown", address.getPostTown(), ADDRESS_LINE_MAX);
        maxLength(errors, path + ".County", address.getCounty(), ADDRESS_LINE_MAX);
        maxLength(errors, path + ".Country", address.getCountry(), ADDRESS_LINE_MAX);
        maxLength(errors, path + ".PostCode", address.getPostCode(), POSTCODE_MAX);
        return errors;
    }

    /**
     * REQUIRED errors for a missing address, or for its first line or postcode.
     */
    public static List<String> requireAddress(String path, AddressUK address) {
        List<String> errors = new ArrayList<>();
        if (address == null) {
            errors.add(required(path));
            return errors;
        }
        if (StringUtils.isBlank(address.getAddressLine1())) {
            errors.add(required(path + ".AddressLine1"));
        }
        if (StringUtils.isBlank(address.getPostCode())) {
            errors.add(required(path + ".PostCode"));
        }
        return errors;
    }

    private static String required(String path) {
        return "REQUIRED:" + path;
    }

}
