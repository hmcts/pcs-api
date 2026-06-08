package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackGround;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatGroundLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatLongDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatNoticeTime;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatRentDescription;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatTenancyLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isNo;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isPopulated;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isYes;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.toClaimPackAddress;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.toLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.yesOrNoToVertical;

/**
 * Builds {@link ClaimPackFormPayload} from a {@link PcsCaseEntity}.
 *
 * <p>One {@code mapXxx} method per source entity; each takes that entity (nullable for optional
 * relations) plus the payload builder and writes its fields. {@link #build(PcsCaseEntity)} runs
 * them in turn. Fields with no data source yet are left null or false, so the template's
 * conditional blocks do not render them.</p>
 */
@Service
public class ClaimPackPayloadBuilder {

    private static final Set<CombinedLicenceType> INTRO_DEMOTED_OTHER_TYPES = Set.of(
        CombinedLicenceType.INTRODUCTORY_TENANCY,
        CombinedLicenceType.DEMOTED_TENANCY,
        CombinedLicenceType.OTHER
    );

    private final CaseReferenceFormatter caseReferenceFormatter;
    private final ClaimPackPartyMapper partyMapper;

    public ClaimPackPayloadBuilder(CaseReferenceFormatter caseReferenceFormatter,
                                   ClaimPackPartyMapper partyMapper) {
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.partyMapper = partyMapper;
    }

    public ClaimPackFormPayload build(PcsCaseEntity pcsCase) {
        final ClaimEntity claim = pcsCase.getClaims().getFirst();
        final ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder = ClaimPackFormPayload.builder();

        mapCase(pcsCase, payloadBuilder);
        mapClaim(claim, payloadBuilder);

        final List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        final List<PartyEntity> defendants = partiesByRole(claim, PartyRole.DEFENDANT);
        partyMapper.mapClaimant(claimants, payloadBuilder);
        partyMapper.mapDefendants(defendants, pcsCase.getPropertyAddress(), payloadBuilder);
        partyMapper.mapUnderlessees(partiesByRole(claim, PartyRole.UNDERLESSEE_OR_MORTGAGEE), payloadBuilder);

        mapGrounds(claim.getClaimGrounds(), payloadBuilder);
        mapNotice(claim.getNoticeOfPossession(), payloadBuilder);
        mapTenancyLicence(pcsCase.getTenancyLicence(), payloadBuilder);
        mapRentArrears(claim.getRentArrears(), payloadBuilder);
        mapAsbProhibitedConduct(claim.getAsbProhibitedConductEntity(), payloadBuilder);
        mapPossessionAlternatives(claim.getPossessionAlternativesEntity(), payloadBuilder);
        mapStatementOfTruth(claim.getStatementOfTruth(), payloadBuilder);

        partyMapper.mapCaseName(claimants, defendants, payloadBuilder);
        mapClaimDetailsShowFlags(pcsCase, claim, payloadBuilder);

        return payloadBuilder.build();
    }

