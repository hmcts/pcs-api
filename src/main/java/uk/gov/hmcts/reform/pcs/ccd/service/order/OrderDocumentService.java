package uk.gov.hmcts.reform.pcs.ccd.service.order;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderSeal;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.IssuedOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.IssuedOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload;
import uk.gov.hmcts.reform.pcs.document.model.order.OrderDocumentPayload.OrderParty;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generates an issued order's document from its wording and adds it to the case, where it shows
 * under "Orders and Notice of Hearings". Like {@link uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimFormService},
 * the Docmosis render runs between a read-only transaction that builds the payload and a short write
 * transaction that attaches the document, and a re-run never attaches a second document.
 */
@Service
@Slf4j
public class OrderDocumentService {

    static final String TEMPLATE_ID = "CV-PCS-ORD-ENG-Order.docx";

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    private static final Map<String, String> TITLES = Map.of(
        "OUTRIGHT_POSSESSION", "Order for possession",
        "SUSPENDED_POSSESSION", "Order for possession (suspended)",
        "ADJOURNMENT", "Order (adjournment)",
        "STRIKE_OUT_DISMISSAL", "Order (strike out or dismissal)"
    );

    private final IssuedOrderRepository issuedOrderRepository;
    private final DocAssemblyService docAssemblyService;
    private final DocumentImportService documentImportService;
    private final LocationReferenceService locationReferenceService;
    private final PartyService partyService;
    private final AddressMapper addressMapper;
    private final AddressFormatter addressFormatter;
    private final CaseReferenceFormatter caseReferenceFormatter;
    private final EntityManager entityManager;
    private final TransactionTemplate readOnly;
    private final TransactionTemplate readWrite;

    public OrderDocumentService(IssuedOrderRepository issuedOrderRepository,
                                DocAssemblyService docAssemblyService,
                                DocumentImportService documentImportService,
                                LocationReferenceService locationReferenceService,
                                PartyService partyService,
                                AddressMapper addressMapper,
                                AddressFormatter addressFormatter,
                                CaseReferenceFormatter caseReferenceFormatter,
                                EntityManager entityManager,
                                PlatformTransactionManager transactionManager) {
        this.issuedOrderRepository = issuedOrderRepository;
        this.docAssemblyService = docAssemblyService;
        this.documentImportService = documentImportService;
        this.locationReferenceService = locationReferenceService;
        this.partyService = partyService;
        this.addressMapper = addressMapper;
        this.addressFormatter = addressFormatter;
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.entityManager = entityManager;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.readWrite = new TransactionTemplate(transactionManager);
    }

    public void generateAndAttach(UUID issuedOrderId) {
        OrderDocumentPayload payload = readOnly.execute(status -> {
            IssuedOrderEntity issued = issuedOrder(issuedOrderId);
            return issued.getDocument() == null ? payload(issued) : null;
        });
        if (payload == null) {
            return;
        }
        String dmStoreUrl = docAssemblyService.generateDocument(
            payload, TEMPLATE_ID, OutputType.PDF, payload.getOrderTitle());
        try {
            readWrite.executeWithoutResult(status -> attach(issuedOrderId, dmStoreUrl));
        } catch (Exception e) {
            deleteOrphanedDocument(issuedOrderId, dmStoreUrl);
            throw e;
        }
    }

    /** The issued order, which the task can look for before the issuing event commits; it then retries. */
    private IssuedOrderEntity issuedOrder(UUID issuedOrderId) {
        return issuedOrderRepository.findById(issuedOrderId)
            .orElseThrow(() -> new IllegalStateException("No issued order " + issuedOrderId));
    }

    private void attach(UUID issuedOrderId, String dmStoreUrl) {
        IssuedOrderEntity issued = issuedOrder(issuedOrderId);
        if (issued.getDocument() != null) {
            deleteOrphanedDocument(issuedOrderId, dmStoreUrl);
            return;
        }
        DocumentEntity document = documentImportService.addDocumentToCase(
            issued.getPcsCase(), dmStoreUrl, CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS);
        document.setType(DocumentType.ORDER);
        document.setIssueDate(issued.getIssuedOn());
        entityManager.persist(document);
        issued.setDocument(document);
    }

    private OrderDocumentPayload payload(IssuedOrderEntity issued) {
        PcsCaseEntity pcsCase = issued.getPcsCase();
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
            .judgeName(judgeName(pcsCase.getCaseReference(), issued.getDraftOrder().getAuthorIdamUserId())
                .orElse("the Judge"))
            .orderDate(ORDER_DATE.format(issued.getIssuedOn()))
            .orderHtml(issued.getOrderHtml())
            .sealName(issued.getSeal() == OrderSeal.HIGH_COURT ? "High Court" : countyCourt(court))
            .countyCourtSeal(issued.getSeal() == OrderSeal.COUNTY_COURT)
            .highCourtSeal(issued.getSeal() == OrderSeal.HIGH_COURT)
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

    /**
     * The judge who made the order, as CCD recorded them when they last changed it: draft orders keep
     * only the judge's IDAM id.
     */
    private Optional<String> judgeName(long caseReference, UUID judge) {
        List<?> names = entityManager.createNativeQuery("""
                SELECT concat_ws(' ', e.user_first_name, e.user_last_name) FROM ccd.case_event e
                JOIN ccd.case_data c ON c.id = e.case_data_id
                WHERE c.reference = :caseReference AND e.user_id = :judge AND e.event_id = 'ext:makeOrder'
                ORDER BY e.id DESC LIMIT 1""")
            .setParameter("caseReference", caseReference)
            .setParameter("judge", judge.toString())
            .getResultList();
        return names.stream().filter(Objects::nonNull).map(name -> name.toString().trim())
            .filter(name -> !name.isEmpty()).findFirst();
    }

    private void deleteOrphanedDocument(UUID issuedOrderId, String dmStoreUrl) {
        try {
            documentImportService.deleteDocument(dmStoreUrl);
        } catch (Exception e) {
            log.error("Failed to delete orphaned order document for issued order {}: {}",
                issuedOrderId, dmStoreUrl, e);
        }
    }
}
