package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ClaimantProvidedInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantProvided;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps database entities (PcsCaseEntity, PartyEntity) to domain objects (PossessionClaimResponse).
 *
 * <p>This service contains all logic for transforming database entities into the PossessionClaimResponse
 * structure used by CCD. It handles:
 * - Claimant-provided information (party details, tenancy, rent, notices)
 * - Defendant-provided information (contact details, responses)
 * - Legislative country-specific logic (England vs Wales)
 * - Address resolution (defendant address vs property address)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PossessionClaimResponseMapper {

    private final AddressMapper addressMapper;

    /**
     * Maps case entity and defendant entity to PossessionClaimResponse.
     *
     * @param caseEntity The loaded case entity (with claims, tenancy, property address, etc.)
     * @param defendantEntity The validated defendant party entity
     * @return PossessionClaimResponse with claimantProvided and defendantProvided sections
     */
    public PossessionClaimResponse mapFrom(PcsCaseEntity caseEntity, PartyEntity defendantEntity) {
        AddressUK contactAddress = resolveAddress(defendantEntity, caseEntity);
        String claimantOrgName = extractClaimantOrgName(caseEntity);

        ClaimantProvidedInfo claimantProvided = buildClaimantProvidedInfo(
            defendantEntity,
            caseEntity,
            contactAddress,
            claimantOrgName
        );

        DefendantProvided defendantProvided = buildDefendantProvidedInfo(
            defendantEntity,
            contactAddress,
            claimantOrgName
        );

        return PossessionClaimResponse.builder()
            .claimantProvided(claimantProvided)
            .defendantProvided(defendantProvided)
            .build();
    }

    private ClaimantProvidedInfo buildClaimantProvidedInfo(
        PartyEntity defendantEntity,
        PcsCaseEntity caseEntity,
        AddressUK contactAddress,
        String claimantOrgName
    ) {
        Party party = buildPartyFromDefendantEntity(defendantEntity, contactAddress);

        return ClaimantProvidedInfo.builder()
            .party(party)
            .claimantOrg(claimantOrgName)
            .legislativeCountry(caseEntity.getLegislativeCountry())
            .tenancyType(extractTenancyType(caseEntity))
            .tenancyStartDate(extractTenancyStartDate(caseEntity))
            .dailyRentAmount(extractDailyRentAmount(caseEntity))
            .rentArrearsOwed(extractRentArrearsOwed(caseEntity))
            .noticeServed(extractNoticeServed(caseEntity))
            .noticeDate(extractNoticeDate(caseEntity))
            .build();
    }

    private DefendantProvided buildDefendantProvidedInfo(
        PartyEntity defendantEntity,
        AddressUK contactAddress,
        String claimantOrgName
    ) {
        // Reuse the same Party builder as claimantProvided to ensure all fields match on first load
        Party defendantEditableParty = buildPartyFromDefendantEntity(
            defendantEntity,
            contactAddress
        );

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(defendantEditableParty)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .build();

        return DefendantProvided.builder()
            .contactDetails(contactDetails)
            .responses(responses)
            .build();
    }

    private AddressUK resolveAddress(PartyEntity defendantEntity, PcsCaseEntity caseEntity) {
        if (defendantEntity.getAddressSameAsProperty() != null
            && defendantEntity.getAddressSameAsProperty() == VerticalYesNo.YES) {
            return addressMapper.toAddressUK(caseEntity.getPropertyAddress());
        } else {
            return addressMapper.toAddressUK(defendantEntity.getAddress());
        }
    }

    private Party buildPartyFromDefendantEntity(
        PartyEntity defendantEntity,
        AddressUK contactAddress
    ) {
        return Party.builder()
            .firstName(defendantEntity.getFirstName())
            .lastName(defendantEntity.getLastName())
            .nameKnown(defendantEntity.getNameKnown())
            .emailAddress(defendantEntity.getEmailAddress())
            .address(contactAddress)
            .addressKnown(defendantEntity.getAddressKnown())
            .addressSameAsProperty(defendantEntity.getAddressSameAsProperty())
            .phoneNumber(defendantEntity.getPhoneNumber())
            .phoneNumberProvided(defendantEntity.getPhoneNumberProvided())
            .build();
    }

    private String extractClaimantOrgName(PcsCaseEntity caseEntity) {
        long caseReference = caseEntity.getCaseReference();
        ClaimEntity mainClaim = caseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> {
                log.error("No claim found for case {}", caseReference);
                return new CaseAccessException("No claim found for this case");
            });

        return mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT)
            .map(ClaimPartyEntity::getParty)
            .map(PartyEntity::getOrgName)
            .findFirst()
            .orElse(null);
    }

    private String extractTenancyType(PcsCaseEntity caseEntity) {
        TenancyLicenceEntity tenancy = caseEntity.getTenancyLicence();
        if (tenancy == null || tenancy.getType() == null) {
            return null;
        }

        CombinedLicenceType combinedType = tenancy.getType();

        // Convert CombinedLicenceType to label based on legislative country
        if (caseEntity.getLegislativeCountry() == LegislativeCountry.WALES) {
            // Wales uses OccupationLicenceTypeWales
            OccupationLicenceTypeWales walesType = OccupationLicenceTypeWales.from(combinedType);
            return walesType != null ? walesType.getLabel() : null;
        } else {
            // England uses TenancyLicenceType
            TenancyLicenceType englandType = TenancyLicenceType.from(combinedType);
            return englandType != null ? englandType.getLabel() : null;
        }
    }

    private LocalDate extractTenancyStartDate(PcsCaseEntity caseEntity) {
        TenancyLicenceEntity tenancy = caseEntity.getTenancyLicence();
        if (tenancy == null) {
            return null;
        }

        return tenancy.getStartDate();
    }

    private BigDecimal extractDailyRentAmount(PcsCaseEntity caseEntity) {
        TenancyLicenceEntity tenancy = caseEntity.getTenancyLicence();
        return tenancy != null ? tenancy.getRentPerDay() : null;
    }

    private BigDecimal extractRentArrearsOwed(PcsCaseEntity caseEntity) {
        ClaimEntity claim = caseEntity.getClaims().stream().findFirst().orElse(null);
        if (claim == null || claim.getRentArrears() == null) {
            return null;
        }
        return claim.getRentArrears().getTotalRentArrears();
    }

    private YesOrNo extractNoticeServed(PcsCaseEntity caseEntity) {
        ClaimEntity claim = caseEntity.getClaims().stream().findFirst().orElse(null);
        if (claim == null || claim.getNoticeOfPossession() == null) {
            return null;
        }
        return claim.getNoticeOfPossession().getNoticeServed();
    }

    private LocalDateTime extractNoticeDate(PcsCaseEntity caseEntity) {
        ClaimEntity claim = caseEntity.getClaims().stream().findFirst().orElse(null);
        if (claim == null || claim.getNoticeOfPossession() == null) {
            return null;
        }

        NoticeOfPossessionEntity notice = claim.getNoticeOfPossession();

        // Priority: use noticeDateTime if available, otherwise convert noticeDate
        if (notice.getNoticeDateTime() != null) {
            return notice.getNoticeDateTime();
        }

        if (notice.getNoticeDate() != null) {
            return notice.getNoticeDate().atStartOfDay();
        }

        return null;
    }
}
