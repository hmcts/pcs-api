package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.COUNTER_CLAIM_ISSUED;

/**
 * The case as the order journeys show it: its property, its main claim's parties, its open applications, and
 * the application an order on one decides.
 */
@Component
@AllArgsConstructor
class OrderCaseContext {

    private final AddressMapper addressMapper;
    private final PartyService partyService;
    private final OrderTasks orderTasks;

    /** The case for the order, which offers the applications no other order is deciding or has decided. */
    public OrderStart.CaseContext of(PcsCaseEntity pcsCase, UUID orderId, UUID genAppId) {
        Optional<ClaimEntity> claim = pcsCase.getClaims().stream().findFirst();
        return new OrderStart.CaseContext(
            pcsCase.getCaseReference(),
            addressMapper.toAddressUK(pcsCase.getPropertyAddress()),
            claim.map(c -> parties(c, PartyRole.CLAIMANT)).orElse(List.of()),
            claim.map(c -> parties(c, PartyRole.DEFENDANT)).orElse(List.of()),
            pcsCase.getCounterClaims().stream()
                .map(CounterClaimEntity::getStatus).anyMatch(COUNTER_CLAIM_ISSUED::equals),
            pcsCase.getGenApps().stream().anyMatch(GenAppEntity::isOpen),
            pcsCase.getGenApps().stream()
                .filter(genApp -> orderTasks.canDecide(genApp, orderId))
                .sorted(Comparator.comparing(GenAppEntity::getRank, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(genApp -> application(genApp, claim.orElse(null)))
                .toList(),
            genAppId == null ? null : application(pcsCase.findGenApp(genAppId).orElseThrow(), claim.orElse(null))
        );
    }

    private OrderStart.Application application(GenAppEntity genApp, ClaimEntity claim) {
        return new OrderStart.Application(
            genApp.getId(),
            isWarrantSuspension(genApp),
            "GA" + genApp.getRank(),
            genApp.getType().getShortName(),
            genApp.getSomethingElseDetails(),
            applicant(genApp, claim),
            // A paper application court staff entered is dated when the court received it, not when they entered it.
            genApp.getApplicationReceivedDate() != null || genApp.getApplicationSubmittedDate() == null
                ? genApp.getApplicationReceivedDate() : genApp.getApplicationSubmittedDate().toLocalDate(),
            genApp.getWithin14Days(),
            genApp.getOtherPartiesAgreed(),
            genApp.getWithoutNotice(),
            genApp.getWithoutNoticeReason(),
            genApp.getWhatOrderWanted(),
            applicationDocuments(genApp)
        );
    }

    /**
     * Whether the application asks to suspend the warrant of possession. The application journeys have no such
     * type yet, so an applicant asks for it as something else: this reads what they said it is for. Any other
     * application about the warrant is decided in the judge's own words, not as a suspension.
     */
    private static boolean isWarrantSuspension(GenAppEntity genApp) {
        return genApp.getType() == GenAppType.SOMETHING_ELSE
            && StringUtils.containsIgnoreCase(genApp.getSomethingElseDetails(), "warrant")
            && StringUtils.containsIgnoreCase(genApp.getSomethingElseDetails(), "suspen");
    }

    /** The applicant by name, with their role on the claim: "Jane Doe (Defendant 1)". */
    private String applicant(GenAppEntity genApp, ClaimEntity claim) {
        if (genApp.getParty() == null) {
            return null;
        }
        String name = partyService.getPartyName(genApp.getParty());
        return Optional.ofNullable(claim).stream()
            .flatMap(c -> c.getClaimParties().stream())
            .filter(claimParty -> claimParty.getParty().getId().equals(genApp.getParty().getId()))
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
}
