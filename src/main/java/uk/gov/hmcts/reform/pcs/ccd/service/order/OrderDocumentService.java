package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.ActorAttribution;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderStepEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.order.OrderSteps;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload.OrderParty;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState.ISSUED;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

/**
 * Generates an issued order's document from its wording and adds it to the case, where it shows
 * under "Orders and Notice of Hearings". It runs as a job once the issuing event has committed, and
 * adds the document in a system event on behalf of the caseworker who issued the order, so the
 * order's row is only written in case events. A re-run never adds a second document.
 */
@Service
@Slf4j
@AllArgsConstructor
public class OrderDocumentService {

    static final String TEMPLATE_ID = "CV-PCS-ORD-ENG-Order.docx";
    static final TaskDescriptor<Generation> ORDER_DOCUMENT =
        TaskDescriptor.of("order-document-generation", Generation.class);
    private static final int MAX_RETRIES = 5;
    private static final Duration RETRY_AFTER = Duration.ofMinutes(1);

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    private static final Map<String, String> TITLES = Map.of(
        "OUTRIGHT_POSSESSION", "Order for possession",
        "SUSPENDED_POSSESSION", "Order for possession (suspended)",
        "ADJOURNMENT", "Order (adjournment)",
        "STRIKE_OUT_DISMISSAL", "Order (strike out or dismissal)"
    );

    private final OrderRepository orderRepository;
    private final OrderSteps orderSteps;
    private final SystemEventExecutor systemEventExecutor;
    private final DocAssemblyService docAssemblyService;
    private final DocumentImportService documentImportService;
    private final LocationReferenceService locationReferenceService;
    private final PartyService partyService;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final TransactionTemplate transactionTemplate;

    /** The job's data: the issued order, and the caseworker who issued it, on whose behalf the document is added. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Generation {
        private UUID orderId;
        private String caseworkerId;
        private String caseworkerFirstName;
        private String caseworkerLastName;
    }

    /** The job that generates an order's document, for the issuing event to schedule; it runs once that commits. */
    public static SchedulableInstance<Generation> generation(OrderEntity order, UserInfo caseworker) {
        return ORDER_DOCUMENT
            .instance(order.getId().toString())
            .data(new Generation(order.getId(), caseworker.getUid(), caseworker.getGivenName(),
                caseworker.getFamilyName()))
            .scheduledTo(Instant.now());
    }

