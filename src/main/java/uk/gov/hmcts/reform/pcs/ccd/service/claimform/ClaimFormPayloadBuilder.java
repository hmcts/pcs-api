package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import org.springframework.beans.factory.annotation.Qualifier;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormGround;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.formatGroundLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.formatLongDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.formatNoticeTime;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.formatRentFrequency;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.formatTenancyLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.isNo;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.isPopulated;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.isYes;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.toClaimFormAddress;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.toLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormFormatter.yesOrNoToVertical;

/**
 * Builds {@link ClaimFormPayload} from a {@link PcsCaseEntity}.
 *
 * <p>One {@code mapXxx} method per source entity; each takes that entity (nullable for optional
 * relations) plus the payload builder and writes its fields. {@link #build(PcsCaseEntity)} runs
 * them in turn. Fields with no data source yet are left null or false, so the template's
 * conditional blocks do not render them.</p>
 */
@Service
public class ClaimFormPayloadBuilder {

    private static final Set<CombinedLicenceType> INTRO_DEMOTED_OTHER_TYPES = Set.of(
        CombinedLicenceType.INTRODUCTORY_TENANCY,
        CombinedLicenceType.DEMOTED_TENANCY,
        CombinedLicenceType.OTHER
    );

    private final CaseReferenceFormatter caseReferenceFormatter;
    private final ClaimFormPartyMapper partyMapper;
    private final Clock ukClock;

    public ClaimFormPayloadBuilder(CaseReferenceFormatter caseReferenceFormatter,
                                   ClaimFormPartyMapper partyMapper,
                                   @Qualifier("ukClock") Clock ukClock) {
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.partyMapper = partyMapper;
        this.ukClock = ukClock;
    }

