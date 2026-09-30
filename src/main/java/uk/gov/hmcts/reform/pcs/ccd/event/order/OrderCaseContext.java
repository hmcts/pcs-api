package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderStart.CaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The case an order is made on, as the frontend shows it to the judge making it and the caseworker
 * reviewing it: its property and the parties and facts of its main claim.
 */
@Component
@AllArgsConstructor
class OrderCaseContext {

    private final AddressMapper addressMapper;
    private final PartyService partyService;

    public MakeOrderStart.CaseContext of(PcsCaseEntity pcsCase) {
        Optional<ClaimEntity> claim = pcsCase.getClaims().stream().findFirst();
        return new MakeOrderStart.CaseContext(
            pcsCase.getCaseReference(),
            addressMapper.toAddressUK(pcsCase.getPropertyAddress()),
            claim.map(c -> parties(c, PartyRole.CLAIMANT)).orElse(List.of()),
            claim.map(c -> parties(c, PartyRole.DEFENDANT)).orElse(List.of()),
            caseFacts(pcsCase.getTenancyLicence(), claim.orElse(null))
        );
    }

    private static CaseFacts caseFacts(TenancyLicenceEntity tenancy, ClaimEntity claim) {
        NoticeOfPossessionEntity notice = claim == null ? null : claim.getNoticeOfPossession();
        return new CaseFacts(
            tenancy == null ? null : tenancy.getStartDate(),
            tenancy == null || tenancy.getType() == null ? null : tenancy.getType().name(),
            noticeDate(notice),
            tenancy == null ? null : tenancy.getRentAmount(),
            tenancy == null || tenancy.getRentFrequency() == null ? null : tenancy.getRentFrequency().name(),
            claim == null ? null : groundsPleaded(claim)
        );
    }

    /** The day the notice was served, whether the claimant recorded a date or a moment. */
    private static LocalDate noticeDate(NoticeOfPossessionEntity notice) {
        if (notice == null) {
            return null;
        }
        if (notice.getNoticeDate() != null) {
            return notice.getNoticeDate();
        }
        return notice.getNoticeDateTime() == null ? null : notice.getNoticeDateTime().toLocalDate();
    }

    private static String groundsPleaded(ClaimEntity claim) {
        String grounds = claim.getClaimGrounds().stream()
            .map(ground -> ClaimGroundSummary.resolveGround(ground.getCategory(), ground.getCode()))
            .filter(Objects::nonNull)
            .map(PossessionGroundEnum::getLabel)
            .distinct()
            .sorted()
            .collect(Collectors.joining(", "));
        return grounds.isEmpty() ? null : grounds;
    }

    private List<MakeOrderStart.Party> parties(ClaimEntity claim, PartyRole role) {
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .map(claimParty -> new MakeOrderStart.Party(
                claimParty.getId().getPartyId().toString(), partyService.getPartyName(claimParty.getParty())))
            .toList();
    }
}
