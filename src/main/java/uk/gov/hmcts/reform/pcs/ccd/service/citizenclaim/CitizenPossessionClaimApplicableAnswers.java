package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimDefendant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNotice;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimOtherInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentArrears;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentProofType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimantKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import java.util.ArrayList;
import java.util.Set;

/**
 * The citizen's yes/no and choice answers decide which of their other answers apply. Validation and
 * the final-submit mapping both work on the claim this returns, so answers on a branch the citizen
 * turned away from (or left behind by changing an answer) are neither checked nor written.
 */
public final class CitizenPossessionClaimApplicableAnswers {

    // Same serialisation as the draft store, so the copy is exactly what a saved draft would read back as
    private static final ObjectMapper DRAFT_MAPPER = new JacksonConfiguration().draftCaseDataObjectMapper();

    private CitizenPossessionClaimApplicableAnswers() {
    }

    /**
     * A deep copy of the claim holding only the answers that apply. The original is not changed.
     */
    public static CitizenPossessionClaim of(CitizenPossessionClaim claim) {
        CitizenPossessionClaim applicable = DRAFT_MAPPER.convertValue(claim, CitizenPossessionClaim.class);

        reduceClaimant(applicable.getClaimant());
        reduceDefendants(applicable);
        reduceTenancy(applicable.getTenancy());
        reduceGrounds(applicable.getGrounds());
        if (!hasRentArrearsGround(applicable)) {
            applicable.setRentArrears(null);
        }
        reduceRentArrears(applicable.getRentArrears());
        applicable.setNotice(reduceNotice(applicable.getNotice()));
        reduceCircumstances(applicable.getCircumstances());
        reduceOtherInformation(applicable.getOtherInformation());

        return applicable;
    }

    public static boolean hasRentArrearsGround(CitizenPossessionClaim applicable) {
        return CitizenPossessionClaimValidator.allGrounds(applicable.getGrounds()).stream()
            .anyMatch(PrivateLandlordAssuredGround::isRentArrears);
    }

    private static void reduceClaimant(CitizenClaimClaimant claimant) {
        if (claimant == null) {
            return;
        }
        if (claimant.getIndividualOrOrganisation() == CitizenClaimantKind.ORGANISATION) {
            claimant.setFirstName(null);
            claimant.setLastName(null);
        } else if (claimant.getIndividualOrOrganisation() == CitizenClaimantKind.INDIVIDUAL) {
            claimant.setOrganisationName(null);
        }
        if (claimant.getContactByPhone() != VerticalYesNo.YES) {
            claimant.setPhoneNumber(null);
        }
        if (claimant.getContactByText() != VerticalYesNo.YES) {
            claimant.setTextMessageNumber(null);
        }
    }

    private static void reduceDefendants(CitizenPossessionClaim claim) {
        if (claim.getDefendants() == null) {
            return;
        }
        if (claim.getAdditionalDefendants() != VerticalYesNo.YES && claim.getDefendants().size() > 1) {
            claim.setDefendants(new ArrayList<>(claim.getDefendants().subList(0, 1)));
        }
        claim.getDefendants().forEach(item -> {
            CitizenClaimDefendant defendant = item != null ? item.getValue() : null;
            if (defendant == null) {
                return;
            }
            if (defendant.getNameKnown() != VerticalYesNo.YES) {
                defendant.setFirstName(null);
                defendant.setLastName(null);
            }
            if (defendant.getAddressKnown() != VerticalYesNo.YES) {
                defendant.setAddressSameAsProperty(null);
                defendant.setAddress(null);
            } else if (defendant.getAddressSameAsProperty() == VerticalYesNo.YES) {
                defendant.setAddress(null);
            }
        });
    }

    private static void reduceTenancy(CitizenClaimTenancy tenancy) {
        if (tenancy == null) {
            return;
        }
        if (tenancy.getTenancyType() != CitizenClaimTenancyType.OTHER) {
            tenancy.setOtherTenancyTypeDetails(null);
        }
        if (tenancy.getTenancyAgreementAvailable() == VerticalYesNo.NO) {
            tenancy.setTenancyAgreementDocuments(null);
        } else {
            tenancy.setNoTenancyAgreementReason(null);
        }
    }

