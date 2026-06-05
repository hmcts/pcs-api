package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
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
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackAddress;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackDefendantRow;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackGround;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackParty;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackUnderlesseeRow;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatDefendantHeading;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatGroundLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatRentDescription;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatTenancyLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.formatUnderlesseeHeading;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isNo;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isPopulated;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.isYes;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.toLabel;
import static uk.gov.hmcts.reform.pcs.ccd.service.claimpack.ClaimPackFormatter.yesOrNoToVertical;

/**
 * Builds {@link ClaimPackFormPayload} from a {@link PcsCaseEntity}.
 *
 * <p>One {@code mapXxx} method per source entity; each takes that entity (nullable for optional
 * relations) plus the payload builder and writes its fields. {@link #build(PcsCaseEntity)}
 * orchestrates. Fields with no data source yet are left null/false — the template's conditional
 * blocks simply don't render them.</p>
 */
@Service
public class ClaimPackPayloadBuilder {

    private static final Set<CombinedLicenceType> INTRO_DEMOTED_OTHER_TYPES = Set.of(
        CombinedLicenceType.INTRODUCTORY_TENANCY,
        CombinedLicenceType.DEMOTED_TENANCY,
        CombinedLicenceType.OTHER
    );

    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;

    public ClaimPackPayloadBuilder(CaseReferenceFormatter caseReferenceFormatter,
                                   CaseNameFormatter caseNameFormatter) {
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.caseNameFormatter = caseNameFormatter;
    }

    public ClaimPackFormPayload build(PcsCaseEntity pcsCase) {
        final ClaimEntity claim = pcsCase.getClaims().getFirst();
        final ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder = ClaimPackFormPayload.builder();

        mapCase(pcsCase, payloadBuilder);
        mapClaim(claim, payloadBuilder);

        final List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        final List<PartyEntity> defendants = partiesByRole(claim, PartyRole.DEFENDANT);
        mapClaimants(claimants, payloadBuilder);
        mapDefendants(defendants, pcsCase.getPropertyAddress(), payloadBuilder);
        mapUnderlessees(partiesByRole(claim, PartyRole.UNDERLESSEE_OR_MORTGAGEE), payloadBuilder);

        mapGrounds(claim.getClaimGrounds(), payloadBuilder);
        mapNotice(claim.getNoticeOfPossession(), payloadBuilder);
        mapTenancyLicence(pcsCase.getTenancyLicence(), payloadBuilder);
        mapRentArrears(claim.getRentArrears(), payloadBuilder);
        mapAsbProhibitedConduct(claim.getAsbProhibitedConductEntity(), payloadBuilder);
        mapPossessionAlternatives(claim.getPossessionAlternativesEntity(), payloadBuilder);
        mapStatementOfTruth(claim.getStatementOfTruth(), payloadBuilder);

        mapCaseName(claimants, defendants, payloadBuilder);
        mapClaimDetailsShowFlags(pcsCase, claim, payloadBuilder);

        return payloadBuilder.build();
    }

    /**
     * Cross-cutting section visibility flags. Depend on country + tenancy + grounds + notice,
     * so they're computed after the individual mappers have populated source data.
     */
    private void mapClaimDetailsShowFlags(PcsCaseEntity pcsCase, ClaimEntity claim,
                                          ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        boolean isWales = isWalesJourney(pcsCase);
        boolean isEngland = !isWales;
        boolean isIntroDemotedOther = isIntroDemotedOtherTenancy(pcsCase.getTenancyLicence());
        // Strip the "No grounds" sentinel (written when the claimant answered "No" to D9) before
        // deriving the flags below — its presence would otherwise make the set non-empty and mask hasNoGrounds.
        List<ClaimGroundEntity> grounds = realGrounds(claim.getClaimGrounds());
        boolean hasNoGrounds = grounds.isEmpty();
        boolean hasOtherGround = anyGroundIsOther(grounds);
        boolean hasAbsoluteGround = anyGroundHasCode(grounds, "ABSOLUTE_GROUNDS");
        boolean hasWalesAsbGround = anyGroundHasCode(grounds, "ANTISOCIAL_BEHAVIOUR_S157");
        boolean noticeServedYes = isNoticeServedYes(claim.getNoticeOfPossession());

        // Row D9 — no country qualifier in the spec, only the tenancy-type check.
        // (D11 and D13 below ARE explicitly English journey per spec; they keep isEngland.)
        payloadBuilder.showGroundsYesNoQuestion(isIntroDemotedOther);
        payloadBuilder.showDescriptionOfGrounds(isEngland && isIntroDemotedOther && hasOtherGround);
        // D13 trigger per Cook [17]: no grounds, absolute grounds, OR Other.
        payloadBuilder.showWhyClaimingPossession(
            isEngland && isIntroDemotedOther && (hasNoGrounds || hasAbsoluteGround || hasOtherGround));
        payloadBuilder.showAsbSection(isWales && hasWalesAsbGround);
        payloadBuilder.showNoticeType(isWales && noticeServedYes);
        payloadBuilder.showPcscSection(isWales);
        payloadBuilder.showRequiredDocumentsSection(isWales);
        // D6/D7 exempt-landlord question is Housing (Wales) Act 2014 — Wales-only.
        payloadBuilder.showExemptLandlordQuestion(isWales);
        // D49/D50 tenancy-copy rows are England-only (Wales never captures the answer).
        payloadBuilder.showTenancyUploadedQuestion(isEngland);
    }

