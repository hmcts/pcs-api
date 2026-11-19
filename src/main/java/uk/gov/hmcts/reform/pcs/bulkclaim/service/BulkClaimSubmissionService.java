package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimKind;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionEntity;
import uk.gov.hmcts.reform.pcs.bulkclaim.entity.BulkClaimSubmissionStatus;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Prepared;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Rejected;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.PreparedBulkClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.repository.BulkClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimResponseBuilder;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimResponseType;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Processes the claims of one bulk request: idempotency, preparation, staging and case creation in CCD.
 * A claim's staging write is committed in its own transaction before CCD is called, so no database
 * transaction is ever held open across the CCD call.
 */
@Slf4j
@Service
public class BulkClaimSubmissionService {

    private static final String DUPLICATE_IN_REQUEST = "DUPLICATE_IDENTIFIER";
    private static final String CUSTOMER_UNIQUE_IDENTIFIER_PATH = "CustomerUniqueIdentifier";

    /**
     * One claim from a request, with the work to prepare it deferred so it runs on the claim's own thread.
     */
    public record ClaimInput(String customerUniqueIdentifier,
                             String claimHash,
                             BulkClaimKind claimKind,
                             String schemaVersion,
                             Supplier<BulkClaimPreparationResult> preparation) {
    }

    private final BulkClaimSubmissionRepository repository;
    private final BulkClaimCcdClient ccdClient;
    private final BulkClaimStagingSerializer serializer;
    private final BulkClaimResponseBuilder responseBuilder;
    private final BulkClaimPaymentRetryService paymentRetryService;
    private final BulkClaimProperties properties;
    private final TransactionTemplate newTransaction;
    private final ExecutorService executor;