    /**
     * Section visibility flags that depend on country, tenancy, grounds and notice, so they are
     * computed after the individual mappers have populated their source data.
     */
    private void mapClaimDetailsShowFlags(PcsCaseEntity pcsCase, ClaimEntity claim,
                                          ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        boolean isWales = isWalesClaim(pcsCase);
        boolean isEngland = !isWales;
        boolean isIntroDemotedOther = isIntroDemotedOtherTenancy(pcsCase.getTenancyLicence());
        // Strip the "No grounds" sentinel (written when the claimant answered "No") before deriving
        // the flags below, otherwise it makes the set non-empty and masks hasNoGrounds.
        List<ClaimGroundEntity> grounds = realGrounds(claim.getClaimGrounds());
        boolean hasNoGrounds = grounds.isEmpty();
        boolean hasOtherGround = anyGroundIsOther(grounds);
        boolean hasAbsoluteGround = anyGroundHasCode(grounds, "ABSOLUTE_GROUNDS");
        boolean hasWalesAsbGround = anyGroundHasCode(grounds, "ANTISOCIAL_BEHAVIOUR_S157");
        boolean noticeServedYes = isNoticeServedYes(claim.getNoticeOfPossession());

        // The grounds Yes/No question has no country qualifier, only the tenancy-type check;
        // the description and why-claiming rows below are England-only.
        payloadBuilder.showGroundsYesNoQuestion(isIntroDemotedOther);
        payloadBuilder.showDescriptionOfGrounds(isEngland && isIntroDemotedOther && hasOtherGround);
        // Shown when there are no grounds, absolute grounds, or an Other ground.
        payloadBuilder.showWhyClaimingPossession(
            isEngland && isIntroDemotedOther && (hasNoGrounds || hasAbsoluteGround || hasOtherGround));
        payloadBuilder.showAsbSection(isWales && hasWalesAsbGround);
        payloadBuilder.showNoticeType(isWales && noticeServedYes);
        payloadBuilder.showPcscSection(isWales);
        payloadBuilder.showRequiredDocumentsSection(isWales);
        // The exempt-landlord question comes from the Housing (Wales) Act 2014, so it is Wales-only.
        payloadBuilder.showExemptLandlordQuestion(isWales);
        // Tenancy-copy rows are England-only (Wales never captures the answer) and show only when
        // the claimant answered, so an unanswered question hides the row instead of a blank value.
        TenancyLicenceEntity tenancyLicence = pcsCase.getTenancyLicence();
        boolean tenancyCopyAnswered = tenancyLicence != null && tenancyLicence.getHasCopyOfTenancyLicence() != null;
        payloadBuilder.showTenancyUploadedQuestion(isEngland && tenancyCopyAnswered);
    }

    private static boolean isWalesClaim(PcsCaseEntity pcsCase) {
        return pcsCase.getLegislativeCountry() == LegislativeCountry.WALES;
    }

    private static boolean isIntroDemotedOtherTenancy(TenancyLicenceEntity tenancy) {
        return tenancy != null
            && tenancy.getType() != null
            && INTRO_DEMOTED_OTHER_TYPES.contains(tenancy.getType());
    }

    private static List<ClaimGroundEntity> realGrounds(Collection<ClaimGroundEntity> grounds) {
        if (grounds == null) {
            return Collections.emptyList();
        }
        return grounds.stream()
            .filter(g -> g.getCategory() != ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS)
            .toList();
    }

    // The "Other" ground is identified by its code, not its category: both the assured and the
    // intro/demoted/other journeys store it as code "OTHER". Category names such as
    // INTRODUCTORY_DEMOTED_OTHER contain "OTHER" but are not the Other ground.
    private static boolean anyGroundIsOther(Collection<ClaimGroundEntity> grounds) {
        return anyGroundHasCode(grounds, "OTHER");
    }

    /**
     * The Wales ASB ground is identified by its {@code code}, not its category: both
     * WalesSecureClaimGroundService and WalesStandardClaimGroundService store
     * {@code ANTISOCIAL_BEHAVIOUR_S157} as the code under a non-ASB category.
     */
    private static boolean anyGroundHasCode(Collection<ClaimGroundEntity> grounds, String code) {
        if (grounds == null) {
            return false;
        }
        return grounds.stream().anyMatch(g -> code.equals(g.getCode()));
    }

    private static boolean isNoticeServedYes(NoticeOfPossessionEntity notice) {
        return notice != null && notice.getNoticeServed() == YesOrNo.YES;
    }

