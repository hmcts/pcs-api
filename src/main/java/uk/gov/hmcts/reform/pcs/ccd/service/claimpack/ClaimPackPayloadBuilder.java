package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
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
 * <p>Layout: one {@code mapFromXxx} method per source entity, matching the §13.2 by-class tables.
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

    private static final Set<NoticeServiceMethod> METHODS_REQUIRING_TIME = Set.of(
        NoticeServiceMethod.PERSONALLY_HANDED,
        NoticeServiceMethod.EMAIL,
        NoticeServiceMethod.OTHER_ELECTRONIC,
        NoticeServiceMethod.OTHER
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
        final ClaimPackFormPayload.ClaimPackFormPayloadBuilder p = ClaimPackFormPayload.builder();

        mapFromPcsCase(pcsCase, p);
        mapFromClaim(claim, p);

        // Index parties by role — claimants used immediately, defendants and underlessees right
        // after, then claimants + defendants reused for the derived case name.
        final List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        mapClaimants(claimants, p);
        final List<PartyEntity> defendants = partiesByRole(claim, PartyRole.DEFENDANT);
        mapDefendants(defendants, pcsCase.getPropertyAddress(), p);
        mapUnderlessees(partiesByRole(claim, PartyRole.UNDERLESSEE_OR_MORTGAGEE), p);

        mapFromGrounds(claim.getClaimGrounds(), p);
        mapFromNotice(claim.getNoticeOfPossession(), p);
        mapFromTenancyLicence(pcsCase.getTenancyLicence(), p);
        mapFromRentArrears(claim.getRentArrears(), p);
        mapFromAsbProhibitedConduct(claim.getAsbProhibitedConductEntity(), p);
        mapFromPossessionAlternatives(claim.getPossessionAlternativesEntity(), p);
        mapFromStatementOfTruth(claim.getStatementOfTruth(), p);

        // Derived after individual mappers populate the source data.
        mapCaseName(claimants, defendants, p);
        mapClaimDetailsShowFlags(pcsCase, claim, p);

        return p.build();
    }

    // Cross-cutting flags for the Claim details section — depend on country + tenancy + grounds.
    // Computed at the end so we can read consistent source state without coupling the
    // individual mappers to each other.
    private void mapClaimDetailsShowFlags(PcsCaseEntity pcsCase, ClaimEntity claim,
                                          ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        boolean isWales = pcsCase.getLegislativeCountry() == LegislativeCountry.WALES;
        boolean isEngland = !isWales;
        TenancyLicenceEntity tenancy = pcsCase.getTenancyLicence();
        boolean isIntroDemotedOther = tenancy != null
            && tenancy.getType() != null
            && INTRO_DEMOTED_OTHER_TYPES.contains(tenancy.getType());
        Set<ClaimGroundEntity> grounds = claim.getClaimGrounds();
        boolean hasNoGrounds = grounds == null || grounds.isEmpty();
        boolean hasOther = grounds != null && grounds.stream()
            .filter(g -> g.getCategory() != null && g.getCode() != null)
            .anyMatch(g -> g.getCategory().name().contains("OTHER") || "OTHER".equals(g.getCode()));
        // Wales ASB ground identity lives in the `code` field, not the category (see
        // WalesSecureClaimGroundService + WalesStandardClaimGroundService — both persist
        // ANTISOCIAL_BEHAVIOUR_S157 as the code under a non-ASB category).
        boolean hasWalesAsbGround = grounds != null && grounds.stream()
            .anyMatch(g -> "ANTISOCIAL_BEHAVIOUR_S157".equals(g.getCode()));

        p.showDescriptionOfGrounds(isEngland && isIntroDemotedOther && hasOther);
        p.showWhyClaimingPossession(isEngland && isIntroDemotedOther && (hasNoGrounds || hasOther));
        p.showAsbSection(isWales && hasWalesAsbGround);

        // Wales-only "Notice type" row — only when notice has actually been served.
        NoticeOfPossessionEntity notice = claim.getNoticeOfPossession();
        boolean noticeServedYes = notice != null
            && notice.getNoticeServed() != null
            && "YES".equalsIgnoreCase(notice.getNoticeServed().name());
        p.showNoticeType(isWales && noticeServedYes);

        // Wales-only PCSC + Required documents section gates.
        p.showPcscSection(isWales);
        p.showRequiredDocumentsSection(isWales);
    }

    // -----------------------------------------------------------------------
    // PcsCaseEntity — §13.2 "From PcsCaseEntity"
    // -----------------------------------------------------------------------
    private void mapFromPcsCase(PcsCaseEntity pcsCase, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        boolean isWales = pcsCase.getLegislativeCountry() == LegislativeCountry.WALES;
        p.isWales(isWales);
        p.isEngland(!isWales);
        AddressEntity propertyAddress = pcsCase.getPropertyAddress();
        p.propertyAddress(toClaimPackAddress(propertyAddress));
        if (propertyAddress != null) {
            p.hasPropertyAddressLine2(isPopulated(propertyAddress.getAddressLine2()));
            p.hasPropertyAddressLine3(isPopulated(propertyAddress.getAddressLine3()));
            p.hasPropertyCounty(isPopulated(propertyAddress.getCounty()));
        }
        p.referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()));
    }

    // -----------------------------------------------------------------------
    // ClaimEntity — §13.2 "From ClaimEntity"
    // -----------------------------------------------------------------------
    private void mapFromClaim(ClaimEntity claim, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        // §6.3.1
        if (claim.getClaimSubmittedDate() != null) {
            p.submittedOn(claim.getClaimSubmittedDate().toLocalDate());
        }
        // issueDateSealed is §13.3 gap (1) — left null.

        // §6.3.10 — Y/N to title-case labels; "Why not followed" row gated by complementary boolean.
        VerticalYesNo preActionEnum = claim.getPreActionProtocolFollowed();
        p.preActionProtocolFollowedYesNo(preActionEnum == null ? null : preActionEnum.getLabel());
        p.showPreActionProtocolNotFollowedReason(preActionEnum == VerticalYesNo.NO);
        p.preActionProtocolNotFollowedReason(claim.getPreActionProtocolIncompleteExplanation());

        VerticalYesNo mediationEnum = claim.getMediationAttempted();
        p.mediationAttemptedYesNo(mediationEnum == null ? null : mediationEnum.getLabel());
        VerticalYesNo settlementEnum = claim.getSettlementAttempted();
        p.settlementAttemptedYesNo(settlementEnum == null ? null : settlementEnum.getLabel());

        // §6.3.13 / §6.3.14 — title-case labels + details show-flag gated on YES.
        VerticalYesNo claimantCircsEnum = claim.getClaimantCircumstancesProvided();
        p.hasClaimantCircsYesNo(claimantCircsEnum == null ? null : claimantCircsEnum.getLabel());
        p.showClaimantCircsFreeText(claimantCircsEnum == VerticalYesNo.YES);
        p.claimantCircsFreeText(claim.getClaimantCircumstances());

        VerticalYesNo defendantCircsEnum = claim.getDefendantCircumstancesProvided();
        p.hasDefendantCircsYesNo(defendantCircsEnum == null ? null : defendantCircsEnum.getLabel());
        p.showDefendantCircsFreeText(defendantCircsEnum == VerticalYesNo.YES);
        p.defendantCircsFreeText(claim.getDefendantCircumstances());

        // §6.3.7 additional reasons row — title-case label for direct Docmosis render.
        VerticalYesNo additionalReasonsEnum = claim.getAdditionalReasonsProvided();
        p.hasAdditionalReasonsYesNo(additionalReasonsEnum == null ? null : additionalReasonsEnum.getLabel());
        p.additionalReasonsProvided(additionalReasonsEnum == VerticalYesNo.YES);
        p.additionalReasonsFreeText(claim.getAdditionalReasons());

        // §6.3.15 yes/no gate — title-case for direct template rendering.
        VerticalYesNo underlesseeEnum = claim.getUnderlesseeOrMortgagee();
        p.hasUnderlesseeYesNo(underlesseeEnum == null ? null : underlesseeEnum.getLabel());

        // §6.3.2 Wales-only row — convert to title-case "Yes"/"No" string for direct Docmosis render.
        p.claimantIsExemptLandlord(
            claim.getIsExemptLandlord() == null ? null : claim.getIsExemptLandlord().getLabel()
        );

        // whyClaimingPossession is §13.3 gap (2) — left null.
    }

    // -----------------------------------------------------------------------
    // PartyEntity (claimant) — §13.2 "From PartyEntity"
    // -----------------------------------------------------------------------
    private void mapClaimants(List<PartyEntity> claimants, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (claimants.isEmpty()) {
            return;
        }
        PartyEntity head = claimants.getFirst();
        ClaimPackParty mapped = toClaimPackParty(head);
        p.claimant(mapped);
        p.claimantDisplayName(deriveDisplayName(mapped));
        ClaimPackAddress addr = mapped.getAddress();
        p.hasClaimantAddressLine2(addr != null && isPopulated(addr.getAddressLine2()));
        p.hasClaimantAddressLine3(addr != null && isPopulated(addr.getAddressLine3()));
        p.hasClaimantCounty(addr != null && isPopulated(addr.getCounty()));
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
                               ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        List<ClaimPackDefendantRow> rows = new ArrayList<>(defendants.size());
        int number = 1;
        for (PartyEntity def : defendants) {
            rows.add(toDefendantRow(def, number++, propertyAddress));
        }
        p.defendants(rows);
    }

    private ClaimPackDefendantRow toDefendantRow(PartyEntity def, int number, AddressEntity propertyAddress) {
        AddressEntity addr = pickAddressOrFallback(def.getAddress(), propertyAddress);
        return ClaimPackDefendantRow.builder()
            .defendantNumber(number)
            .heading("Defendant " + number + " details")
            .displayName(derivePartyDisplayName(def))
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

    private static String derivePartyDisplayName(PartyEntity p) {
        if (isPopulated(p.getOrgName())) {
            return p.getOrgName();
        }
        if (p.getNameKnown() == VerticalYesNo.NO) {
            return "Persons unknown";
        }
        boolean hasFirst = isPopulated(p.getFirstName());
        boolean hasLast = isPopulated(p.getLastName());
        if (!hasFirst && !hasLast) {
            return "Persons unknown";
        }
        return (hasFirst ? p.getFirstName() : "") + (hasFirst && hasLast ? " " : "")
            + (hasLast ? p.getLastName() : "");
    }

    // -----------------------------------------------------------------------
    // PartyEntity (underlessees / mortgagees) — §13.2 "From PartyEntity"
    //
    // Flat list with Address-unknown semantic per Excel row 58/60: address either
    // provided (render 6 lines) or unknown (render "Address unknown" single line).
    // -----------------------------------------------------------------------
    private void mapUnderlessees(List<PartyEntity> underlessees,
                                 ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        List<ClaimPackUnderlesseeRow> rows = new ArrayList<>(underlessees.size());
        int number = 1;
        for (PartyEntity u : underlessees) {
            rows.add(toUnderlesseeRow(u, number++));
        }
        p.underlessees(rows);
    }

    private ClaimPackUnderlesseeRow toUnderlesseeRow(PartyEntity u, int number) {
        AddressEntity addr = u.getAddress();
        boolean addressKnown = addr != null && isPopulated(addr.getAddressLine1());
        ClaimPackUnderlesseeRow.ClaimPackUnderlesseeRowBuilder b = ClaimPackUnderlesseeRow.builder()
            .underlesseeNumber(number)
            .heading(number == 1
                ? "Underlessee or mortgagee 1 details"
                : "Additional underlessee or mortgagee " + (number - 1) + " details")
            .displayName(derivePartyDisplayName(u))
            .addressKnown(addressKnown)
            .addressUnknown(!addressKnown);
        if (addressKnown) {
            b.addressLine1(addr.getAddressLine1());
            b.addressLine2(addr.getAddressLine2());
            b.addressLine3(addr.getAddressLine3());
            b.postTown(addr.getPostTown());
            b.county(addr.getCounty());
            b.postcode(addr.getPostcode());
            b.hasAddressLine2(isPopulated(addr.getAddressLine2()));
            b.hasAddressLine3(isPopulated(addr.getAddressLine3()));
            b.hasCounty(isPopulated(addr.getCounty()));
        }
        return b.build();
    }

    // -----------------------------------------------------------------------
    // ClaimGroundEntity (collection) — §13.2 "From ClaimGroundEntity"
    // -----------------------------------------------------------------------
    private void mapFromGrounds(Set<ClaimGroundEntity> grounds, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (grounds == null || grounds.isEmpty()) {
            p.grounds(Collections.emptyList());
            p.groundsWithReasons(Collections.emptyList());
            p.hasGroundsYesNo(VerticalYesNo.NO.getLabel());
            p.hasRentArrearsGround(false);
            p.hasAsbGround(false);
            p.hasOtherGround(false);
            p.isNoOrAbsoluteOrOtherGrounds(true); // no grounds case
            return;
        }

        List<ClaimPackGround> mapped = grounds.stream()
            .map(g -> ClaimPackGround.builder()
                .nameAndNumber(formatGroundLabel(g))
                .reasonFreeText(g.getReason())
                .hasReason(g.getReason() != null && !g.getReason().isBlank())
                .build())
            .toList();
        p.grounds(mapped);
        p.groundsWithReasons(mapped.stream().filter(ClaimPackGround::isHasReason).toList());
        p.hasGroundsYesNo(VerticalYesNo.YES.getLabel());

        boolean hasRentArrears = grounds.stream()
            .anyMatch(g -> Boolean.TRUE.equals(g.getIsRentArrears()));
        p.hasRentArrearsGround(hasRentArrears);

        boolean hasAsb = grounds.stream()
            .filter(g -> g.getCategory() != null)
            .anyMatch(g -> g.getCategory().name().contains("ANTISOCIAL"));
        p.hasAsbGround(hasAsb);

        boolean hasOther = grounds.stream()
            .filter(g -> g.getCategory() != null && g.getCode() != null)
            .anyMatch(g -> g.getCategory().name().contains("OTHER") || "OTHER".equals(g.getCode()));
        p.hasOtherGround(hasOther);

        // First "Other" ground's description, if any — see plan §13.5 (refine once code conventions confirmed).
        grounds.stream()
            .filter(g -> g.getCategory() != null && g.getCategory().name().contains("OTHER"))
            .map(ClaimGroundEntity::getDescription)
            .filter(d -> d != null && !d.isBlank())
            .findFirst()
            .ifPresent(p::otherGroundsDescription);

        boolean hasAbsolute = grounds.stream()
            .filter(g -> g.getCategory() != null)
            .anyMatch(g -> g.getCategory().name().contains("ABSOLUTE"));
        p.isNoOrAbsoluteOrOtherGrounds(hasAbsolute || hasOther);
    }

    // -----------------------------------------------------------------------
    // NoticeOfPossessionEntity — §13.2 "From NoticeOfPossessionEntity"
    // -----------------------------------------------------------------------
    private void mapFromNotice(NoticeOfPossessionEntity notice, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (notice == null) {
            return;
        }
        // YesOrNo (CCD SDK) → title-case label; same bridge but rendered directly.
        VerticalYesNo noticeServedEnum = yesOrNoToVertical(notice.getNoticeServed());
        p.noticeServedYesNo(noticeServedEnum == null ? null : noticeServedEnum.getLabel());
        boolean notServed = noticeServedEnum == VerticalYesNo.NO;
        p.noticeNotServedDisplayed(notServed);
        // Positive boolean — gates the "Method of service onwards" sub-table.
        p.noticeServedYes(noticeServedEnum == VerticalYesNo.YES);

        // Optional-value-presence show-flags (Excel mapping rows 37-42).
        p.showNoticeServedOn(notice.getNoticeDate() != null);
        p.showNoticeServedTime(notice.getNoticeDateTime() != null);
        p.noticeNotServedReason(notice.getNoticeStatement());
        p.noticeType(notice.getNoticeType());

        NoticeServiceMethod method = notice.getServingMethod();
        p.methodOfService(method);
        if (method != null) {
            p.methodOfServiceLabel(method.getLabel());
            p.methodRequiresTime(METHODS_REQUIRING_TIME.contains(method));
        }

        p.noticeServedOn(notice.getNoticeDate());
        if (notice.getNoticeDateTime() != null) {
            p.noticeServedTime(notice.getNoticeDateTime().toLocalTime());
        }

        // Route the single noticeDetails column to whichever per-method payload field renders.
        String details = notice.getNoticeDetails();
        if (method != null && details != null) {
            switch (method) {
                case PERSONALLY_HANDED -> {
                    p.noticeLeftWithName(details);
                    p.showNoticeLeftWithName(isPopulated(details));
                }
                case EMAIL -> {
                    p.noticeServedToEmail(details);
                    p.showNoticeServedToEmail(isPopulated(details));
                }
                case OTHER_ELECTRONIC -> {
                    p.noticeOtherElectronicDetails(details);
                    p.showNoticeOtherElectronicDetails(isPopulated(details));
                }
                case OTHER -> {
                    p.noticeOtherMeansDetails(details);
                    p.showNoticeOtherMeansDetails(isPopulated(details));
                }
                default -> {
                    // FIRST_CLASS_POST, DELIVERED_PERMITTED_PLACE — no detail row in §6.3.11.
                }
            }
        }
        // noticeUploadedYesNo and noticeNotUploadedReason — see plan §13.3 gap (3) and the
        // ClaimDocumentEntity-driven derivation; deferred to follow-up.
        // The complementary booleans below will gate "Yes, a copy attached"/"No" conditionals
        // once that source field is wired — for now both are false (null upstream).
        VerticalYesNo uploaded = null; // placeholder until §13.3 gap (3) is closed
        p.noticeUploadedYes(uploaded == VerticalYesNo.YES);
        p.noticeUploadedNo(uploaded == VerticalYesNo.NO);
    }

    // -----------------------------------------------------------------------
    // TenancyLicenceEntity — §13.2 "From TenancyLicenceEntity"
    // -----------------------------------------------------------------------
    private void mapFromTenancyLicence(TenancyLicenceEntity tenancy,
                                       ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (tenancy == null) {
            return;
        }
        if (tenancy.getType() != null) {
            p.isIntroDemotedOtherTenancy(INTRO_DEMOTED_OTHER_TYPES.contains(tenancy.getType()));
            p.tenancyTypeLabel(formatTenancyLabel(tenancy));
        }
        p.tenancyStartDate(tenancy.getStartDate());
        p.showTenancyStartDate(tenancy.getStartDate() != null);
        VerticalYesNo tenancyUploaded = tenancy.getHasCopyOfTenancyLicence();
        p.tenancyUploadedYesNo(tenancyUploaded);
        p.tenancyUploadedYes(tenancyUploaded == VerticalYesNo.YES);
        p.tenancyUploadedNo(tenancyUploaded == VerticalYesNo.NO);
        p.tenancyNotUploadedReason(tenancy.getReasonsForNoTenancyLicence());
        p.rentAmount(tenancy.getRentAmount());
        p.rentCalculatedDescription(formatRentDescription(tenancy));
    }

    // -----------------------------------------------------------------------
    // RentArrearsEntity — §13.2 "From RentArrearsEntity"
    // -----------------------------------------------------------------------
    private void mapFromRentArrears(RentArrearsEntity rent, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (rent == null) {
            return;
        }
        p.rentArrearsTotal(rent.getTotalRentArrears());
        VerticalYesNo judgmentEnum = rent.getArrearsJudgmentWanted();
        p.judgmentRequestedYesNo(judgmentEnum == null ? null : judgmentEnum.getLabel());
        // "Details of previous steps" row only renders when previous-steps Y/N is YES.
        VerticalYesNo prevStepsEnum = rent.getRecoveryAttempted();
        p.hasPreviousStepsYesNo(prevStepsEnum == null ? null : prevStepsEnum.getLabel());
        p.showPreviousStepsFreeText(prevStepsEnum == VerticalYesNo.YES);
        p.previousStepsFreeText(rent.getRecoveryAttemptDetails());
    }

    // -----------------------------------------------------------------------
    // AsbProhibitedConductEntity — §13.2 "From AsbProhibitedConductEntity"
    // -----------------------------------------------------------------------
    private void mapFromAsbProhibitedConduct(AsbProhibitedConductEntity asb,
                                             ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (asb == null) {
            return;
        }
        // §6.3.8 — title-case Y/N strings for direct template rendering; per-row show
        // flags follow each enum's YES/NO state.
        VerticalYesNo asbEnum = asb.getAntisocialBehaviour();
        p.asbAllegedYesNo(asbEnum == null ? null : asbEnum.getLabel());
        p.showAsbDetails(asbEnum == VerticalYesNo.YES);
        p.asbDetailsFreeText(asb.getAntisocialBehaviourDetails());

        VerticalYesNo illegalUseEnum = asb.getIllegalPurposes();
        p.illegalUseAllegedYesNo(illegalUseEnum == null ? null : illegalUseEnum.getLabel());
        p.showIllegalUseDetails(illegalUseEnum == VerticalYesNo.YES);
        p.illegalUseDetailsFreeText(asb.getIllegalPurposesDetails());

        VerticalYesNo otherProhibitedEnum = asb.getOtherProhibitedConduct();
        p.otherProhibitedAllegedYesNo(otherProhibitedEnum == null ? null : otherProhibitedEnum.getLabel());
        p.showOtherProhibitedDetails(otherProhibitedEnum == VerticalYesNo.YES);
        p.otherProhibitedDetailsFreeText(asb.getOtherProhibitedConductDetails());

        // §6.3.18 PCSC (Wales) — yes, same entity (see plan §13.2 note).
        // Title-case labels + nested show-flags. showPcscSection (outer) is computed in
        // mapClaimDetailsShowFlags since it depends on country.
        VerticalYesNo pcscEnum = asb.getClaimingStandardContract();
        p.isPcscYesNo(pcscEnum == null ? null : pcscEnum.getLabel());
        p.showPcscDetails(pcscEnum == VerticalYesNo.YES);
        p.pcscReasonFreeText(asb.getClaimingStandardContractDetails());

        VerticalYesNo pcscTermsEnum = asb.getPeriodicContractAgreed();
        p.pcscTermsAgreedYesNo(pcscTermsEnum == null ? null : pcscTermsEnum.getLabel());
        p.showPcscTermsFreeText(pcscTermsEnum == VerticalYesNo.YES);
        p.pcscTermsFreeText(asb.getPeriodicContractDetails());
    }

    // -----------------------------------------------------------------------
    // PossessionAlternativesEntity — §13.2 "From PossessionAlternativesEntity"
    // -----------------------------------------------------------------------
    private void mapFromPossessionAlternatives(PossessionAlternativesEntity alt,
                                               ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (alt == null) {
            return;
        }
        // Demotion — optional Y/N: row hidden if user didn't answer; follow-ups gated on YES.
        VerticalYesNo demotionEnum = yesOrNoToVertical(alt.getDotRequested());
        p.showIsDemotionClaim(demotionEnum != null);
        p.isDemotionClaimYesNo(demotionEnum == null ? null : demotionEnum.getLabel());
        p.showDemotionDetails(demotionEnum == VerticalYesNo.YES);
        if (alt.getDotHousingActSection() != null) {
            p.demotionHousingActSection(alt.getDotHousingActSection().name());
        }
        VerticalYesNo demoTermsEnum = yesOrNoToVertical(alt.getDotStatementServed());
        p.hasServedDemotionTermsYesNo(demoTermsEnum == null ? null : demoTermsEnum.getLabel());
        p.showDemotionTermsFreeText(demoTermsEnum == VerticalYesNo.YES);
        p.demotionTermsFreeText(alt.getDotStatementDetails());
        p.demotionReasonsFreeText(alt.getDotReason());

        // Suspension — same shape (optional Y/N + follow-ups gated on YES).
        VerticalYesNo suspensionEnum = yesOrNoToVertical(alt.getSuspensionOfRTB());
        p.showIsSuspensionClaim(suspensionEnum != null);
        p.isSuspensionClaimYesNo(suspensionEnum == null ? null : suspensionEnum.getLabel());
        p.showSuspensionDetails(suspensionEnum == VerticalYesNo.YES);
        if (alt.getSuspensionOfRTBHousingActSection() != null) {
            p.suspensionHousingActSection(alt.getSuspensionOfRTBHousingActSection().name());
        }
        p.suspensionReasonsFreeText(alt.getSuspensionOfRTBReason());
    }

    // -----------------------------------------------------------------------
    // StatementOfTruthEntity — §13.2 "From StatementOfTruthEntity"
    // -----------------------------------------------------------------------
    private void mapFromStatementOfTruth(StatementOfTruthEntity sot,
                                         ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (sot == null) {
            return;
        }
        p.signedByLegalRep(sot.getCompletedBy() != null
            && "LEGAL_REPRESENTATIVE".equals(sot.getCompletedBy().name()));
        p.sotFullName(sot.getFullName());
        p.sotFirmName(sot.getFirmName());
        p.sotPositionHeld(sot.getPositionHeld());
    }

    // -----------------------------------------------------------------------
    // Derived — §13.2 "From helpers / computed"
    // -----------------------------------------------------------------------
    private void mapCaseName(List<PartyEntity> claimants,
                             List<PartyEntity> defendants,
                             ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (claimants.isEmpty() && defendants.isEmpty()) {
            return;
        }
        List<Party> claimantDomain = claimants.stream().map(ClaimPackPayloadBuilder::toDomainParty).toList();
        List<Party> defendantDomain = defendants.stream().map(ClaimPackPayloadBuilder::toDomainParty).toList();
        p.caseName(caseNameFormatter.formatCaseName(claimantDomain, defendantDomain));
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

    private ClaimPackParty toClaimPackParty(PartyEntity p) {
        boolean unknown = p.getNameKnown() == VerticalYesNo.NO;
        return ClaimPackParty.builder()
            .firstName(p.getFirstName())
            .lastName(p.getLastName())
            .orgName(p.getOrgName())
            .isPersonsUnknown(unknown)
            .address(toClaimPackAddress(p.getAddress()))
            .build();
    }

    private ClaimPackAddress toClaimPackAddress(AddressEntity a) {
        if (a == null) {
            return null;
        }
        return ClaimPackAddress.builder()
            .addressLine1(a.getAddressLine1())
            .addressLine2(a.getAddressLine2())
            .addressLine3(a.getAddressLine3())
            .postTown(a.getPostTown())
            .county(a.getCounty())
            .postcode(a.getPostcode())
            .country(a.getCountry())
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
        // Placeholder formatter — refine once the ground-code to label convention is confirmed
        // (see plan §13.5). For now: "<category>: <code>" so the template at least has a string.
        if (g.getCode() == null) {
            return g.getCategory() != null ? g.getCategory().name() : "";
        }
        return g.getCategory() != null ? g.getCategory().name() + ": " + g.getCode() : g.getCode();
    }

    private static String formatTenancyLabel(TenancyLicenceEntity t) {
        if (t.getType() == CombinedLicenceType.OTHER && t.getOtherTypeDetails() != null) {
            return CombinedLicenceType.OTHER.name() + ": " + t.getOtherTypeDetails();
        }
        return t.getType() != null ? t.getType().name() : null;
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

    private static boolean isPopulated(String s) {
        return s != null && !s.isBlank();
    }

    private static String formatRentDescription(TenancyLicenceEntity t) {
        if (t.getRentAmount() == null || t.getRentFrequency() == null) {
            return null;
        }
        return "£" + t.getRentAmount() + " (" + t.getRentFrequency().name() + ")";
    }

}
