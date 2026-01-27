package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicence;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimDraftService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartEventHandler implements Start<PCSCase, State> {

    private final PcsCaseService pcsCaseService;
    private final AddressMapper addressMapper;
    private final RespondPossessionClaimDraftService draftService;
    private final SecurityContextService securityContextService;

    @Override
    public PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseDataFromPayload = eventPayload.caseData();
        UUID authenticatedUserId = UUID.fromString(securityContextService.getCurrentUserDetails().getUid());

        if (draftService.exists(caseReference)) {
            return draftService.load(caseReference, caseDataFromPayload);
        }

        PartyEntity defendantEntity = validateAccess(caseReference, authenticatedUserId);
        PossessionClaimResponse initialResponse = buildInitialResponse(defendantEntity, caseReference);

        return draftService.initialize(caseReference, initialResponse, caseDataFromPayload);
    }

    private PartyEntity validateAccess(long caseReference, UUID authenticatedUserId) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        List<PartyEntity> defendants = extractDefendants(pcsCaseEntity, caseReference);
        return findMatchingDefendant(defendants, authenticatedUserId, caseReference);
    }

    private List<PartyEntity> extractDefendants(PcsCaseEntity pcsCaseEntity, long caseReference) {
        ClaimEntity mainClaim = pcsCaseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> {
                log.error("No claim found for case {}", caseReference);
                return new CaseAccessException("No claim found for this case");
            });

        List<PartyEntity> defendants = mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();

        if (defendants.isEmpty()) {
            log.error("No defendants found for case {}", caseReference);
            throw new CaseAccessException("No defendants associated with this case");
        }

        return defendants;
    }

    private PartyEntity findMatchingDefendant(
            List<PartyEntity> defendants,
            UUID authenticatedUserId,
            long caseReference) {
        return defendants.stream()
            .filter(defendant -> authenticatedUserId.equals(defendant.getIdamId()))
            .findFirst()
            .orElseThrow(() -> {
                log.error("Access denied: User {} is not linked as a defendant on case {}",
                    authenticatedUserId, caseReference);
                return new CaseAccessException("User is not linked as a defendant on this case");
            });
    }

    private PossessionClaimResponse buildInitialResponse(PartyEntity defendantEntity, long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        AddressUK contactAddress = resolveAddress(defendantEntity, caseReference);
        Party party = buildParty(defendantEntity, contactAddress);

        return PossessionClaimResponse.builder()
            .party(party)
            .claimantProvidedLegislativeCountry(extractLegislativeCountry(pcsCaseEntity))
            .claimantProvidedTenancyType(extractTenancyType(pcsCaseEntity))
            .claimantProvidedTenancyStartDate(extractTenancyStartDate(pcsCaseEntity))
            .claimantProvidedDailyRentAmount(extractDailyRentAmount(pcsCaseEntity))
            .claimantProvidedRentArrearsOwed(extractRentArrearsOwed(pcsCaseEntity))
            .claimantProvidedNoticeServed(extractNoticeServed(pcsCaseEntity))
            .claimantProvidedNoticeDate(extractNoticeDate(pcsCaseEntity))
            .build();
    }

    private AddressUK resolveAddress(PartyEntity defendantEntity, long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        if (defendantEntity.getAddressSameAsProperty() != null
            && defendantEntity.getAddressSameAsProperty() == VerticalYesNo.YES) {
            return addressMapper.toAddressUK(pcsCaseEntity.getPropertyAddress());
        } else {
            return addressMapper.toAddressUK(defendantEntity.getAddress());
        }
    }

    private Party buildParty(PartyEntity defendantEntity, AddressUK contactAddress) {
        return Party.builder()
            .firstName(defendantEntity.getFirstName())
            .lastName(defendantEntity.getLastName())
            .orgName(defendantEntity.getOrgName())
            .nameKnown(defendantEntity.getNameKnown())
            .emailAddress(defendantEntity.getEmailAddress())
            .address(contactAddress)
            .addressKnown(defendantEntity.getAddressKnown())
            .addressSameAsProperty(defendantEntity.getAddressSameAsProperty())
            .phoneNumber(defendantEntity.getPhoneNumber())
            .build();
    }

    private String extractLegislativeCountry(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getLegislativeCountry() != null
            ? pcsCaseEntity.getLegislativeCountry().name()
            : null;
    }

    private String extractTenancyType(PcsCaseEntity pcsCaseEntity) {
        TenancyLicence tenancy = pcsCaseEntity.getTenancyLicence();
        if (tenancy == null) {
            return null;
        }

        if (pcsCaseEntity.getLegislativeCountry() == LegislativeCountry.WALES) {
            return tenancy.getOccupationLicenceTypeWales() != null
                ? tenancy.getOccupationLicenceTypeWales().name()
                : null;
        }

        return tenancy.getTenancyLicenceType();
    }

    private String extractTenancyStartDate(PcsCaseEntity pcsCaseEntity) {
        TenancyLicence tenancy = pcsCaseEntity.getTenancyLicence();
        if (tenancy == null) {
            return null;
        }

        if (pcsCaseEntity.getLegislativeCountry() == LegislativeCountry.WALES) {
            return tenancy.getWalesLicenceStartDate() != null
                ? tenancy.getWalesLicenceStartDate().toString()
                : null;
        }

        return tenancy.getTenancyLicenceDate() != null
            ? tenancy.getTenancyLicenceDate().toString()
            : null;
    }

    private BigDecimal extractDailyRentAmount(PcsCaseEntity pcsCaseEntity) {
        TenancyLicence tenancy = pcsCaseEntity.getTenancyLicence();
        return tenancy != null ? tenancy.getDailyRentChargeAmount() : null;
    }

    private BigDecimal extractRentArrearsOwed(PcsCaseEntity pcsCaseEntity) {
        TenancyLicence tenancy = pcsCaseEntity.getTenancyLicence();
        return tenancy != null ? tenancy.getTotalRentArrears() : null;
    }

    private YesOrNo extractNoticeServed(PcsCaseEntity pcsCaseEntity) {
        TenancyLicence tenancy = pcsCaseEntity.getTenancyLicence();
        if (tenancy == null) {
            return null;
        }

        Boolean noticeServed;
        if (pcsCaseEntity.getLegislativeCountry() == LegislativeCountry.WALES) {
            noticeServed = tenancy.getWalesNoticeServed();
        } else {
            noticeServed = tenancy.getNoticeServed();
        }

        if (noticeServed == null) {
            return null;
        }

        return noticeServed ? YesOrNo.YES : YesOrNo.NO;
    }

    private LocalDateTime extractNoticeDate(PcsCaseEntity pcsCaseEntity) {
        TenancyLicence tenancy = pcsCaseEntity.getTenancyLicence();
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
