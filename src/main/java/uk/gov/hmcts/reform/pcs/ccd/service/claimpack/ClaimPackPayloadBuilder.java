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
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackGround;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackParty;
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
        mapDefendants(defendants, p);
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

        return p.build();
    }

    // -----------------------------------------------------------------------
    // PcsCaseEntity — §13.2 "From PcsCaseEntity"
    // -----------------------------------------------------------------------
    private void mapFromPcsCase(PcsCaseEntity pcsCase, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        p.isWales(pcsCase.getLegislativeCountry() == LegislativeCountry.WALES);
        p.propertyAddress(toClaimPackAddress(pcsCase.getPropertyAddress()));
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

        // §6.3.10
        p.preActionProtocolFollowedYesNo(claim.getPreActionProtocolFollowed());
        p.preActionProtocolNotFollowedReason(claim.getPreActionProtocolIncompleteExplanation());
        p.mediationAttemptedYesNo(claim.getMediationAttempted());
        p.settlementAttemptedYesNo(claim.getSettlementAttempted());

        // §6.3.13 / §6.3.14
        p.hasClaimantCircsYesNo(claim.getClaimantCircumstancesProvided());
        p.claimantCircsFreeText(claim.getClaimantCircumstances());
        p.hasDefendantCircsYesNo(claim.getDefendantCircumstancesProvided());
        p.defendantCircsFreeText(claim.getDefendantCircumstances());

        // §6.3.7 additional reasons row
        p.hasAdditionalReasonsYesNo(claim.getAdditionalReasonsProvided());
        p.additionalReasonsProvided(claim.getAdditionalReasonsProvided() == VerticalYesNo.YES);
        p.additionalReasonsFreeText(claim.getAdditionalReasons());

        // §6.3.15 yes/no gate (parties themselves are mapped separately)
        p.hasUnderlesseeYesNo(claim.getUnderlesseeOrMortgagee());

        // §6.3.2 Wales-only row
        p.claimantIsExemptLandlord(claim.getIsExemptLandlord());

        // whyClaimingPossession is §13.3 gap (2) — left null.
    }

    // -----------------------------------------------------------------------
    // PartyEntity (claimant) — §13.2 "From PartyEntity"
    // -----------------------------------------------------------------------
    private void mapClaimants(List<PartyEntity> claimants, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (!claimants.isEmpty()) {
            p.claimant(toClaimPackParty(claimants.getFirst()));
        }
    }

    // -----------------------------------------------------------------------
    // PartyEntity (defendants) — §13.2 "From PartyEntity"
    // -----------------------------------------------------------------------
    private void mapDefendants(List<PartyEntity> defendants, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (!defendants.isEmpty()) {
            p.defendant1(toClaimPackParty(defendants.getFirst()));
            if (defendants.size() > 1) {
                p.additionalDefendants(
                    defendants.subList(1, defendants.size()).stream()
                        .map(this::toClaimPackParty)
                        .toList()
                );
            } else {
                p.additionalDefendants(Collections.emptyList());
            }
        }
    }

    // -----------------------------------------------------------------------
    // PartyEntity (underlessees / mortgagees) — §13.2 "From PartyEntity"
    // -----------------------------------------------------------------------
    private void mapUnderlessees(List<PartyEntity> underlessees, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (!underlessees.isEmpty()) {
            p.underlessee1(toClaimPackParty(underlessees.getFirst()));
            if (underlessees.size() > 1) {
                p.additionalUnderlessees(
                    underlessees.subList(1, underlessees.size()).stream()
                        .map(this::toClaimPackParty)
                        .toList()
                );
            } else {
                p.additionalUnderlessees(Collections.emptyList());
            }
        }
    }

    // -----------------------------------------------------------------------
    // ClaimGroundEntity (collection) — §13.2 "From ClaimGroundEntity"
    // -----------------------------------------------------------------------
    private void mapFromGrounds(Set<ClaimGroundEntity> grounds, ClaimPackFormPayload.ClaimPackFormPayloadBuilder p) {
        if (grounds == null || grounds.isEmpty()) {
            p.grounds(Collections.emptyList());
            p.hasGroundsYesNo(VerticalYesNo.NO);
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
        p.hasGroundsYesNo(VerticalYesNo.YES);

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
        // YesOrNo (CCD SDK) -> VerticalYesNo bridge needed for noticeServed / noticeUploaded
        p.noticeServedYesNo(yesOrNoToVertical(notice.getNoticeServed()));
        boolean notServed = notice.getNoticeServed() != null
            && "NO".equalsIgnoreCase(notice.getNoticeServed().name());
        p.noticeNotServedDisplayed(notServed);
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
                case PERSONALLY_HANDED -> p.noticeLeftWithName(details);
                case EMAIL -> p.noticeServedToEmail(details);
                case OTHER_ELECTRONIC -> p.noticeOtherElectronicDetails(details);
                case OTHER -> p.noticeOtherMeansDetails(details);
                default -> {
                    // FIRST_CLASS_POST, DELIVERED_PERMITTED_PLACE — no detail row in §6.3.11.
                }
            }
        }
        // noticeUploadedYesNo and noticeNotUploadedReason — see plan §13.3 gap (3) and the
        // ClaimDocumentEntity-driven derivation; deferred to follow-up.
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
        p.tenancyUploadedYesNo(tenancy.getHasCopyOfTenancyLicence());
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
        p.judgmentRequestedYesNo(rent.getArrearsJudgmentWanted());
        p.hasPreviousStepsYesNo(rent.getRecoveryAttempted());
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
        // §6.3.8
        p.asbAllegedYesNo(asb.getAntisocialBehaviour());
        p.asbDetailsFreeText(asb.getAntisocialBehaviourDetails());
        p.illegalUseAllegedYesNo(asb.getIllegalPurposes());
        p.illegalUseDetailsFreeText(asb.getIllegalPurposesDetails());
        p.otherProhibitedAllegedYesNo(asb.getOtherProhibitedConduct());
        p.otherProhibitedDetailsFreeText(asb.getOtherProhibitedConductDetails());

        // §6.3.18 PCSC (Wales) — yes, same entity (see plan §13.2 note).
        p.isPcscYesNo(asb.getClaimingStandardContract());
        p.pcscReasonFreeText(asb.getClaimingStandardContractDetails());
        p.pcscTermsAgreedYesNo(asb.getPeriodicContractAgreed());
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
        p.isDemotionClaimYesNo(yesOrNoToVertical(alt.getDotRequested()));
        if (alt.getDotHousingActSection() != null) {
            p.demotionHousingActSection(alt.getDotHousingActSection().name());
        }
        p.hasServedDemotionTermsYesNo(yesOrNoToVertical(alt.getDotStatementServed()));
        p.demotionTermsFreeText(alt.getDotStatementDetails());
        p.demotionReasonsFreeText(alt.getDotReason());

        p.isSuspensionClaimYesNo(yesOrNoToVertical(alt.getSuspensionOfRTB()));
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

    private static String formatRentDescription(TenancyLicenceEntity t) {
        if (t.getRentAmount() == null || t.getRentFrequency() == null) {
            return null;
        }
        return "£" + t.getRentAmount() + " (" + t.getRentFrequency().name() + ")";
    }

}