    private void mapCase(PcsCaseEntity pcsCase, ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        boolean isWales = isWalesClaim(pcsCase);
        payloadBuilder.isWales(isWales);
        payloadBuilder.isEngland(!isWales);
        AddressEntity propertyAddress = pcsCase.getPropertyAddress();
        payloadBuilder.propertyAddress(toClaimPackAddress(propertyAddress));
        if (propertyAddress != null) {
            payloadBuilder.hasPropertyAddressLine2(isPopulated(propertyAddress.getAddressLine2()));
            payloadBuilder.hasPropertyAddressLine3(isPopulated(propertyAddress.getAddressLine3()));
            payloadBuilder.hasPropertyCounty(isPopulated(propertyAddress.getCounty()));
        }
        payloadBuilder.referenceNumber(
            caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()));
    }

    private void mapClaim(ClaimEntity claim, ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (claim.getClaimSubmittedDate() != null) {
            payloadBuilder.submittedOn(claim.getClaimSubmittedDate().toLocalDate());
        }

        VerticalYesNo preActionFollowed = claim.getPreActionProtocolFollowed();
        payloadBuilder.preActionProtocolFollowedYesNo(toLabel(preActionFollowed));
        String preActionReason = claim.getPreActionProtocolIncompleteExplanation();
        // Hide the "why not followed" row unless a reason exists (Wales doesn't capture one).
        payloadBuilder.showPreActionProtocolNotFollowedReason(isNo(preActionFollowed) && isPopulated(preActionReason));
        payloadBuilder.preActionProtocolNotFollowedReason(preActionReason);

        payloadBuilder.mediationAttemptedYesNo(toLabel(claim.getMediationAttempted()));
        payloadBuilder.settlementAttemptedYesNo(toLabel(claim.getSettlementAttempted()));

        VerticalYesNo claimantCircumstances = claim.getClaimantCircumstancesProvided();
        payloadBuilder.hasClaimantCircsYesNo(toLabel(claimantCircumstances));
        payloadBuilder.showClaimantCircsFreeText(isYes(claimantCircumstances));
        payloadBuilder.claimantCircsFreeText(claim.getClaimantCircumstances());

        VerticalYesNo defendantCircumstances = claim.getDefendantCircumstancesProvided();
        payloadBuilder.hasDefendantCircsYesNo(toLabel(defendantCircumstances));
        payloadBuilder.showDefendantCircsFreeText(isYes(defendantCircumstances));
        payloadBuilder.defendantCircsFreeText(claim.getDefendantCircumstances());

        VerticalYesNo additionalReasonsProvided = claim.getAdditionalReasonsProvided();
        payloadBuilder.hasAdditionalReasonsYesNo(toLabel(additionalReasonsProvided));
        payloadBuilder.additionalReasonsProvided(isYes(additionalReasonsProvided));
        payloadBuilder.additionalReasonsFreeText(claim.getAdditionalReasons());

        payloadBuilder.hasUnderlesseeYesNo(toLabel(claim.getUnderlesseeOrMortgagee()));
        payloadBuilder.claimantIsExemptLandlord(toLabel(claim.getIsExemptLandlord()));
    }

    private void mapGrounds(Set<ClaimGroundEntity> allGrounds,
                            ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        // Exclude the "No grounds" sentinel so it never lists as a real ground, and an explicit-No
        // answer reports hasGroundsYesNo = No.
        List<ClaimGroundEntity> grounds = realGrounds(allGrounds);
        if (grounds.isEmpty()) {
            payloadBuilder.grounds(Collections.emptyList());
            payloadBuilder.groundsWithReasons(Collections.emptyList());
            payloadBuilder.hasGroundsYesNo(VerticalYesNo.NO.getLabel());
            payloadBuilder.showGroundsList(false);
            payloadBuilder.hasRentArrearsGround(false);
            payloadBuilder.hasAsbGround(false);
            payloadBuilder.hasOtherGround(false);
            payloadBuilder.isNoOrAbsoluteOrOtherGrounds(true);
            return;
        }

        List<ClaimPackGround> mapped = grounds.stream()
            .map(g -> ClaimPackGround.builder()
                .nameAndNumber(formatGroundLabel(g))
                .reasonFreeText(g.getReason())
                .hasReason(g.getReason() != null && !g.getReason().isBlank())
                .build())
            .toList();
        payloadBuilder.grounds(mapped);
        payloadBuilder.groundsWithReasons(mapped.stream().filter(ClaimPackGround::isHasReason).toList());
        payloadBuilder.hasGroundsYesNo(VerticalYesNo.YES.getLabel());
        payloadBuilder.showGroundsList(true);

        boolean hasRentArrears = grounds.stream()
            .anyMatch(g -> Boolean.TRUE.equals(g.getIsRentArrears()));
        payloadBuilder.hasRentArrearsGround(hasRentArrears);

        boolean hasAsb = grounds.stream()
            .anyMatch(g -> g.getCategory() == ClaimGroundCategory.SECURE_OR_FLEXIBLE_ANTISOCIAL);
        payloadBuilder.hasAsbGround(hasAsb);

        boolean hasOther = anyGroundIsOther(grounds);
        payloadBuilder.hasOtherGround(hasOther);

        // First "Other" ground's description, if any.
        grounds.stream()
            .filter(g -> "OTHER".equals(g.getCode()))
            .map(ClaimGroundEntity::getDescription)
            .filter(d -> d != null && !d.isBlank())
            .findFirst()
            .ifPresent(payloadBuilder::otherGroundsDescription);

        boolean hasAbsolute = anyGroundHasCode(grounds, "ABSOLUTE_GROUNDS");
        payloadBuilder.isNoOrAbsoluteOrOtherGrounds(hasAbsolute || hasOther);
    }

    private void mapNotice(NoticeOfPossessionEntity notice,
                            ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (notice == null) {
            return;
        }
        // Convert the CCD SDK YesOrNo to its display label.
        VerticalYesNo noticeServedEnum = yesOrNoToVertical(notice.getNoticeServed());
        payloadBuilder.noticeServedYesNo(toLabel(noticeServedEnum));
        // Show the "why not served" row only when a reason exists (England never captures it, so it
        // would otherwise print blank).
        boolean notServed = isNo(noticeServedEnum);
        payloadBuilder.noticeNotServedDisplayed(notServed && isPopulated(notice.getNoticeStatement()));
        // Gates the "Method of service onwards" sub-table.
        payloadBuilder.noticeServedYes(isYes(noticeServedEnum));

        // The serving method decides which field holds the served date and time: first-class post
        // and delivered-to-permitted-place store a date only (noticeDate); personally handed, email,
        // other electronic and other store a date and time (noticeDateTime). Take the date from
        // whichever is set so it renders for every method, and the time only when one was captured.
        LocalDate servedDate = notice.getNoticeDate() != null
            ? notice.getNoticeDate()
            : (notice.getNoticeDateTime() != null ? notice.getNoticeDateTime().toLocalDate() : null);
        LocalTime servedTime = notice.getNoticeDateTime() != null
            ? notice.getNoticeDateTime().toLocalTime()
            : null;
        payloadBuilder.showNoticeServedOn(servedDate != null);
        payloadBuilder.noticeServedOn(formatLongDate(servedDate));
        payloadBuilder.showNoticeServedTime(servedTime != null);
        payloadBuilder.noticeServedTime(formatNoticeTime(servedTime));
        payloadBuilder.noticeNotServedReason(notice.getNoticeStatement());
        payloadBuilder.noticeType(notice.getNoticeType());

        NoticeServiceMethod method = notice.getServingMethod();
        payloadBuilder.methodOfService(method);
        if (method != null) {
            payloadBuilder.methodOfServiceLabel(method.getLabel());
        }

        routeNoticeDetailByMethod(method, notice.getNoticeDetails(), payloadBuilder);
        clearUnsourcedNoticeUploadFlags(payloadBuilder);
    }

    // The entity's single noticeDetails field means different things per serving method; route it
    // to the matching payload slot. First-class-post and delivered-to-permitted-place have no detail.
    private void routeNoticeDetailByMethod(NoticeServiceMethod method, String details,
                                           ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (method == null || details == null) {
            return;
        }
        switch (method) {
            case PERSONALLY_HANDED -> {
                payloadBuilder.noticeLeftWithName(details);
                payloadBuilder.showNoticeLeftWithName(isPopulated(details));
            }
            case EMAIL -> {
                payloadBuilder.noticeServedToEmail(details);
                payloadBuilder.showNoticeServedToEmail(isPopulated(details));
            }
            case OTHER_ELECTRONIC -> {
                payloadBuilder.noticeOtherElectronicDetails(details);
                payloadBuilder.showNoticeOtherElectronicDetails(isPopulated(details));
            }
            case OTHER -> {
                payloadBuilder.noticeOtherMeansDetails(details);
                payloadBuilder.showNoticeOtherMeansDetails(isPopulated(details));
            }
            default -> {
                // First-class post and delivered-to-permitted-place have no detail row.
            }
        }
    }

    // The "can you upload the notice?" answer has no data source yet, so hide the whole row instead
    // of printing a label with a blank value. Set showNoticeUploadQuestion from the answer once an
    // entity field exists.
    private void clearUnsourcedNoticeUploadFlags(ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        payloadBuilder.showNoticeUploadQuestion(false);
        payloadBuilder.noticeUploadedYes(false);
        payloadBuilder.noticeUploadedNo(false);
    }

    private void mapTenancyLicence(TenancyLicenceEntity tenancy,
                                       ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (tenancy == null) {
            return;
        }
        if (tenancy.getType() != null) {
            payloadBuilder.isIntroDemotedOtherTenancy(INTRO_DEMOTED_OTHER_TYPES.contains(tenancy.getType()));
            payloadBuilder.tenancyTypeLabel(formatTenancyLabel(tenancy));
        }
        payloadBuilder.tenancyStartDate(formatLongDate(tenancy.getStartDate()));
        payloadBuilder.showTenancyStartDate(tenancy.getStartDate() != null);
        VerticalYesNo tenancyUploaded = tenancy.getHasCopyOfTenancyLicence();
        payloadBuilder.tenancyUploadedYesNo(tenancyUploaded);
        payloadBuilder.tenancyUploadedYes(isYes(tenancyUploaded));
        payloadBuilder.tenancyUploadedNo(isNo(tenancyUploaded));
        payloadBuilder.tenancyNotUploadedReason(tenancy.getReasonsForNoTenancyLicence());
        payloadBuilder.rentAmount(formatGbp(tenancy.getRentAmount()));
        payloadBuilder.rentCalculatedDescription(formatRentDescription(tenancy));
    }

    private void mapRentArrears(RentArrearsEntity rent,
                            ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (rent == null) {
            return;
        }
        payloadBuilder.rentArrearsTotal(formatGbp(rent.getTotalRentArrears()));
        VerticalYesNo judgmentEnum = rent.getArrearsJudgmentWanted();
        payloadBuilder.judgmentRequestedYesNo(toLabel(judgmentEnum));
        // "Details of previous steps" row only renders when previous-steps Y/N is YES.
        VerticalYesNo prevStepsEnum = rent.getRecoveryAttempted();
        payloadBuilder.hasPreviousStepsYesNo(toLabel(prevStepsEnum));
        payloadBuilder.showPreviousStepsFreeText(isYes(prevStepsEnum));
        payloadBuilder.previousStepsFreeText(rent.getRecoveryAttemptDetails());
    }

    private void mapAsbProhibitedConduct(AsbProhibitedConductEntity asb,
                                             ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (asb == null) {
            return;
        }
        mapAsbDetails(asb, payloadBuilder);
        mapPcscDetails(asb, payloadBuilder);
    }

    private void mapAsbDetails(AsbProhibitedConductEntity asb,
                               ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        VerticalYesNo asbEnum = asb.getAntisocialBehaviour();
        payloadBuilder.asbAllegedYesNo(toLabel(asbEnum));
        // Hide the Yes/No row when unanswered, matching the null-gate on the sibling rows below.
        payloadBuilder.showAsbAlleged(asbEnum != null);
        payloadBuilder.showAsbDetails(isYes(asbEnum));
        payloadBuilder.asbDetailsFreeText(asb.getAntisocialBehaviourDetails());

        VerticalYesNo illegalUseEnum = asb.getIllegalPurposes();
        payloadBuilder.illegalUseAllegedYesNo(toLabel(illegalUseEnum));
        payloadBuilder.showIllegalUseDetails(isYes(illegalUseEnum));
        payloadBuilder.illegalUseDetailsFreeText(asb.getIllegalPurposesDetails());

        VerticalYesNo otherProhibitedEnum = asb.getOtherProhibitedConduct();
        payloadBuilder.otherProhibitedAllegedYesNo(toLabel(otherProhibitedEnum));
        payloadBuilder.showOtherProhibitedDetails(isYes(otherProhibitedEnum));
        payloadBuilder.otherProhibitedDetailsFreeText(asb.getOtherProhibitedConductDetails());
    }

    // PCSC (Wales) is captured on the same entity as ASB. Its outer section gate (showPcscSection)
    // lives in mapClaimDetailsShowFlags, which is where the country is known.
    private void mapPcscDetails(AsbProhibitedConductEntity asb,
                                ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        VerticalYesNo pcscEnum = asb.getClaimingStandardContract();
        payloadBuilder.isPcscYesNo(toLabel(pcscEnum));
        payloadBuilder.showPcscDetails(isYes(pcscEnum));
        payloadBuilder.pcscReasonFreeText(asb.getClaimingStandardContractDetails());

        VerticalYesNo pcscTermsEnum = asb.getPeriodicContractAgreed();
        payloadBuilder.pcscTermsAgreedYesNo(toLabel(pcscTermsEnum));
        payloadBuilder.showPcscTermsFreeText(isYes(pcscTermsEnum));
        payloadBuilder.pcscTermsFreeText(asb.getPeriodicContractDetails());
    }

    private void mapPossessionAlternatives(PossessionAlternativesEntity alternatives,
                                               ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (alternatives == null) {
            return;
        }
        // Optional Y/N: the row is hidden if unanswered; the follow-up rows show only on "Yes".
        VerticalYesNo demotionClaimed = yesOrNoToVertical(alternatives.getDotRequested());
        payloadBuilder.showIsDemotionClaim(demotionClaimed != null);
        payloadBuilder.isDemotionClaimYesNo(toLabel(demotionClaimed));
        payloadBuilder.showDemotionDetails(isYes(demotionClaimed));
        if (alternatives.getDotHousingActSection() != null) {
            payloadBuilder.demotionHousingActSection(alternatives.getDotHousingActSection().getLabel());
        }
        VerticalYesNo demotionTermsServed = yesOrNoToVertical(alternatives.getDotStatementServed());
        payloadBuilder.hasServedDemotionTermsYesNo(toLabel(demotionTermsServed));
        payloadBuilder.showDemotionTermsFreeText(isYes(demotionTermsServed));
        payloadBuilder.demotionTermsFreeText(alternatives.getDotStatementDetails());
        payloadBuilder.demotionReasonsFreeText(alternatives.getDotReason());

        VerticalYesNo suspensionClaimed = yesOrNoToVertical(alternatives.getSuspensionOfRTB());
        payloadBuilder.showIsSuspensionClaim(suspensionClaimed != null);
        payloadBuilder.isSuspensionClaimYesNo(toLabel(suspensionClaimed));
        payloadBuilder.showSuspensionDetails(isYes(suspensionClaimed));
        if (alternatives.getSuspensionOfRTBHousingActSection() != null) {
            payloadBuilder.suspensionHousingActSection(alternatives.getSuspensionOfRTBHousingActSection().getLabel());
        }
        payloadBuilder.suspensionReasonsFreeText(alternatives.getSuspensionOfRTBReason());
    }

    private void mapStatementOfTruth(StatementOfTruthEntity sot,
                                         ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (sot == null) {
            return;
        }
        boolean legalRep = sot.getCompletedBy() == StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE;
        payloadBuilder.signedByLegalRep(legalRep);
        payloadBuilder.signedByClaimant(!legalRep);
        payloadBuilder.sotFullName(sot.getFullName());
        // Firm name is not shown this release: the value is populated but the template row is removed.
        payloadBuilder.sotFirmName(sot.getFirmName());
        payloadBuilder.sotPositionHeld(sot.getPositionHeld());
    }

    private List<PartyEntity> partiesByRole(ClaimEntity claim, PartyRole role) {
        if (claim.getClaimParties() == null) {
            return List.of();
        }
        List<ClaimPartyEntity> filtered = new ArrayList<>(claim.getClaimParties().stream()
            .filter(cp -> cp.getRole() == role)
            .toList());
        filtered.sort(Comparator.comparingInt(ClaimPartyEntity::getRank));
        return filtered.stream().map(ClaimPartyEntity::getParty).toList();
    }

}