    public BulkClaimSubmissionService(BulkClaimSubmissionRepository repository,
                                      BulkClaimCcdClient ccdClient,
                                      BulkClaimStagingSerializer serializer,
                                      BulkClaimResponseBuilder responseBuilder,
                                      BulkClaimPaymentRetryService paymentRetryService,
                                      BulkClaimProperties properties,
                                      PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.ccdClient = ccdClient;
        this.serializer = serializer;
        this.responseBuilder = responseBuilder;
        this.paymentRetryService = paymentRetryService;
        this.properties = properties;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        AtomicInteger threadNumber = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(properties.getParallelism(), runnable -> {
            Thread thread = new Thread(runnable, "bulk-claim-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    /**
     * Processes the claims, at most {@code parallelism} at a time for this request, and returns a response for each
     * in request order. A claim still running when the request times out is reported as pending; its work carries on
     * and the status endpoint resolves it.
     */
    public List<ClaimResponseType> submit(String organisationId,
                                          OrganisationDetailsResponse organisation,
                                          List<ClaimInput> claims,
                                          boolean validateOnly) {
        Set<String> seen = new HashSet<>();
        Queue<Runnable> pendingWork = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<ClaimResponseType>> futures = new ArrayList<>();
        for (ClaimInput claim : claims) {
            if (seen.add(claim.customerUniqueIdentifier())) {
                CompletableFuture<ClaimResponseType> future = new CompletableFuture<>();
                pendingWork.add(() -> future.complete(
                    processSafely(organisationId, organisation, claim, validateOnly)));
                futures.add(future);
            } else {
                futures.add(CompletableFuture.completedFuture(duplicateInRequest(claim)));
            }
        }

        int workers = Math.min(properties.getParallelism(), pendingWork.size());
        for (int i = 0; i < workers; i++) {
            executor.execute(() -> runNext(pendingWork));
        }

        awaitCompletion(futures);
        return IntStream.range(0, claims.size())
            .mapToObj(index -> completedOrPending(futures.get(index), claims.get(index)))
            .toList();
    }

    /**
     * Runs one claim, then requeues itself behind other requests' work if this request has more.
     */
    private void runNext(Queue<Runnable> pendingWork) {
        Runnable work = pendingWork.poll();
        if (work == null) {
            return;
        }
        try {
            work.run();
        } finally {
            if (!pendingWork.isEmpty()) {
                executor.execute(() -> runNext(pendingWork));
            }
        }
    }

    private void awaitCompletion(List<CompletableFuture<ClaimResponseType>> futures) {
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .get(properties.getRequestTimeout().toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            log.warn("Bulk claim request timed out after {}; unfinished claims are reported as pending",
                     properties.getRequestTimeout());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Bulk claim processing failed unexpectedly", e);
        }
    }

    private ClaimResponseType completedOrPending(CompletableFuture<ClaimResponseType> future, ClaimInput claim) {
        return future.isDone() && !future.isCompletedExceptionally()
            ? future.join()
            : responseBuilder.claimPending(claim.customerUniqueIdentifier());
    }

    private ClaimResponseType processSafely(String organisationId,
                                            OrganisationDetailsResponse organisation,
                                            ClaimInput claim,
                                            boolean validateOnly) {
        try {
            return process(organisationId, organisation, claim, validateOnly);
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            log.warn("Concurrent submission of claim {} for organisation {}", claim.customerUniqueIdentifier(),
                     organisationId, e);
            return responseBuilder.claimPending(claim.customerUniqueIdentifier());
        } catch (RuntimeException e) {
            log.error("Unexpected failure processing claim {} for organisation {}",
                      claim.customerUniqueIdentifier(), organisationId, e);
            return responseBuilder.claimFailure(claim.customerUniqueIdentifier(), BulkFailReason.SAVE_FAILED, null);
        }
    }

    private ClaimResponseType process(String organisationId,
                                      OrganisationDetailsResponse organisation,
                                      ClaimInput claim,
                                      boolean validateOnly) {
        final String cui = claim.customerUniqueIdentifier();

        Optional<ClaimResponseType> existing = respondToExisting(organisationId, organisation, claim, validateOnly);
        if (existing.isPresent()) {
            return existing.get();
        }

        BulkClaimPreparationResult preparation = claim.preparation().get();
        if (preparation instanceof Rejected rejected) {
            return reject(organisationId, claim, rejected.reason(), rejected.errors(), validateOnly);
        }
        PreparedBulkClaim prepared = ((Prepared) preparation).claim();

        boolean noticeOfActing = noticeOfActingRequired(prepared);
        if (validateOnly) {
            return responseBuilder.claimSuccess(cui, null, noticeOfActing);
        }

        UUID submissionId = stage(organisationId, organisation, claim, prepared);
        BulkClaimCcdResult result = ccdClient.createCase(submissionId);
        if (result.isCreated()) {
            return responseBuilder.claimSuccess(cui, String.valueOf(result.caseReference()), noticeOfActing);
        }
        if (result.retryable()) {
            return afterUnavailableCcd(submissionId, cui, noticeOfActing);
        }
        markFailed(submissionId, result.failReason(), result.errors());
        return responseBuilder.claimFailure(cui, result.failReason(), result.errors());
    }

    private ClaimResponseType afterUnavailableCcd(UUID submissionId, String cui, boolean noticeOfActing) {
        Long caseReference = repository.findById(submissionId)
            .map(BulkClaimSubmissionEntity::getCaseReference)
            .orElse(null);
        return caseReference != null
            ? responseBuilder.claimSuccess(cui, String.valueOf(caseReference), noticeOfActing)
            : responseBuilder.claimPending(cui);
    }

    private Optional<ClaimResponseType> respondToExisting(String organisationId,
                                                          OrganisationDetailsResponse organisation,
                                                          ClaimInput claim,
                                                          boolean validateOnly) {
        String cui = claim.customerUniqueIdentifier();
        Optional<BulkClaimSubmissionEntity> found =
            repository.findByOrganisationIdAndCustomerUniqueIdentifier(organisationId, cui);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        BulkClaimSubmissionEntity existing = found.get();
        boolean sameClaim = claim.claimHash().equals(existing.getRequestHash());

        return switch (existing.getStatus()) {
            case CASE_CREATED -> Optional.of(sameClaim
                ? resubmitted(existing, claim, organisation, validateOnly)
                : identifierReused(cui));
            case PENDING -> pendingOutcome(existing, cui, sameClaim, validateOnly);
            case FAILED_VALIDATION, FAILED -> Optional.empty();
        };
    }

    /**
     * A resubmission of a claim whose case exists replays the original success. When its PBA payment failed it
     * also schedules the payment again, from the payment account in this request.
     */
    private ClaimResponseType resubmitted(BulkClaimSubmissionEntity existing,
                                          ClaimInput claim,
                                          OrganisationDetailsResponse organisation,
                                          boolean validateOnly) {
        ClaimResponseType replay = replay(existing);
        if (validateOnly || !paymentRetryService.canRetry(existing)) {
            return replay;
        }

        BulkClaimPreparationResult preparation = claim.preparation().get();
        if (preparation instanceof Rejected rejected) {
            return responseBuilder.claimFailure(
                claim.customerUniqueIdentifier(), rejected.reason(), rejected.errors());
        }
        PreparedBulkClaim prepared = ((Prepared) preparation).claim();
        if (prepared.pbaAccountNumber() != null) {
            paymentRetryService.retry(existing, prepared.pbaAccountNumber(), prepared.pbaCustomerReference(),
                                      organisation.getName());
        }
        return replay;
    }

    private ClaimResponseType replay(BulkClaimSubmissionEntity submission) {
        return responseBuilder.claimSuccess(
            submission.getCustomerUniqueIdentifier(), String.valueOf(submission.getCaseReference()),
            submission.isNoticeOfActingRequired());
    }

    private static boolean noticeOfActingRequired(PreparedBulkClaim prepared) {
        return prepared.extras() != null && Boolean.TRUE.equals(prepared.extras().noticeOfActingRequired());
    }

    private Optional<ClaimResponseType> pendingOutcome(BulkClaimSubmissionEntity existing,
                                                       String cui,
                                                       boolean sameClaim,
                                                       boolean validateOnly) {
        if (existing.getCaseReference() != null) {
            return Optional.of(sameClaim
                ? replay(existing)
                : identifierReused(cui));
        }
        if (isStale(existing)) {
            return Optional.empty();
        }
        if (sameClaim) {
            return validateOnly ? Optional.empty() : Optional.of(responseBuilder.claimPending(cui));
        }
        return Optional.of(identifierReused(cui));
    }

    private boolean isStale(BulkClaimSubmissionEntity submission) {
        Instant lastTouched = submission.getLastModified() != null
            ? submission.getLastModified() : submission.getCreatedAt();
        Duration retryAfter = properties.getPendingRetryAfter();
        return lastTouched == null || lastTouched.plus(retryAfter).isBefore(Instant.now());
    }

    private ClaimResponseType identifierReused(String cui) {
        return responseBuilder.claimFailure(cui, BulkFailReason.IDENTIFIER_REUSED, null);
    }

    private ClaimResponseType duplicateInRequest(ClaimInput claim) {
        BulkValidationError error = new BulkValidationError(
            CUSTOMER_UNIQUE_IDENTIFIER_PATH, DUPLICATE_IN_REQUEST,
            "The customer unique identifier appears more than once in this request");
        return responseBuilder.claimFailure(
            claim.customerUniqueIdentifier(), BulkFailReason.IDENTIFIER_REUSED, List.of(error));
    }

    private ClaimResponseType reject(String organisationId,
                                     ClaimInput claim,
                                     BulkFailReason reason,
                                     List<BulkValidationError> errors,
                                     boolean validateOnly) {
        if (!validateOnly) {
            newTransaction.executeWithoutResult(status -> {
                BulkClaimSubmissionEntity row = rowFor(organisationId, claim);
                row.setStatus(BulkClaimSubmissionStatus.FAILED_VALIDATION);
                applyFailure(row, reason, errors);
                clearStaging(row);
                repository.save(row);
            });
        }
        return responseBuilder.claimFailure(claim.customerUniqueIdentifier(), reason, errors);
    }

    private UUID stage(String organisationId,
                       OrganisationDetailsResponse organisation,
                       ClaimInput claim,
                       PreparedBulkClaim prepared) {
        return newTransaction.execute(status -> {
            BulkClaimSubmissionEntity row = rowFor(organisationId, claim);
            row.setStatus(BulkClaimSubmissionStatus.PENDING);
            row.setStatusCode(BulkFailReason.CLAIM_PENDING.getStatusCode());
            row.setFailReasonCode(null);
            row.setFailReason(null);
            row.setNoticeOfActingRequired(noticeOfActingRequired(prepared));
            row.setValidationErrors(null);
            row.setStagedCaseData(serializer.writeCase(prepared.pcsCase()));
            row.setStagedExtras(serializer.writeExtras(prepared.extras()));
            row.setOrganisationSnapshot(serializer.writeOrganisation(organisation));
            row.setPbaAccountNumber(prepared.pbaAccountNumber());
            row.setPaymentCustomerReference(prepared.pbaCustomerReference());
            return repository.save(row).getId();
        });
    }

    private void markFailed(UUID submissionId, BulkFailReason reason, List<BulkValidationError> errors) {
        newTransaction.executeWithoutResult(status -> repository.findById(submissionId)
            .filter(row -> row.getStatus() == BulkClaimSubmissionStatus.PENDING && row.getCaseReference() == null)
            .ifPresent(row -> {
                row.setStatus(BulkClaimSubmissionStatus.FAILED);
                applyFailure(row, reason, errors);
                repository.save(row);
            }));
    }

    private BulkClaimSubmissionEntity rowFor(String organisationId, ClaimInput claim) {
        BulkClaimSubmissionEntity row = repository
            .findByOrganisationIdAndCustomerUniqueIdentifier(organisationId, claim.customerUniqueIdentifier())
            .orElseGet(() -> BulkClaimSubmissionEntity.builder()
                .organisationId(organisationId)
                .customerUniqueIdentifier(claim.customerUniqueIdentifier())
                .build());
        row.setClaimKind(claim.claimKind());
        row.setSchemaVersion(claim.schemaVersion());
        row.setRequestHash(claim.claimHash());
        return row;
    }

    private void applyFailure(BulkClaimSubmissionEntity row, BulkFailReason reason, List<BulkValidationError> errors) {
        row.setStatusCode(reason.getStatusCode());
        row.setFailReasonCode(reason.getFailReasonCode());
        row.setFailReason(reason.getReason());
        row.setValidationErrors(serializer.writeErrors(errors));
    }

    private static void clearStaging(BulkClaimSubmissionEntity row) {
        row.setStagedCaseData(null);
        row.setStagedExtras(null);
        row.setOrganisationSnapshot(null);
        row.setPbaAccountNumber(null);
        row.setPaymentCustomerReference(null);
    }
}
