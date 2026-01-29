package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantProvidedInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantProvided;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
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

        DefendantProvided defendantProvided = buildDefendantProvidedInfo(defendantEntity, contactAddress);

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
        Party party = buildPartyFromDefendantEntity(defendantEntity, contactAddress, claimantOrgName);

        return ClaimantProvidedInfo.builder()
            .party(party)
            .legislativeCountry(caseEntity.getLegislativeCountry())
            .tenancyType(extractTenancyType(caseEntity))
            .tenancyStartDate(extractTenancyStartDate(caseEntity))
            .dailyRentAmount(extractDailyRentAmount(caseEntity))
            .rentArrearsOwed(extractRentArrearsOwed(caseEntity))
            .noticeServed(extractNoticeServed(caseEntity))
            .noticeDate(extractNoticeDate(caseEntity))
            .build();
    }

    private DefendantProvided buildDefendantProvidedInfo(PartyEntity defendantEntity, AddressUK contactAddress) {
        Party defendantEditableParty = buildDefendantEditableParty(defendantEntity, contactAddress);

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(defendantEditableParty)
            .contactByPhone(extractContactByPhone(defendantEntity))
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .build();

        return DefendantProvided.builder()
            .contactDetails(contactDetails)
            .responses(responses)
            .build();
    }

    private Party buildDefendantEditableParty(PartyEntity defendantEntity, AddressUK contactAddress) {
        return Party.builder()
            .firstName(defendantEntity.getFirstName())
            .lastName(defendantEntity.getLastName())
            .emailAddress(defendantEntity.getEmailAddress())
            .address(contactAddress)
            .phoneNumber(defendantEntity.getPhoneNumber())
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
        AddressUK contactAddress,
        String claimantOrgName
    ) {
        return Party.builder()
            .firstName(defendantEntity.getFirstName())
            .lastName(defendantEntity.getLastName())
            .orgName(claimantOrgName)
            .nameKnown(defendantEntity.getNameKnown())
            .emailAddress(defendantEntity.getEmailAddress())
            .address(contactAddress)
            .addressKnown(defendantEntity.getAddressKnown())
            .addressSameAsProperty(defendantEntity.getAddressSameAsProperty())
            .phoneNumber(defendantEntity.getPhoneNumber())
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
        TenancyLicence tenancy = caseEntity.getTenancyLicence();
        if (tenancy == null) {
            return null;
        }

        if (caseEntity.getLegislativeCountry() == LegislativeCountry.WALES) {
            return tenancy.getOccupationLicenceTypeWales() != null
                ? tenancy.getOccupationLicenceTypeWales().getLabel()
                : null;
        }

        return tenancy.getTenancyLicenceType();
    }

    private LocalDate extractTenancyStartDate(PcsCaseEntity caseEntity) {
        TenancyLicence tenancy = caseEntity.getTenancyLicence();
        if (tenancy == null) {
            return null;
        }

        if (caseEntity.getLegislativeCountry() == LegislativeCountry.WALES) {
            return tenancy.getWalesLicenceStartDate();
        }

        return tenancy.getTenancyLicenceDate();
    }

    private BigDecimal extractDailyRentAmount(PcsCaseEntity caseEntity) {
        TenancyLicence tenancy = caseEntity.getTenancyLicence();
        return tenancy != null ? tenancy.getDailyRentChargeAmount() : null;
    }

    private BigDecimal extractRentArrearsOwed(PcsCaseEntity caseEntity) {
        TenancyLicence tenancy = caseEntity.getTenancyLicence();
        return tenancy != null ? tenancy.getTotalRentArrears() : null;
    }

    private YesOrNo extractContactByPhone(PartyEntity defendantEntity) {
        VerticalYesNo phoneNumberProvided = defendantEntity.getPhoneNumberProvided();

        if (phoneNumberProvided == null) {
            return null;
        }

        return phoneNumberProvided == VerticalYesNo.YES ? YesOrNo.YES : YesOrNo.NO;
    }

    private YesOrNo extractNoticeServed(PcsCaseEntity caseEntity) {
        TenancyLicence tenancy = caseEntity.getTenancyLicence();
        if (tenancy == null) {
            return null;
        }

        Boolean noticeServed;
        if (caseEntity.getLegislativeCountry() == LegislativeCountry.WALES) {
            noticeServed = tenancy.getWalesNoticeServed();
        } else {
            noticeServed = tenancy.getNoticeServed();
        }

        if (noticeServed == null) {
            return null;
        }

        return noticeServed ? YesOrNo.YES : YesOrNo.NO;
    }

    private LocalDateTime extractNoticeDate(PcsCaseEntity caseEntity) {
        TenancyLicence tenancy = caseEntity.getTenancyLicence();
        if (tenancy == null) {
            return null;
        }

        // Priority order: posted > delivered > handed over > email > other electronic > other
        if (tenancy.getNoticePostedDate() != null) {
            return tenancy.getNoticePostedDate().atStartOfDay();
        }
        if (tenancy.getNoticeDeliveredDate() != null) {
            return tenancy.getNoticeDeliveredDate().atStartOfDay();
        }
        if (tenancy.getNoticeHandedOverDateTime() != null) {
            return tenancy.getNoticeHandedOverDateTime();
        }
        if (tenancy.getNoticeEmailSentDateTime() != null) {
            return tenancy.getNoticeEmailSentDateTime();
        }
        if (tenancy.getNoticeOtherElectronicDateTime() != null) {
            return tenancy.getNoticeOtherElectronicDateTime();
        }
        if (tenancy.getNoticeOtherDateTime() != null) {
            return tenancy.getNoticeOtherDateTime();
        }

        return null;
    }
}