    @Bean
    public CustomTask<Generation> orderDocumentJob() {
        return Tasks.custom(ORDER_DOCUMENT)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(MAX_RETRIES,
                new FailureHandler.ExponentialBackoffFailureHandler<>(RETRY_AFTER)))
            .execute((instance, context) -> {
                generateAndAttach(instance.getData());
                return new CompletionHandler.OnCompleteRemove<>();
            });
    }

    public void generateAndAttach(Generation generation) {
        Optional<Rendering> rendering = transactionTemplate.execute(status -> rendering(generation.orderId));
        if (rendering.isEmpty()) {
            return;
        }
        OrderDocumentPayload payload = rendering.get().payload();
        String dmStoreUrl = docAssemblyService.generateDocument(
            payload, TEMPLATE_ID, OutputType.PDF, payload.getOrderTitle());
        try {
            systemEventExecutor.execute(rendering.get().caseReference(), caseworker(generation),
                generation.orderId, context -> attach(rendering.get(), generation.orderId, dmStoreUrl));
        } catch (Exception e) {
            deleteOrphanedDocument(generation.orderId, dmStoreUrl);
            throw e;
        }
    }

    private record Rendering(long caseReference, LocalDate issuedOn, OrderDocumentPayload payload) {
    }

    /** What to render, or nothing when the order already has its document. */
    private Optional<Rendering> rendering(UUID orderId) {
        OrderEntity order = issuedOrder(orderId);
        if (order.getDocument() != null) {
            return Optional.empty();
        }
        // The order is issued, so its latest step is the issuing one.
        OrderStepEntity issued = orderSteps.latest(order);
        LocalDate issuedOn = issued.getCreatedAt().atZone(UK_ZONE_ID).toLocalDate();
        return Optional.of(new Rendering(order.getPcsCase().getCaseReference(), issuedOn,
            payload(order, issued, issuedOn)));
    }

    private OrderEntity issuedOrder(UUID orderId) {
        return orderRepository.findById(orderId)
            .filter(order -> order.getState() == ISSUED)
            .orElseThrow(() -> new IllegalStateException("No issued order " + orderId));
    }

    /** Adds the document to the case; the system event's idempotency key means this runs once per order. */
    private SystemEventResult attach(Rendering rendering, UUID orderId, String dmStoreUrl) {
        OrderEntity order = issuedOrder(orderId);
        DocumentEntity document = documentImportService.addDocumentToCase(
            order.getPcsCase(), dmStoreUrl, CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS);
        document.setType(DocumentType.ORDER);
        document.setIssueDate(rendering.issuedOn());
        order.setDocument(document);
        return SystemEventResult.withoutStateTransition("orderDocumentGenerated", "Order document generated");
    }

    private static ActorAttribution caseworker(Generation generation) {
        return new ActorAttribution(generation.caseworkerId, generation.caseworkerFirstName,
            generation.caseworkerLastName);
    }

    private OrderDocumentPayload payload(OrderEntity order, OrderStepEntity issued, LocalDate issuedOn) {
        PcsCaseEntity pcsCase = order.getPcsCase();
        Optional<ClaimEntity> claim = pcsCase.getClaims().stream().findFirst();
        Optional<CourtVenue> court = court(pcsCase);
        return OrderDocumentPayload.builder()
            .caseNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()))
            .courtName(court.map(CourtVenue::courtName).orElse(null))
            .courtAddress(court.map(venue -> Stream.of(venue.courtAddress(), venue.postcode())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(", "))).orElse(null))
            .orderTitle(TITLES.getOrDefault(issued.getOrderType(), "Order"))
            .parties(claim.map(c -> Stream.concat(
                parties(c, PartyRole.CLAIMANT, "Claimant"), parties(c, PartyRole.DEFENDANT, "Defendant")).toList())
                .orElse(List.of()))
            .propertyAddress(addressFormatter.formatFullAddressWithoutCountry(
                addressMapper.toAddressUK(pcsCase.getPropertyAddress()), AddressFormatter.COMMA_DELIMITER))
            .judgeName(Optional.ofNullable(order.getAuthorName()).orElse("the Judge"))
            .orderDate(ORDER_DATE.format(issuedOn))
            .orderHtml(issued.getOrderHtml())
            .sealName(order.getSeal() == OrderSeal.HIGH_COURT ? "High Court" : countyCourt(court))
            .countyCourtSeal(order.getSeal() == OrderSeal.COUNTY_COURT)
            .highCourtSeal(order.getSeal() == OrderSeal.HIGH_COURT)
            .build();
    }

    /** The county court by name, as "County Court at Luton" when its venue name does not say so. */
    private static String countyCourt(Optional<CourtVenue> court) {
        return court.map(CourtVenue::courtName)
            .map(name -> name.contains("County Court") ? name : "County Court at " + name)
            .orElse("County Court");
    }

    /** The claim's parties in a role, numbered when there is more than one: "Defendant 1", "Defendant 2". */
    private Stream<OrderParty> parties(ClaimEntity claim, PartyRole role, String label) {
        List<String> names = claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .map(claimParty -> partyService.getPartyName(claimParty.getParty()))
            .toList();
        return IntStream.range(0, names.size())
            .mapToObj(i -> new OrderParty(names.get(i), names.size() == 1 ? label : label + " " + (i + 1)));
    }

    /** The court managing the case, from location reference data; the order names none without it. */
    private Optional<CourtVenue> court(PcsCaseEntity pcsCase) {
        if (pcsCase.getCaseManagementLocation() == null) {
            return Optional.empty();
        }
        try {
            return locationReferenceService.getCourtVenues(List.of(pcsCase.getCaseManagementLocation()))
                .stream().findFirst();
        } catch (Exception e) {
            log.warn("No court venue for case {}", pcsCase.getCaseReference(), e);
            return Optional.empty();
        }
    }

    private void deleteOrphanedDocument(UUID orderId, String dmStoreUrl) {
        try {
            documentImportService.deleteDocument(dmStoreUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned order document for order {}: {}", orderId, dmStoreUrl, e);
        }
    }
}