    public ClaimFormPayload build(PcsCaseEntity pcsCase) {
        final ClaimEntity claim = pcsCase.getClaims().getFirst();
        final ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder = ClaimFormPayload.builder();

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
        mapRequiredDocuments(claim, payloadBuilder);
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
                                          ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        boolean isWales = isWalesClaim(pcsCase);
        boolean isEngland = !isWales;
        boolean isIntroDemotedOther = isIntroDemotedOtherTenancy(pcsCase.getTenancyLicence());
        List<ClaimGroundEntity> grounds = groundsExcludingNoGroundsSentinel(claim.getClaimGrounds());
        boolean hasOtherGround = anyGroundIsOther(grounds);
        boolean hasWalesAsbGround = anyGroundHasCode(grounds, "ANTISOCIAL_BEHAVIOUR_S157");
        boolean noticeServedYes = isNoticeServedYes(claim.getNoticeOfPossession());

        // One "Why is the claimant claiming possession?" row per Absolute/Other/No-grounds answer.
        payloadBuilder.whyClaimingPossessionGrounds(whyClaimingPossessionGrounds(claim.getClaimGrounds()));
        payloadBuilder.showGroundsYesNoQuestion(isIntroDemotedOther);
        // "Description of grounds" covers any England "Other" ground (intro/demoted/other or assured).
        payloadBuilder.showDescriptionOfGrounds(isEngland && hasOtherGround);
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

    private static List<ClaimGroundEntity> groundsExcludingNoGroundsSentinel(Collection<ClaimGroundEntity> grounds) {
        if (grounds == null) {
            return Collections.emptyList();
        }
        return grounds.stream()
            .filter(g -> g.getCategory() != ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS)
            .toList();
    }

    // D13 "Why is the claimant claiming possession?": the intro/demoted/other journey asks this general
    // question (rather than "...under this ground?") for the No-grounds, Absolute and Other grounds, so
    // their answers feed this single row. mapGrounds excludes the same grounds from the per-ground D12
    // list so the text is not shown twice. Absolute and Other can both be selected, so the answers are
    // combined in a stable order (no-grounds, absolute, other).
    private static List<ClaimFormGround> whyClaimingPossessionGrounds(Collection<ClaimGroundEntity> grounds) {
        if (grounds == null) {
            return Collections.emptyList();
        }
        return grounds.stream()
            .filter(ClaimFormPayloadBuilder::isWhyClaimingPossessionGround)
            .filter(g -> isPopulated(g.getReason()))
            .sorted(Comparator.comparingInt(ClaimFormPayloadBuilder::whyClaimingPossessionOrder))
            .map(g -> ClaimFormGround.builder()
                .nameAndNumber(whyClaimingPossessionGroundName(g))
                .reasonFreeText(g.getReason())
                .hasReason(true)
                .build())
            .toList();
    }

    // Bracket label: the Other ground reads "Other grounds" (its journey heading); no-grounds has none.
    private static String whyClaimingPossessionGroundName(ClaimGroundEntity ground) {
        if (ground.getCategory() == ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS) {
            return null;
        }
        return "OTHER".equals(ground.getCode()) ? "Other grounds" : formatGroundLabel(ground);
    }

    // The grounds whose reason answers the general "Why are you claiming possession?" question.
    private static boolean isWhyClaimingPossessionGround(ClaimGroundEntity ground) {
        if (ground.getCategory() == ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS) {
            return true;
        }
        return ground.getCategory() == ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER
            && ("ABSOLUTE_GROUNDS".equals(ground.getCode()) || "OTHER".equals(ground.getCode()));
    }

    private static int whyClaimingPossessionOrder(ClaimGroundEntity ground) {
        if (ground.getCategory() == ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS) {
            return 0;
        }
        return "ABSOLUTE_GROUNDS".equals(ground.getCode()) ? 1 : 2;
    }

    private static ClaimFormGround toClaimFormGround(ClaimGroundEntity ground) {
        return ClaimFormGround.builder()
            .nameAndNumber(formatGroundLabel(ground))
            .reasonFreeText(ground.getReason())
            .hasReason(isPopulated(ground.getReason()))
            .build();
    }

    private static Optional<String> firstOtherGroundDescription(Collection<ClaimGroundEntity> grounds) {
        return grounds.stream()
            .filter(g -> "OTHER".equals(g.getCode()))
            .map(ClaimGroundEntity::getDescription)
            .filter(description -> isPopulated(description))
            .findFirst();
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

    private void mapCase(PcsCaseEntity pcsCase, ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        boolean isWales = isWalesClaim(pcsCase);
        payloadBuilder.isWales(isWales);
        payloadBuilder.isEngland(!isWales);
        AddressEntity propertyAddress = pcsCase.getPropertyAddress();
        payloadBuilder.propertyAddress(toClaimFormAddress(propertyAddress));
        if (propertyAddress != null) {
            payloadBuilder.hasPropertyAddressLine2(isPopulated(propertyAddress.getAddressLine2()));
            payloadBuilder.hasPropertyAddressLine3(isPopulated(propertyAddress.getAddressLine3()));
            payloadBuilder.hasPropertyCounty(isPopulated(propertyAddress.getCounty()));
        }
        payloadBuilder.referenceNumber(
            caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()));
    }

    private void mapClaim(ClaimEntity claim, ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        if (claim.getClaimSubmittedDate() != null) {
            // The claim form is generated at submission, so the current UK date is the date
            // submitted - same approach as the general-application document.
            payloadBuilder.submittedOn(LocalDate.now(ukClock));
        }
        if (claim.getClaimIssuedDate() != null) {
            // Stored as a UTC timestamp; convert to the UK calendar date so a claim issued just
            // after midnight BST shows the correct day rather than the previous one.
            payloadBuilder.issueDateSealed(claim.getClaimIssuedDate().atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ukClock.getZone()).toLocalDate());
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
                            ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        List<ClaimGroundEntity> grounds = groundsExcludingNoGroundsSentinel(allGrounds);
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

        List<ClaimFormGround> mapped = grounds.stream()
            .map(ClaimFormPayloadBuilder::toClaimFormGround)
            .toList();
        payloadBuilder.grounds(mapped);
        // The Absolute/Other intro grounds answer the general "Why are you claiming possession?"
        // question (D13), so they are excluded from the per-ground reason list to avoid duplication.
        payloadBuilder.groundsWithReasons(grounds.stream()
            .filter(g -> isPopulated(g.getReason()) && !isWhyClaimingPossessionGround(g))
            .map(ClaimFormPayloadBuilder::toClaimFormGround)
            .toList());
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

        firstOtherGroundDescription(grounds).ifPresent(payloadBuilder::otherGroundsDescription);

        boolean hasAbsolute = anyGroundHasCode(grounds, "ABSOLUTE_GROUNDS");
        payloadBuilder.isNoOrAbsoluteOrOtherGrounds(hasAbsolute || hasOther);
    }

    private void mapNotice(NoticeOfPossessionEntity notice,
                            ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        if (notice == null) {
            return;
        }
        VerticalYesNo noticeServedEnum = yesOrNoToVertical(notice.getNoticeServed());
        payloadBuilder.noticeServedYesNo(toLabel(noticeServedEnum));
        boolean notServed = isNo(noticeServedEnum);
        payloadBuilder.noticeNotServedDisplayed(notServed && isPopulated(notice.getNoticeStatement()));
        payloadBuilder.noticeServedYes(isYes(noticeServedEnum));

        LocalDate servedDate = servedDate(notice);
        LocalTime servedTime = servedTime(notice);
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

    private static LocalDate servedDate(NoticeOfPossessionEntity notice) {
        if (notice.getNoticeDate() != null) {
            return notice.getNoticeDate();
        }
        return notice.getNoticeDateTime() != null ? notice.getNoticeDateTime().toLocalDate() : null;
    }

    private static LocalTime servedTime(NoticeOfPossessionEntity notice) {
        return notice.getNoticeDateTime() != null ? notice.getNoticeDateTime().toLocalTime() : null;
    }

    private void routeNoticeDetailByMethod(NoticeServiceMethod method, String details,
                                           ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
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
    private void clearUnsourcedNoticeUploadFlags(ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        payloadBuilder.showNoticeUploadQuestion(false);
        payloadBuilder.noticeUploadedYes(false);
        payloadBuilder.noticeUploadedNo(false);
    }

    private void mapTenancyLicence(TenancyLicenceEntity tenancy,
                                       ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
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
        payloadBuilder.rentCalculatedDescription(formatRentFrequency(tenancy));
    }

    // Wales-only required documents (EPC, gas safety, EICR). The section itself is gated on isWales
    // via showRequiredDocumentsSection; England claims never capture these so the getters return null.
    private void mapRequiredDocuments(ClaimEntity claim,
                                      ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        VerticalYesNo epc = claim.getEnergyPerformanceCertificateProvided();
        payloadBuilder.epcUploadedYesNo(toLabel(epc));
        payloadBuilder.showEpcNotUploadedReason(isNo(epc));
        payloadBuilder.epcNotUploadedReason(claim.getNoEnergyPerformanceCertificateReason());

        VerticalYesNo gas = claim.getGasSafetyReportProvided();
        payloadBuilder.gasSafetyUploadedYesNo(toLabel(gas));
        payloadBuilder.showGasSafetyNotUploadedReason(isNo(gas));
        payloadBuilder.gasSafetyNotUploadedReason(claim.getNoGasSafetyReportReason());

        VerticalYesNo eicr = claim.getElectricalInstallationConditionProvided();
        payloadBuilder.eicrUploadedYesNo(toLabel(eicr));
        payloadBuilder.showEicrNotUploadedReason(isNo(eicr));
        payloadBuilder.eicrNotUploadedReason(claim.getNoElectricalInstallationConditionReason());
    }

    private void mapRentArrears(RentArrearsEntity rent,
                            ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        if (rent == null) {
            return;
        }
        payloadBuilder.rentArrearsTotal(formatGbp(rent.getTotalRentArrears()));
        VerticalYesNo judgmentEnum = rent.getArrearsJudgmentWanted();
        payloadBuilder.judgmentRequestedYesNo(toLabel(judgmentEnum));
        VerticalYesNo prevStepsEnum = rent.getRecoveryAttempted();
        payloadBuilder.hasPreviousStepsYesNo(toLabel(prevStepsEnum));
        payloadBuilder.showPreviousStepsFreeText(isYes(prevStepsEnum));
        payloadBuilder.previousStepsFreeText(rent.getRecoveryAttemptDetails());
    }

    private void mapAsbProhibitedConduct(AsbProhibitedConductEntity asb,
                                             ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
        if (asb == null) {
            return;
        }
        mapAsbDetails(asb, payloadBuilder);
        mapPcscDetails(asb, payloadBuilder);
    }

    private void mapAsbDetails(AsbProhibitedConductEntity asb,
                               ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
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
                                ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
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
                                               ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
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
                                         ClaimFormPayload.ClaimFormPayloadBuilder payloadBuilder) {
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
        return PartyDisplayMapper.partiesByRole(claim, role);
    }

}
