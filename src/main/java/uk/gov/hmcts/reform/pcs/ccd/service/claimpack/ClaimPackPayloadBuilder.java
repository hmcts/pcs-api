package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PossessionGroundLabelResolver;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
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

/**
 * Builds {@link ClaimPackFormPayload} from a {@link PcsCaseEntity}.
 *
 * <p>Field-by-field source mapping is in plan §13.2; the section-by-section visibility rules are
 * in §6.5. Gap fields (§13.3) are left null/false — the template's {@code <<? cond>>} blocks
 * simply don't render until the gap is closed.</p>
 *
 * <p>Layout: one {@code mapXxx} method per source entity, matching the §13.2 by-class tables.
 * Each helper takes the source entity (nullable for optional relations) plus the payload builder
 * and writes its fields. The top-level {@link #build(PcsCaseEntity)} orchestrates.</p>
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
        // §6.3.1
        if (claim.getClaimSubmittedDate() != null) {
            payloadBuilder.submittedOn(claim.getClaimSubmittedDate().toLocalDate());
        }
        // issueDateSealed is §13.3 gap (1) — left null.

        // §6.3.10 — Y/N to title-case labels; "Why not followed" row gated by complementary boolean.
        VerticalYesNo preActionEnum = claim.getPreActionProtocolFollowed();
        payloadBuilder.preActionProtocolFollowedYesNo(toLabel(preActionEnum));
        // Drop-on-null: hide the "why not followed" row unless a reason was actually captured
        // (e.g. the Wales journey doesn't capture it, so it would otherwise print blank).
        String preActionReason = claim.getPreActionProtocolIncompleteExplanation();
        payloadBuilder.showPreActionProtocolNotFollowedReason(isNo(preActionEnum) && isPopulated(preActionReason));
        payloadBuilder.preActionProtocolNotFollowedReason(preActionReason);

        VerticalYesNo mediationEnum = claim.getMediationAttempted();
        payloadBuilder.mediationAttemptedYesNo(toLabel(mediationEnum));
        VerticalYesNo settlementEnum = claim.getSettlementAttempted();
        payloadBuilder.settlementAttemptedYesNo(toLabel(settlementEnum));

        // §6.3.13 / §6.3.14 — title-case labels + details show-flag gated on YES.
        VerticalYesNo claimantCircsEnum = claim.getClaimantCircumstancesProvided();
        payloadBuilder.hasClaimantCircsYesNo(toLabel(claimantCircsEnum));
        payloadBuilder.showClaimantCircsFreeText(isYes(claimantCircsEnum));
        payloadBuilder.claimantCircsFreeText(claim.getClaimantCircumstances());

        VerticalYesNo defendantCircsEnum = claim.getDefendantCircumstancesProvided();
        payloadBuilder.hasDefendantCircsYesNo(toLabel(defendantCircsEnum));
        payloadBuilder.showDefendantCircsFreeText(isYes(defendantCircsEnum));
        payloadBuilder.defendantCircsFreeText(claim.getDefendantCircumstances());

        // §6.3.7 additional reasons row — title-case label for direct Docmosis render.
        VerticalYesNo additionalReasonsEnum = claim.getAdditionalReasonsProvided();
        payloadBuilder.hasAdditionalReasonsYesNo(toLabel(additionalReasonsEnum));
        payloadBuilder.additionalReasonsProvided(isYes(additionalReasonsEnum));
        payloadBuilder.additionalReasonsFreeText(claim.getAdditionalReasons());

        // §6.3.15 yes/no gate — title-case for direct template rendering.
        VerticalYesNo underlesseeEnum = claim.getUnderlesseeOrMortgagee();
        payloadBuilder.hasUnderlesseeYesNo(toLabel(underlesseeEnum));

        // §6.3.2 Wales-only row — convert to title-case "Yes"/"No" string for direct Docmosis render.
        payloadBuilder.claimantIsExemptLandlord(
            toLabel(claim.getIsExemptLandlord())
        );

        // whyClaimingPossession is §13.3 gap (2) — left null.
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

    // -----------------------------------------------------------------------
    // PartyEntity (defendants) — §13.2 "From PartyEntity"
    //
    // Produces one flat list rendered by a single <<rs_defendants>>...<<es_>> loop.
    // Address fallback: defendant.address used if its addressLine1 is populated,
    // otherwise the property address (spec: "address cannot be Unknown").
    // -----------------------------------------------------------------------
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

    // -----------------------------------------------------------------------
    // PartyEntity (underlessees / mortgagees) — §13.2 "From PartyEntity"
    //
    // Flat list with Address-unknown semantic per Excel row 58/60: address either
    // provided (render 6 lines) or unknown (render "Address unknown" single line).
    // -----------------------------------------------------------------------
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
        applyNoticeUploadGapPlaceholder(payloadBuilder);
    }

    /**
     * The {@code noticeDetails} column on the entity carries one of four distinct fields depending
     * on {@code servingMethod}. Route to the right payload slot; FIRST_CLASS_POST and
     * DELIVERED_PERMITTED_PLACE have no detail row (§6.3.11).
     */
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

    /** Plan §13.3 gap (3): noticeUploadedYesNo source field is pending. Both branches false. */
    private void applyNoticeUploadGapPlaceholder(ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        VerticalYesNo uploaded = null;
        payloadBuilder.noticeUploadedYes(isYes(uploaded));
        payloadBuilder.noticeUploadedNo(isNo(uploaded));
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

    /**
     * PCSC (Wales) is captured on the same entity as ASB — see plan §13.2.
     * The outer section gate {@code showPcscSection} lives in {@link #mapClaimDetailsShowFlags}
     * because it depends on country, which this method doesn't see.
     */
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

    private void mapPossessionAlternatives(PossessionAlternativesEntity alt,
                                               ClaimPackFormPayload.ClaimPackFormPayloadBuilder payloadBuilder) {
        if (alt == null) {
            return;
        }
        // Demotion — optional Y/N: row hidden if user didn't answer; follow-ups gated on YES.
        VerticalYesNo demotionEnum = yesOrNoToVertical(alt.getDotRequested());
        payloadBuilder.showIsDemotionClaim(demotionEnum != null);
        payloadBuilder.isDemotionClaimYesNo(toLabel(demotionEnum));
        payloadBuilder.showDemotionDetails(isYes(demotionEnum));
        if (alt.getDotHousingActSection() != null) {
            payloadBuilder.demotionHousingActSection(alt.getDotHousingActSection().getLabel());
        }
        VerticalYesNo demoTermsEnum = yesOrNoToVertical(alt.getDotStatementServed());
        payloadBuilder.hasServedDemotionTermsYesNo(toLabel(demoTermsEnum));
        payloadBuilder.showDemotionTermsFreeText(isYes(demoTermsEnum));
        payloadBuilder.demotionTermsFreeText(alt.getDotStatementDetails());
        payloadBuilder.demotionReasonsFreeText(alt.getDotReason());

        // Suspension — same shape (optional Y/N + follow-ups gated on YES).
        VerticalYesNo suspensionEnum = yesOrNoToVertical(alt.getSuspensionOfRTB());
        payloadBuilder.showIsSuspensionClaim(suspensionEnum != null);
        payloadBuilder.isSuspensionClaimYesNo(toLabel(suspensionEnum));
        payloadBuilder.showSuspensionDetails(isYes(suspensionEnum));
        if (alt.getSuspensionOfRTBHousingActSection() != null) {
            payloadBuilder.suspensionHousingActSection(alt.getSuspensionOfRTBHousingActSection().getLabel());
        }
        payloadBuilder.suspensionReasonsFreeText(alt.getSuspensionOfRTBReason());
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

    // -----------------------------------------------------------------------
    // Derived — §13.2 "From helpers / computed"
    // -----------------------------------------------------------------------
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

    // =====================================================================
    // Small helpers
    // =====================================================================

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

    private static Party toDomainParty(PartyEntity e) {
        return Party.builder()
            .firstName(e.getFirstName())
            .lastName(e.getLastName())
            .orgName(e.getOrgName())
            .nameKnown(e.getNameKnown())
            .build();
    }

    /**
     * Bridge from the CCD SDK {@code YesOrNo} (used by some entities) to PCS's {@code VerticalYesNo}
     * (used by the payload). Returns null for null input. Tolerates either spelling defensively.
     */
    private static VerticalYesNo yesOrNoToVertical(uk.gov.hmcts.ccd.sdk.type.YesOrNo y) {
        if (y == null) {
            return null;
        }
        return y == uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES ? VerticalYesNo.YES : VerticalYesNo.NO;
    }

    private static String formatGroundLabel(ClaimGroundEntity g) {
        // Resolve the persisted category + code to the ground's human-readable label
        // (e.g. "Serious rent arrears (ground 8)"), not the raw enum identifier.
        return PossessionGroundLabelResolver.label(g.getCategory(), g.getCode());
    }

    private static String formatTenancyLabel(TenancyLicenceEntity t) {
        CombinedLicenceType type = t.getType();
        if (type == null) {
            return null;
        }
        String label = combinedLicenceLabel(type);
        return type == CombinedLicenceType.OTHER && t.getOtherTypeDetails() != null
            ? label + ": " + t.getOtherTypeDetails()
            : label;
    }

    // CombinedLicenceType carries no label of its own — reuse the England/Wales source enums that do.
    private static String combinedLicenceLabel(CombinedLicenceType type) {
        for (TenancyLicenceType e : TenancyLicenceType.values()) {
            if (e.getCombinedLicenceType() == type) {
                return e.getLabel();
            }
        }
        for (OccupationLicenceTypeWales e : OccupationLicenceTypeWales.values()) {
            if (e.getCombinedLicenceType() == type) {
                return e.getLabel();
            }
        }
        return type.name();
    }

    /**
     * Render-ready name: org name if set, else "first last" if either is set, else "Persons unknown".
     * Mirrors how the template's {@code ((name))} designer placeholder is intended to read.
     */
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

    private static boolean isPopulated(String text) {
        return text != null && !text.isBlank();
    }

    /** AC06: first party "Defendant 1 details", subsequent "Additional defendant N details". */
    static String formatDefendantHeading(int defendantNumber) {
        return defendantNumber == 1
            ? "Defendant 1 details"
            : "Additional defendant " + (defendantNumber - 1) + " details";
    }

    /** Same numbering convention as defendants but underlessee/mortgagee phrasing. */
    static String formatUnderlesseeHeading(int underlesseeNumber) {
        return underlesseeNumber == 1
            ? "Underlessee or mortgagee 1 details"
            : "Additional underlessee or mortgagee " + (underlesseeNumber - 1) + " details";
    }

    /** Null-safe title-case label for direct rendering — VerticalYesNo.YES → "Yes". */
    private static String toLabel(VerticalYesNo yesNo) {
        return yesNo == null ? null : yesNo.getLabel();
    }

    /** Null-safe label for NoticeServiceMethod (used inside the notice section). */
    private static String toLabel(NoticeServiceMethod method) {
        return method == null ? null : method.getLabel();
    }

    private static boolean isYes(VerticalYesNo yesNo) {
        return yesNo == VerticalYesNo.YES;
    }

    private static boolean isNo(VerticalYesNo yesNo) {
        return yesNo == VerticalYesNo.NO;
    }

    private static String formatRentDescription(TenancyLicenceEntity t) {
        if (t.getRentAmount() == null || t.getRentFrequency() == null) {
            return null;
        }
        return formatGbp(t.getRentAmount()) + " (" + t.getRentFrequency().getLabel() + ")";
    }

    /** Format a money value as a GBP currency string, e.g. £1,200.00. Null-safe. */
    static String formatGbp(java.math.BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        java.text.NumberFormat fmt = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.UK);
        return fmt.format(amount);
    }

}
