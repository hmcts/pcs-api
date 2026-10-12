package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleRequest;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleResult;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJob;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobCompletionHandler;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobContext;
import uk.gov.hmcts.ccd.sdk.bundling.job.OutboxBundleJobService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.util.InMemoryMultipartFile;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores a finished case bundle. A case has one bundle: a CASE_BUNDLE document, replaced in place
 * each time the bundle is regenerated, with the previous PDF deleted from CDAM. What the bundle
 * contains, where each document landed and which are missing is in the rendering job's report,
 * which the document row points at.
 *
 * <p>The PDF is uploaded to CDAM and attached to the case through the attachCaseBundle event
 * (CDAM grants ATTACH only to ccd_data); an unattached document expires after its TTL and is not
 * checked against case access. Renders can finish out of order, so a bundle replaces the stored
 * one only if its documents were selected later; a slower, older render is discarded.
 *
 * <p>Neither CDAM nor the event can join the database transaction. If attaching or storing fails,
 * the job fails with COMPLETION_FAILED and the upload is left behind (an unattached one expires);
 * the next document change queues a fresh bundle. A job run again after its lease expired simply
 * replaces the bundle with its new render.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ccd.bundling.job", name = "enabled")
public class CaseBundlePublisher implements BundleJobCompletionHandler {

    static final String CLASSIFICATION = "PUBLIC";
    // Summary key: whether this job's render became the case's bundle.
    static final String STORED = "stored";
    private static final ZoneId UK = ZoneId.of("Europe/London");

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final DocumentRepository documentRepository;
    private final EntityManager entityManager;
    private final CoreCaseDataApi coreCaseDataApi;
    private final OutboxBundleJobService bundleJobs;
    private final TransactionTemplate transaction;

