package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.SubmittedDraftOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.order.CaseCourts;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseBundlesView;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.toDomainParties;

/**
 * The case as the order journeys show it: its property, its main claim's parties, its open applications, the
 * hearings an order can be made at and the court managing it.
 */
@Component
@AllArgsConstructor
class OrderCaseContext {

    private final AddressMapper addressMapper;
    private final PartyService partyService;
    private final CaseNameFormatter caseNameFormatter;
    private final OrderTasks orderTasks;
    private final CaseBundleRepository bundles;
    private final CaseBundlesView bundleView;
    private final CaseCourts caseCourts;
    private final SubmittedDraftOrderRepository submittedDraftOrders;

    /** The case for the order, which offers the applications no issued order has decided. */
    public OrderStart.CaseContext of(PcsCaseEntity pcsCase) {
        Optional<ClaimEntity> claim = pcsCase.getClaims().stream().findFirst();
        return new OrderStart.CaseContext(
            pcsCase.getCaseReference(),
            addressMapper.toAddressUK(pcsCase.getPropertyAddress()),
            claim.map(this::buildCaseName).orElse(null),
            claim.map(c -> parties(c, PartyRole.CLAIMANT)).orElse(List.of()),
            claim.map(c -> parties(c, PartyRole.DEFENDANT)).orElse(List.of()),
            pcsCase.getCounterClaims().stream()
                .map(CounterClaimEntity::getStatus).anyMatch(COUNTER_CLAIM_ISSUED::equals),
            pcsCase.getGenApps().stream()
                .filter(orderTasks::canDecide)
                .sorted(Comparator.comparing(GenAppEntity::getRank, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(genApp -> application(genApp, claim.orElse(null)))
                .toList(),
            submittedDraftOrders.findByPartyPcsCaseIdOrderBySubmittedAtDesc(pcsCase.getId()).stream()
                .map(draft -> new OrderStart.SubmittedDraft(draft.getId(),
                    partyName(draft.getParty(), claim.orElse(null)), draft.getSubmittedAt(), draft.getWording()))
                .toList(),
            caseBundle(pcsCase),
            pcsCase.getHearings().stream()
                .filter(HearingRecords::canHaveOrder)
                .sorted(Comparator.comparing(HearingEntity::getHearingDate))
                .map(hearing -> new OrderStart.HearingOption(hearing.getId(), hearing.getHearingDate()))
                .toList(),
            court(pcsCase)
        );
    }

    /**
     * The court managing the case, as an order names where the judge sits: its name and its venue's short name, or a
     * placeholder for the judge to fill in.
     */
    private String court(PcsCaseEntity pcsCase) {
        return caseCourts.of(pcsCase)
            .map(venue -> Stream.of(venue.courtName(),
                    Optional.ofNullable(venue.externalShortName()).orElse(venue.venueName()))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" ")))
            .filter(StringUtils::isNotBlank)
            .orElse("[court]");
    }

    /** The case's current bundle, for those given the case's bundles. */
    private OrderStart.CaseBundle caseBundle(PcsCaseEntity pcsCase) {
        if (!bundleView.shownInMakeOrder()) {
            return null;
        }
        return bundles.findFirstByCaseIdOrderByCaseRevisionDesc(pcsCase.getId())
            .map(bundle -> new OrderStart.CaseBundle(bundle.getDocumentId(), bundle.getFileName(),
                                                     bundle.getCreatedAt()))
            .orElse(null);
    }

    private OrderStart.Application application(GenAppEntity genApp, ClaimEntity claim) {
        return new OrderStart.Application(
            genApp.getId(),
            genApp.getType() == GenAppType.SUSPEND,
            genApp.getType() == GenAppType.ADJOURN,
            orderTasks.isListed(genApp),
            "GA" + genApp.getRank(),
            genApp.getType().getShortName(),
            genApp.getSomethingElseDetails(),
            partyName(genApp.getParty(), claim),
            // A paper application court staff entered is dated when the court received it, not when they entered it.
            genApp.getApplicationReceivedDate() != null
                ? genApp.getApplicationReceivedDate() : genApp.getApplicationSubmittedDate().toLocalDate(),
            genApp.getWithin14Days(),
            genApp.getOtherPartiesAgreed(),
            genApp.getWithoutNotice(),
            genApp.getWithoutNoticeReason(),
            genApp.getWhatOrderWanted(),
            genApp.getDraftOrderWording(),
            applicationDocuments(genApp)
        );
    }

    /** The party by name, with their role on the claim: "Jane Doe (Defendant 1)". */
    private String partyName(PartyEntity party, ClaimEntity claim) {
        String name = partyService.getPartyName(party);
        return Optional.ofNullable(claim).stream()
            .flatMap(c -> c.getClaimParties().stream())
            .filter(claimParty -> claimParty.getParty().getId().equals(party.getId()))
            .findFirst()
            .map(claimParty -> "%s (%s %d)".formatted(name,
                StringUtils.capitalize(claimParty.getRole().name().toLowerCase()), claimParty.getRank()))
            .orElse(name);
    }

    /** The application form, then the documents filed with it; the form is also one of them, so each is once. */
    private static List<OrderStart.ApplicationDocument> applicationDocuments(GenAppEntity genApp) {
        Map<UUID, OrderStart.ApplicationDocument> documents = new LinkedHashMap<>();
        Stream.concat(Stream.ofNullable(genApp.getSubmissionDocument()), genApp.getDocuments().stream())
            .filter(document -> document.getDocumentId() != null)
            .forEach(document -> documents.putIfAbsent(document.getDocumentId(),
                new OrderStart.ApplicationDocument(document.getDocumentId(), document.getFileName())));
        return List.copyOf(documents.values());
    }

    private List<OrderStart.Party> parties(ClaimEntity claim, PartyRole role) {
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .map(claimParty -> new OrderStart.Party(
                claimParty.getId().getPartyId().toString(), partyService.getPartyName(claimParty.getParty())))
            .toList();
    }

    private String buildCaseName(ClaimEntity mainClaim) {
        List<PartyEntity> claimants = PartyDisplayMapper.partiesByRole(mainClaim, PartyRole.CLAIMANT);
        List<PartyEntity> defendants = PartyDisplayMapper.partiesByRole(mainClaim, PartyRole.DEFENDANT);
        return caseNameFormatter.formatCaseName(toDomainParties(claimants), toDomainParties(defendants));
    }

}