    private static boolean isWalesJourney(PcsCaseEntity pcsCase) {
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

    // "Other" ground identity lives in the code, not the category — both the assured and the
    // intro/demoted/other journeys persist it as code "OTHER". (Category names like
    // INTRODUCTORY_DEMOTED_OTHER contain the substring "OTHER" but are not the Other ground.)
    private static boolean anyGroundIsOther(Collection<ClaimGroundEntity> grounds) {
        return anyGroundHasCode(grounds, "OTHER");
    }

    /**
     * Wales ASB ground identity lives in the {@code code} field, not the category — both
     * WalesSecureClaimGroundService and WalesStandardClaimGroundService persist
     * {@code ANTISOCIAL_BEHAVIOUR_S157} as the code under a non-ASB category.
     */
    private static boolean anyGroundHasCode(Collection<ClaimGroundEntity> grounds, String code) {
        if (grounds == null) {
            return false;
        }
        return grounds.stream().anyMatch(g -> code.equals(g.getCode()));
    }

    private static boolean isNoticeServedYes(NoticeOfPossessionEntity notice) {
        return notice != null
            && notice.getNoticeServed() != null
            && "YES".equalsIgnoreCase(notice.getNoticeServed().name());
    }

    private void mapCase(PcsCaseEntity pcsCase, ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        boolean isWales = pcsCase.getLegislativeCountry() == LegislativeCountry.WALES;
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
        // Hide the "why not followed" row unless a reason exists (Wales doesn't capture one → would print blank).
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

    private void mapClaimants(List<PartyEntity> claimants,
                            ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (claimants.isEmpty()) {
            return;
        }
        PartyEntity head = claimants.getFirst();
        ClaimPackParty mapped = toClaimPackParty(head);
        payloadBuilder.claimant(mapped);
        payloadBuilder.claimantDisplayName(deriveDisplayName(mapped));
        ClaimPackAddress addr = mapped.getAddress();
        payloadBuilder.hasClaimantAddressLine2(addr != null && isPopulated(addr.getAddressLine2()));
        payloadBuilder.hasClaimantAddressLine3(addr != null && isPopulated(addr.getAddressLine3()));
        payloadBuilder.hasClaimantCounty(addr != null && isPopulated(addr.getCounty()));
    }

    // A defendant with no address of their own falls back to the property address
    // (the claim form can't show a defendant address as "unknown").
    private void mapDefendants(List<PartyEntity> defendants,
                               AddressEntity propertyAddress,
                               ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        List<ClaimPackDefendantRow> rows = new ArrayList<>(defendants.size());
        int number = 1;
        for (PartyEntity defendant : defendants) {
            rows.add(toDefendantRow(defendant, number++, propertyAddress));
        }
        payloadBuilder.defendants(rows);
    }

    private ClaimPackDefendantRow toDefendantRow(PartyEntity defendant, int number, AddressEntity propertyAddress) {
        AddressEntity addr = pickAddressOrFallback(defendant.getAddress(), propertyAddress);
        return ClaimPackDefendantRow.builder()
            .defendantNumber(number)
            .heading(formatDefendantHeading(number))
            .displayName(derivePartyDisplayName(defendant))
            .addressLine1(addr.getAddressLine1())
            .addressLine2(addr.getAddressLine2())
            .addressLine3(addr.getAddressLine3())
            .postTown(addr.getPostTown())
            .county(addr.getCounty())
            .postcode(addr.getPostcode())
            .hasAddressLine2(isPopulated(addr.getAddressLine2()))
            .hasAddressLine3(isPopulated(addr.getAddressLine3()))
            .hasCounty(isPopulated(addr.getCounty()))
            .build();
    }

    private static AddressEntity pickAddressOrFallback(AddressEntity defendantAddress, AddressEntity propertyAddress) {
        if (defendantAddress != null && isPopulated(defendantAddress.getAddressLine1())) {
            return defendantAddress;
        }
        return propertyAddress;
    }

    private static String derivePartyDisplayName(PartyEntity party) {
        if (isPopulated(party.getOrgName())) {
            return party.getOrgName();
        }
        if (isNo(party.getNameKnown())) {
            return "Persons unknown";
        }
        boolean hasFirst = isPopulated(party.getFirstName());
        boolean hasLast = isPopulated(party.getLastName());
        if (!hasFirst && !hasLast) {
            return "Persons unknown";
        }
        return (hasFirst ? party.getFirstName() : "") + (hasFirst && hasLast ? " " : "")
            + (hasLast ? party.getLastName() : "");
    }

    // Each underlessee/mortgagee renders either a full address or a single "Address unknown" line.
    private void mapUnderlessees(List<PartyEntity> underlessees,
                                 ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        List<ClaimPackUnderlesseeRow> rows = new ArrayList<>(underlessees.size());
        int number = 1;
        for (PartyEntity underlessee : underlessees) {
            rows.add(toUnderlesseeRow(underlessee, number++));
        }
        payloadBuilder.underlessees(rows);
    }

    private ClaimPackUnderlesseeRow toUnderlesseeRow(PartyEntity underlessee, int number) {
        AddressEntity addr = underlessee.getAddress();
        boolean addressKnown = addr != null && isPopulated(addr.getAddressLine1());
        ClaimPackUnderlesseeRow.ClaimPackUnderlesseeRowBuilder rowBuilder = ClaimPackUnderlesseeRow.builder()
            .underlesseeNumber(number)
            .heading(formatUnderlesseeHeading(number))
            .displayName(derivePartyDisplayName(underlessee))
            .addressKnown(addressKnown)
            .addressUnknown(!addressKnown);
        if (addressKnown) {
            rowBuilder.addressLine1(addr.getAddressLine1());
            rowBuilder.addressLine2(addr.getAddressLine2());
            rowBuilder.addressLine3(addr.getAddressLine3());
            rowBuilder.postTown(addr.getPostTown());
            rowBuilder.county(addr.getCounty());
            rowBuilder.postcode(addr.getPostcode());
            rowBuilder.hasAddressLine2(isPopulated(addr.getAddressLine2()));
            rowBuilder.hasAddressLine3(isPopulated(addr.getAddressLine3()));
            rowBuilder.hasCounty(isPopulated(addr.getCounty()));
        }
        return rowBuilder.build();
    }

    private void mapGrounds(Set<ClaimGroundEntity> allGrounds,
                            ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        // Exclude the explicit "No grounds" sentinel so it never lists as a real ground (D10)
        // and so an explicit-No answer reports hasGroundsYesNo = No (D9).
        List<ClaimGroundEntity> grounds = realGrounds(allGrounds);
        if (grounds.isEmpty()) {
            payloadBuilder.grounds(Collections.emptyList());
            payloadBuilder.groundsWithReasons(Collections.emptyList());
            payloadBuilder.hasGroundsYesNo(VerticalYesNo.NO.getLabel());
            payloadBuilder.showGroundsList(false);
            payloadBuilder.hasRentArrearsGround(false);
            payloadBuilder.hasAsbGround(false);
            payloadBuilder.hasOtherGround(false);
            payloadBuilder.isNoOrAbsoluteOrOtherGrounds(true); // no grounds case
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
            .filter(g -> g.getCategory() != null)
            .anyMatch(g -> g.getCategory().name().contains("ANTISOCIAL"));
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
        // YesOrNo (CCD SDK) → title-case label; same bridge but rendered directly.
        VerticalYesNo noticeServedEnum = yesOrNoToVertical(notice.getNoticeServed());
        payloadBuilder.noticeServedYesNo(toLabel(noticeServedEnum));
        // Drop-on-null: show the "why not served" row only when a reason exists (England never
        // captures it, so it would otherwise print blank).
        boolean notServed = isNo(noticeServedEnum);
        payloadBuilder.noticeNotServedDisplayed(notServed && isPopulated(notice.getNoticeStatement()));
        // Positive boolean — gates the "Method of service onwards" sub-table.
        payloadBuilder.noticeServedYes(isYes(noticeServedEnum));

        // Optional-value-presence show-flags (Excel mapping rows 37-42).
        payloadBuilder.showNoticeServedOn(notice.getNoticeDate() != null);
        payloadBuilder.showNoticeServedTime(notice.getNoticeDateTime() != null);
        payloadBuilder.noticeNotServedReason(notice.getNoticeStatement());
        payloadBuilder.noticeType(notice.getNoticeType());

        NoticeServiceMethod method = notice.getServingMethod();
        payloadBuilder.methodOfService(method);
        if (method != null) {
            payloadBuilder.methodOfServiceLabel(method.getLabel());
        }

        payloadBuilder.noticeServedOn(notice.getNoticeDate());
        if (notice.getNoticeDateTime() != null) {
            payloadBuilder.noticeServedTime(notice.getNoticeDateTime().toLocalTime());
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
                // FIRST_CLASS_POST, DELIVERED_PERMITTED_PLACE — no detail row.
            }
        }
    }

    // The "can you upload the notice?" answer has no data source yet, so neither branch renders.
    private void clearUnsourcedNoticeUploadFlags(ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
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
        payloadBuilder.tenancyStartDate(tenancy.getStartDate());
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
        // Hide the D16 Yes/No row when unanswered — matches the null-gate on its D18/D20 siblings.
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
        boolean legalRep = sot.getCompletedBy() != null
            && "LEGAL_REPRESENTATIVE".equals(sot.getCompletedBy().name());
        payloadBuilder.signedByLegalRep(legalRep);
        payloadBuilder.signedByClaimant(!legalRep);
        payloadBuilder.sotFullName(sot.getFullName());
        // D80 (Excel): "N/A for this release phase" — populated but the template row is removed for now.
        payloadBuilder.sotFirmName(sot.getFirmName());
        payloadBuilder.sotPositionHeld(sot.getPositionHeld());
    }

    private void mapCaseName(List<PartyEntity> claimants,
                             List<PartyEntity> defendants,
                             ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (claimants.isEmpty() && defendants.isEmpty()) {
            return;
        }
        List<Party> claimantDomain = claimants.stream().map(ClaimPackPayloadBuilder::toDomainParty).toList();
        List<Party> defendantDomain = defendants.stream().map(ClaimPackPayloadBuilder::toDomainParty).toList();
        payloadBuilder.caseName(caseNameFormatter.formatCaseName(claimantDomain, defendantDomain));
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

    private ClaimPackParty toClaimPackParty(PartyEntity party) {
        boolean isPersonsUnknown = isNo(party.getNameKnown());
        return ClaimPackParty.builder()
            .firstName(party.getFirstName())
            .lastName(party.getLastName())
            .orgName(party.getOrgName())
            .isPersonsUnknown(isPersonsUnknown)
            .address(toClaimPackAddress(party.getAddress()))
            .build();
    }

    private ClaimPackAddress toClaimPackAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }
        return ClaimPackAddress.builder()
            .addressLine1(address.getAddressLine1())
            .addressLine2(address.getAddressLine2())
            .addressLine3(address.getAddressLine3())
            .postTown(address.getPostTown())
            .county(address.getCounty())
            .postcode(address.getPostcode())
            .country(address.getCountry())
            .build();
    }

    private static Party toDomainParty(PartyEntity party) {
        return Party.builder()
            .firstName(party.getFirstName())
            .lastName(party.getLastName())
            .orgName(party.getOrgName())
            .nameKnown(party.getNameKnown())
            .build();
    }

    // org name if set, else "first last" if either is set, else "Persons unknown".
    private static String deriveDisplayName(ClaimPackParty party) {
        if (isPopulated(party.getOrgName())) {
            return party.getOrgName();
        }
        boolean hasFirst = isPopulated(party.getFirstName());
        boolean hasLast = isPopulated(party.getLastName());
        if (hasFirst || hasLast) {
            return (hasFirst ? party.getFirstName() : "") + (hasFirst && hasLast ? " " : "")
                + (hasLast ? party.getLastName() : "");
        }
        return "Persons unknown";
    }

}