    public CaseBundlePublisher(CaseDocumentClientApi caseDocumentClientApi,
                               AuthTokenGenerator authTokenGenerator,
                               @Qualifier("systemUpdateUserTokenProvider")
                               IdamTokenProvider systemUpdateUserTokenProvider,
                               DocumentRepository documentRepository,
                               EntityManager entityManager,
                               CoreCaseDataApi coreCaseDataApi,
                               OutboxBundleJobService bundleJobs,
                               PlatformTransactionManager transactionManager) {
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.documentRepository = documentRepository;
        this.entityManager = entityManager;
        this.coreCaseDataApi = coreCaseDataApi;
        this.bundleJobs = bundleJobs;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public Object onCompleted(BundleJob job, BundleJobContext context, BundleRequest request,
                              BundleResult result) throws IOException {
        UUID caseId = CaseBundleScope.caseId(context.parameters());
        Long caseReference = transaction.execute(status -> entityManager.find(PcsCaseEntity.class, caseId)
            .getCaseReference());
        if (Boolean.TRUE.equals(transaction.execute(status -> isSuperseded(storedBundle(caseId), job)))) {
            log.info("Case bundle job {} for case {} is older than the stored bundle; discarding it",
                     job.externalId(), caseReference);
            return Map.of(STORED, false);
        }

        Document uploaded = upload(result);
        attach(caseReference, uploaded, result);
        Stored stored = transaction.execute(status -> store(caseId, job, result, uploaded));

        // The PDF no longer stored on the case: the one replaced, or this one if a newer bundle
        // was stored while it was being attached.
        stored.discarded().ifPresent(this::deleteFromCdam);
        log.info("Case bundle for case {} {}: {} pages, {} document(s) missing", caseReference,
                 stored.document().isPresent() ? "stored" : "discarded as superseded", result.pageCount(),
                 result.missingDocuments().size());
        return stored.document()
            .<Object>map(document -> Map.of(
                STORED, true,
                "documentId", document.getId().toString(),
                "pageCount", result.pageCount(),
                "missingDocuments", result.missingDocuments().size()))
            .orElse(Map.of(STORED, false));
    }

    /** The outcome of storing: the stored document unless superseded, and the CDAM id to delete. */
    private record Stored(Optional<DocumentEntity> document, Optional<UUID> discarded) {
    }

    private Optional<DocumentEntity> storedBundle(UUID caseId) {
        return documentRepository.findByPcsCase_IdAndType(caseId, DocumentType.CASE_BUNDLE);
    }

    /** Whether the stored bundle's documents were selected after this job's. */
    private boolean isSuperseded(Optional<DocumentEntity> stored, BundleJob job) {
        return stored
            .map(DocumentEntity::getBundleJobId)
            .filter(storedJobId -> !storedJobId.equals(job.externalId()))
            .flatMap(bundleJobs::find)
            .map(storedJob -> selectedAt(storedJob).isAfter(selectedAt(job)))
            .orElse(false);
    }

    private static Instant selectedAt(BundleJob job) {
        return job.claimedAt().orElse(job.submittedAt());
    }

    private void attach(Long caseReference, Document uploaded, BundleResult result) {
        String authorisation = systemUpdateUserTokenProvider.getAuthToken();
        String serviceAuthorisation = authTokenGenerator.generate();
        String caseId = String.valueOf(caseReference);
        final StartEventResponse start = coreCaseDataApi.startEvent(authorisation, serviceAuthorisation, caseId,
                                                                    EventId.attachCaseBundle.name());
        Map<String, Object> bundleDocument = new LinkedHashMap<>();
        bundleDocument.put("document_url", uploaded.links.self.href);
        bundleDocument.put("document_binary_url", uploaded.links.binary.href);
        bundleDocument.put("document_filename", result.artifact().fileName());
        bundleDocument.put("document_hash", uploaded.hashToken);
        coreCaseDataApi.createEvent(authorisation, serviceAuthorisation, caseId, CaseDataContent.builder()
            .event(Event.builder()
                       .id(EventId.attachCaseBundle.name())
                       .summary("Case bundle generated: " + result.pageCount() + " pages")
                       .build())
            .eventToken(start.getToken())
            .data(Map.of("caseBundleAttachment", bundleDocument))
            .build());
    }

    private Document upload(BundleResult result) throws IOException {
        byte[] pdf;
        try (InputStream in = result.artifact().open()) {
            pdf = in.readAllBytes();
        }
        return caseDocumentClientApi.uploadDocuments(
                systemUpdateUserTokenProvider.getAuthToken(),
                authTokenGenerator.generate(),
                new DocumentUploadRequest(CLASSIFICATION, CaseType.getCaseType(), CaseType.getJurisdictionId(),
                    List.of(new InMemoryMultipartFile("files", result.artifact().fileName(),
                        result.artifact().mediaType(), pdf))))
            .getDocuments().getFirst();
    }

    private Stored store(UUID caseId, BundleJob job, BundleResult result, Document uploaded) {
        // Serialises bundle stores per case, so deciding which render is newer cannot race. A plain
        // row lock: Hibernate's entity locking cannot follow pcs_case's secondary mappings.
        entityManager.createNativeQuery("select id from pcs_case where id = :caseId for update")
            .setParameter("caseId", caseId)
            .getSingleResult();
        Optional<DocumentEntity> current = storedBundle(caseId);
        UUID uploadedId = documentId(uploaded);
        if (isSuperseded(current, job)) {
            return new Stored(Optional.empty(), Optional.of(uploadedId));
        }

        DocumentEntity bundle = current.orElseGet(() -> DocumentEntity.builder()
            .pcsCase(entityManager.getReference(PcsCaseEntity.class, caseId))
            .type(DocumentType.CASE_BUNDLE)
            .categoryId(CaseBundleScope.BUNDLES_CATEGORY_ID)
            .build());
        final Optional<UUID> replaced = current.map(DocumentEntity::getDocumentId)
            .filter(previous -> !previous.equals(uploadedId));
        bundle.setDocumentId(uploadedId);
        bundle.setUrl(uploaded.links.self.href);
        bundle.setBinaryUrl(uploaded.links.binary.href);
        bundle.setFileName(result.artifact().fileName());
        bundle.setContentType(result.artifact().mediaType());
        bundle.setSize(result.artifact().size());
        bundle.setIssueDate(LocalDate.ofInstant(selectedAt(job), UK));
        bundle.setSubmittedDate(Instant.now());
        bundle.setBundleJobId(job.externalId());
        return new Stored(Optional.of(documentRepository.save(bundle)), replaced);
    }

    private void deleteFromCdam(UUID documentId) {
        try {
            caseDocumentClientApi.deleteDocument(systemUpdateUserTokenProvider.getAuthToken(),
                                                 authTokenGenerator.generate(), documentId, true);
        } catch (RuntimeException e) {
            // The bundle stored on the case is unaffected; this PDF is merely orphaned in CDAM.
            log.warn("Could not delete superseded case bundle {} from CDAM", documentId, e);
        }
    }

    private static UUID documentId(Document document) {
        String self = document.links.self.href;
        return UUID.fromString(self.substring(self.lastIndexOf('/') + 1));
    }
}
