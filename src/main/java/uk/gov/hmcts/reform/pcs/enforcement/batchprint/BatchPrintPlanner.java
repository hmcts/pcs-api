package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffAllocationRepository;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffEnforcementRules;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementDocumentService;
import uk.gov.hmcts.reform.pcs.idam.IdamUserIds;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Decides what a bailiff's batch print contains (HDPI-8012), without fetching any document.
 *
 * <p>A case is printed only when the bailiff is the active allocated bailiff on its current warrant, and that
 * warrant is approved and still live. For each case the latest warrant, risk assessment and notice of eviction
 * in the Enforcement folder are printed, in that order. Documents filed against an earlier enforcement on the
 * same claim are ignored, so a repeat warrant never prints the previous warrant's papers.
 */
@Service
@RequiredArgsConstructor
public class BatchPrintPlanner {

    private static final Pattern CASE_REFERENCE = Pattern.compile("\\d{16}");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s-]");

    private static final Comparator<DocumentEntity> OLDEST_FIRST =
        Comparator.comparing(DocumentEntity::getSubmittedDate, Comparator.nullsFirst(Comparator.naturalOrder()));

    private final PcsCaseRepository pcsCaseRepository;
    private final CurrentEnforcementService currentEnforcementService;
    private final BailiffAllocationService bailiffAllocationService;
    private final BailiffAllocationRepository bailiffAllocationRepository;

    /**
     * Plans a batch print of {@code caseReferences} for the bailiff, keeping the order they were chosen in.
     * A reference given twice is printed once. References may contain dashes or spaces.
     */
    @Transactional(readOnly = true)
    public BatchPrintPlan plan(String bailiffUserId, List<String> caseReferences) {
        String bailiff = IdamUserIds.normalise(bailiffUserId);
        List<PrintableCase> included = new ArrayList<>();
        List<ExcludedCase> excluded = new ArrayList<>();

        for (String reference : distinct(caseReferences)) {
            if (!CASE_REFERENCE.matcher(reference).matches()) {
                excluded.add(ExcludedCase.of(reference, BatchPrintExclusion.INVALID_REFERENCE));
                continue;
            }
            CaseOutcome outcome = planCase(bailiff, reference);
            if (outcome.printable() != null) {
                included.add(outcome.printable());
            } else {
                excluded.add(ExcludedCase.of(reference, outcome.exclusion()));
            }
        }
        return new BatchPrintPlan(List.copyOf(included), List.copyOf(excluded));
    }

    /**
     * The approved, live warrants allocated to the bailiff, soonest eviction first. With a date range, only
     * warrants whose eviction is scheduled in it (inclusive) are listed.
     */
    @Transactional(readOnly = true)
    public List<AllocatedWarrant> allocatedWarrants(String bailiffUserId, LocalDate from, LocalDate to) {
        return bailiffAllocationRepository.findAllocatedWarrants(IdamUserIds.normalise(bailiffUserId),
                                                                 BailiffEnforcementRules.APPROVED_LIVE_STATUSES)
            .stream()
            .filter(warrant -> evictionInRange(warrant.bailiffDate(), from, to))
            .map(warrant -> new AllocatedWarrant(String.valueOf(warrant.caseReference()),
                                                 warrant.enforcementNumber(),
                                                 warrant.status(),
                                                 warrant.bailiffDate()))
            .sorted(Comparator.comparing(AllocatedWarrant::evictionDate,
                                         Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AllocatedWarrant::caseReference))
            .toList();
    }

    private CaseOutcome planCase(String bailiff, String reference) {
        Optional<PcsCaseEntity> pcsCase = pcsCaseRepository.findByCaseReference(Long.parseLong(reference));
        if (pcsCase.isEmpty()) {
            return CaseOutcome.excluded(BatchPrintExclusion.NOT_ALLOCATED);
        }
        List<EnforcementOrderEntity> enforcements = currentEnforcementService.findEnforcementsCurrentFirst(
            pcsCase.get());
        if (enforcements.isEmpty()) {
            return CaseOutcome.excluded(BatchPrintExclusion.NOT_ALLOCATED);
        }
        EnforcementOrderEntity current = enforcements.getFirst();
        boolean allocatedToBailiff = bailiffAllocationService.findActiveAllocation(current)
            .map(allocation -> bailiff.equals(IdamUserIds.normalise(allocation.getBailiffUserId())))
            .orElse(false);
        if (!allocatedToBailiff) {
            return CaseOutcome.excluded(BatchPrintExclusion.NOT_ALLOCATED);
        }
        if (!BailiffEnforcementRules.isApprovedLiveWarrant(current)) {
            return CaseOutcome.excluded(BatchPrintExclusion.NO_LIVE_WARRANT);
        }

        Map<BatchPrintDocumentKind, DocumentEntity> latest =
            latestDocuments(pcsCase.get(), otherEnforcementsDocuments(enforcements));
        if (latest.isEmpty()) {
            return CaseOutcome.excluded(BatchPrintExclusion.NO_DOCUMENTS);
        }

        List<PrintableDocument> documents = new ArrayList<>();
        List<BatchPrintDocumentKind> missing = new ArrayList<>();
        for (BatchPrintDocumentKind kind : BatchPrintDocumentKind.values()) {
            DocumentEntity document = latest.get(kind);
            if (document == null) {
                missing.add(kind);
            } else {
                documents.add(new PrintableDocument(kind, document.getFileName(), document.getSubmittedDate(),
                                                    document.getDocumentId()));
            }
        }
        return CaseOutcome.printed(
            new PrintableCase(reference, current.getEnforcementNumber(), List.copyOf(documents), List.copyOf(missing)));
    }

    private Map<BatchPrintDocumentKind, DocumentEntity> latestDocuments(PcsCaseEntity pcsCase, Set<UUID> ignored) {
        Map<BatchPrintDocumentKind, DocumentEntity> latest = new EnumMap<>(BatchPrintDocumentKind.class);
        pcsCase.getDocuments().stream()
            .filter(document -> EnforcementDocumentService.ENFORCEMENT_CATEGORY_ID.equals(document.getCategoryId()))
            .filter(document -> document.getDocumentId() != null)
            .filter(document -> !ignored.contains(document.getId()))
            .sorted(OLDEST_FIRST)
            .forEach(document -> BatchPrintDocumentKind.of(document.getType())
                .ifPresent(kind -> latest.put(kind, document)));
        return latest;
    }

    private static Set<UUID> otherEnforcementsDocuments(List<EnforcementOrderEntity> enforcementsCurrentFirst) {
        return enforcementsCurrentFirst.stream()
            .skip(1)
            .flatMap(enforcement -> enforcement.getDocuments().stream())
            .map(DocumentEntity::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private static boolean evictionInRange(LocalDateTime evictionDate, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (evictionDate == null) {
            return false;
        }
        LocalDate day = evictionDate.toLocalDate();
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }

    private static Set<String> distinct(List<String> caseReferences) {
        return caseReferences.stream()
            .filter(Objects::nonNull)
            .map(reference -> SEPARATORS.matcher(reference).replaceAll(""))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private record CaseOutcome(PrintableCase printable, BatchPrintExclusion exclusion) {

        static CaseOutcome printed(PrintableCase printable) {
            return new CaseOutcome(printable, null);
        }

        static CaseOutcome excluded(BatchPrintExclusion exclusion) {
            return new CaseOutcome(null, exclusion);
        }
    }
}
