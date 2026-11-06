package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentLedgerEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimWhoIsClaiming;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimantKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
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
    public static final String MULTIPLE_CLAIMANTS_NOT_ELIGIBLE = "MULTIPLE_CLAIMANTS_NOT_ELIGIBLE";
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
        validateDefendants(claim, errors);
        validateTenancy(claim.getTenancy(), errors);
        validateGrounds(claim, errors);
        validateNotice(claim.getNotice(), errors);
        validateRentArrears(claim.getRentArrears(), errors);
        validateCircumstances(claim.getCircumstances(), errors);
        validateOtherInformation(claim.getOtherInformation(), errors);
        validateSubmission(claim.getSubmission(), errors);

        return errors;
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
        if (claimant.getMultipleClaimants() == VerticalYesNo.YES) {
            errors.add(MULTIPLE_CLAIMANTS_NOT_ELIGIBLE);
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
        maxLength(errors, "claimant.emailAddress", claimant.getEmailAddress(), EMAIL_MAX);
        maxLength(errors, "claimant.phoneNumber", claimant.getPhoneNumber(), NAME_MAX);
        maxLength(errors, "claimant.textMessageNumber", claimant.getTextMessageNumber(), NAME_MAX);
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

    private static void validateTenancy(CitizenClaimTenancy tenancy, List<String> errors) {
        if (tenancy == null || tenancy.getTenancyType() == null) {
            errors.add(required("tenancy.tenancyType"));
            return;
        }
        if (tenancy.getTenancyType() == CitizenClaimTenancyType.OTHER) {
            requireText(errors, "tenancy.otherTenancyTypeDetails", tenancy.getOtherTenancyTypeDetails(), REASON_MAX);
        }
        maxLength(errors, "tenancy.noTenancyAgreementReason", tenancy.getNoTenancyAgreementReason(), REASON_MAX);
    }

    private static void validateGrounds(CitizenPossessionClaim claim, List<String> errors) {
        CitizenClaimGrounds grounds = claim.getGrounds();
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
        if (notice.getNoticeDate() == null) {
            errors.add(required("notice.noticeDate"));
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
        List<ListValue<CitizenClaimRentLedgerEntry>> ledger = rentArrears.getRentHistoryLedger();
        if (ledger != null) {
            for (int i = 0; i < ledger.size(); i++) {
                CitizenClaimRentLedgerEntry entry = ledger.get(i) != null ? ledger.get(i).getValue() : null;
                if (entry != null) {
                    maxLength(errors, "rentArrears.rentHistoryLedger[" + i + "].description",
                              entry.getDescription(), REASON_MAX);
                }
            }
        }
    }

    private static void validateCircumstances(CitizenClaimCircumstances circumstances, List<String> errors) {
        if (circumstances == null) {
            return;
        }
        maxLength(errors, "circumstances.defendantCircumstances",
                  circumstances.getDefendantCircumstances(), CIRCUMSTANCES_MAX);
        maxLength(errors, "circumstances.claimantCircumstances",
                  circumstances.getClaimantCircumstances(), CIRCUMSTANCES_MAX);
        if (circumstances.getUnderlesseeExists() == VerticalYesNo.YES) {
            requireText(errors, "circumstances.underlesseeName", circumstances.getUnderlesseeName(), NAME_MAX);
            errors.addAll(validateAddressLengths("circumstances.underlesseeAddress",
                                                 circumstances.getUnderlesseeAddress()));
        }
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