    private static void reduceGrounds(CitizenClaimGrounds grounds) {
        if (grounds == null) {
            return;
        }
        if (grounds.getHasRentArrearsGrounds() == VerticalYesNo.NO) {
            grounds.setRentArrearsGrounds(null);
        }
        if (grounds.getHasOtherGrounds() == VerticalYesNo.NO) {
            grounds.setOtherGrounds(null);
        }
        Set<PrivateLandlordAssuredGround> selected = CitizenPossessionClaimValidator.allGrounds(grounds);
        if (!selected.contains(PrivateLandlordAssuredGround.OTHER)) {
            grounds.setOtherGroundsDetails(null);
        }
        if (grounds.getGroundReasons() != null) {
            grounds.setGroundReasons(grounds.getGroundReasons().stream()
                                         .filter(item -> item == null || item.getValue() == null
                                             || item.getValue().getGround() == null
                                             || selected.contains(item.getValue().getGround()))
                                         .toList());
        }
    }

    private static void reduceRentArrears(CitizenClaimRentArrears rentArrears) {
        if (rentArrears == null) {
            return;
        }
        if (rentArrears.getRentFrequency() != RentPaymentFrequency.OTHER) {
            rentArrears.setOtherFrequencyDetails(null);
        }
        CitizenClaimRentProofType proofType = rentArrears.getProofType();
        if (proofType != CitizenClaimRentProofType.UPLOAD_STATEMENT) {
            rentArrears.setRentStatementDocuments(null);
        }
        if (proofType != CitizenClaimRentProofType.LEDGER) {
            rentArrears.setRentHistoryLedger(null);
        }
        if (proofType != CitizenClaimRentProofType.TABLE) {
            rentArrears.setRentHistoryTable(null);
        }
    }

    private static CitizenClaimNotice reduceNotice(CitizenClaimNotice notice) {
        if (notice == null) {
            return null;
        }
        if (notice.getNoticeServed() != VerticalYesNo.YES) {
            return CitizenClaimNotice.builder()
                .noticeServed(notice.getNoticeServed())
                .noNoticeReason(notice.getNoNoticeReason())
                .build();
        }
        notice.setNoNoticeReason(null);
        if (notice.getNoticeType() != CitizenClaimNoticeType.OTHER) {
            notice.setNoticeTypeOther(null);
        }
        Set<CitizenClaimNoticeMethod> methods =
            notice.getNoticeMethods() != null ? notice.getNoticeMethods() : Set.of();
        if (!methods.contains(CitizenClaimNoticeMethod.FIRST_CLASS_POST)) {
            notice.setPostedDate(null);
        }
        if (!methods.contains(CitizenClaimNoticeMethod.PERMITTED_PLACE)) {
            notice.setDeliveredDate(null);
        }
        if (!methods.contains(CitizenClaimNoticeMethod.PERSONALLY_HANDED)) {
            notice.setHandedToName(null);
            notice.setHandedDate(null);
        }
        if (!methods.contains(CitizenClaimNoticeMethod.EMAIL)) {
            notice.setEmailedTo(null);
            notice.setEmailedDate(null);
        }
        if (!methods.contains(CitizenClaimNoticeMethod.OTHER_ELECTRONIC)) {
            notice.setOtherElectronicDetails(null);
            notice.setOtherElectronicDate(null);
        }
        if (!methods.contains(CitizenClaimNoticeMethod.OTHER)) {
            notice.setOtherMethodDetails(null);
            notice.setOtherMethodDate(null);
        }
        if (notice.getNoticeAvailable() == VerticalYesNo.NO) {
            notice.setNoticeDocuments(null);
        } else {
            notice.setNoticeUnavailableReason(null);
        }
        return notice;
    }

    private static void reduceCircumstances(CitizenClaimCircumstances circumstances) {
        if (circumstances != null && circumstances.getUnderlesseeExists() != VerticalYesNo.YES) {
            circumstances.setUnderlesseeName(null);
            circumstances.setUnderlesseeAddress(null);
        }
    }

    private static void reduceOtherInformation(CitizenClaimOtherInformation otherInformation) {
        if (otherInformation != null && otherInformation.getHasOtherReasons() != VerticalYesNo.YES) {
            otherInformation.setOtherReasonsDetail(null);
        }
    }

}
