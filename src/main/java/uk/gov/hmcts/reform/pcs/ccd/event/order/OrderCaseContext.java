package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart.CaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.COUNTER_CLAIM_ISSUED;

/**
 * The case an order is made on, as the frontend shows it to the judge making it and the caseworker
 * reviewing it: its property and the parties and facts of its main claim, and the application an
 * order on one decides.
 */
@Component
@AllArgsConstructor
class OrderCaseContext {

    private final AddressMapper addressMapper;
    private final PartyService partyService;

    public OrderStart.CaseContext of(PcsCaseEntity pcsCase) {
        return of(pcsCase, null);
    }

    /** The case, with the application the order decides when it decides one. */
    public OrderStart.CaseContext of(PcsCaseEntity pcsCase, UUID genAppId) {
        Optional<ClaimEntity> claim = pcsCase.getClaims().stream().findFirst();
        return new OrderStart.CaseContext(
            pcsCase.getCaseReference(),
            addressMapper.toAddressUK(pcsCase.getPropertyAddress()),
            claim.map(c -> parties(c, PartyRole.CLAIMANT)).orElse(List.of()),
            claim.map(c -> parties(c, PartyRole.DEFENDANT)).orElse(List.of()),
            caseFacts(pcsCase.getTenancyLicence(), claim.orElse(null)),
            pcsCase.getCounterClaims().stream()
                .map(CounterClaimEntity::getStatus).anyMatch(COUNTER_CLAIM_ISSUED::equals),
            pcsCase.getGenApps().stream().anyMatch(GenAppEntity::isOpen),
            genAppId == null ? null : pcsCase.findGenApp(genAppId)
                .map(genApp -> application(genApp, claim.orElse(null)))
                .orElseThrow(() -> new IllegalStateException("No application " + genAppId + " on the case"))
        );
    }

    private OrderStart.Application application(GenAppEntity genApp, ClaimEntity claim) {
        return new OrderStart.Application(
            genApp.getId(),
            "GA" + genApp.getRank(),
            genApp.getType().getShortName(),
            genApp.getSomethingElseDetails(),
            applicant(genApp, claim),
            genApp.getApplicationSubmittedDate() != null
                ? genApp.getApplicationSubmittedDate().toLocalDate() : genApp.getApplicationReceivedDate(),
            yesNo(genApp.getWithin14Days()),
            yesNo(genApp.getOtherPartiesAgreed()),
            yesNo(genApp.getWithoutNotice()),
            genApp.getWithoutNoticeReason(),
            genApp.getWhatOrderWanted(),
            applicationDocuments(genApp),
            genApp.getReferredOn(),
            genApp.getReferralNote()
        );
    }

    /** The applicant by name, with the role they have on the claim: "Jane Doe (Defendant 1)". */
    private String applicant(GenAppEntity genApp, ClaimEntity claim) {
        if (genApp.getParty() == null) {
            return null;
        }
        String name = partyService.getPartyName(genApp.getParty());
        return Optional.ofNullable(claim).stream()
            .flatMap(c -> c.getClaimParties().stream())
            .filter(claimParty -> claimParty.getParty().getId().equals(genApp.getParty().getId()))
            .findFirst()
            .map(claimParty -> "%s (%s %d)".formatted(name, StringUtils.capitalize(
                claimParty.getRole().name().toLowerCase()), claimParty.getRank()))
            .orElse(name);
    }

    /**
     * The application form, then the documents filed with it, which leave out removed ones. The form is
     * also one of the application's documents, so each document is listed once.
     */
    private static List<OrderStart.ApplicationDocument> applicationDocuments(GenAppEntity genApp) {
        Map<String, OrderStart.ApplicationDocument> documents = new LinkedHashMap<>();
        Stream.concat(Stream.ofNullable(genApp.getSubmissionDocument()), genApp.getDocuments().stream())
            .map(document -> new OrderStart.ApplicationDocument(documentId(document.getUrl()),
                document.getFileName()))
            .filter(document -> document.id() != null)
            .forEach(document -> documents.putIfAbsent(document.id(), document));
        return List.copyOf(documents.values());
    }

    /** The document store's id for a document, the last part of its URL. */
    private static String documentId(String url) {
        if (url == null || !url.contains("/documents/")) {
            return null;
        }
        String id = url.substring(url.lastIndexOf("/documents/") + "/documents/".length());
        return id.contains("/") ? id.substring(0, id.indexOf('/')) : id;
    }

    private static String yesNo(VerticalYesNo answer) {
        return answer == null ? null : answer.name();
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

    private List<OrderStart.Party> parties(ClaimEntity claim, PartyRole role) {
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .map(claimParty -> new OrderStart.Party(
                claimParty.getId().getPartyId().toString(), partyService.getPartyName(claimParty.getParty())))
            .toList();
    }
}
