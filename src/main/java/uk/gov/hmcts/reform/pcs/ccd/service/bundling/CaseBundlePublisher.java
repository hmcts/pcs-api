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
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleSection;
import uk.gov.hmcts.ccd.sdk.bundling.api.DocumentResult;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJob;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobCompletionHandler;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobContext;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.bundling.CaseBundleDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.bundling.CaseBundleFolder;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseBundleEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CaseBundleRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DocumentRepository;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Stores a finished case bundle: uploads the PDF to CDAM, has the data store attach it to the case
 * through the attachCaseBundle event (CDAM grants ATTACH only to ccd_data), then records it as a
 * CASE_BUNDLE document plus a case_bundle row. Attaching matters: an unattached CDAM document
 * expires after its TTL and is not checked against case access. The new bundle becomes latest unless a
 * bundle whose documents were selected later has already been stored, so a slow render that
 * finishes after a newer one never displaces it.
 *
 * <p>Neither CDAM nor the event can join the database transaction. If attaching fails, the upload
 * is left unattached and expires with CDAM's TTL. If recording fails after attaching, the PDF stays
 * on the case unrecorded. Either way the job fails with COMPLETION_FAILED, and the next document
 * change queues a fresh bundle. A job re-run after its lease expired finds its own bundle already
 * recorded and returns it rather than storing a second one.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ccd.bundling.job", name = "enabled")
public class CaseBundlePublisher implements BundleJobCompletionHandler {

    static final String CLASSIFICATION = "PUBLIC";
    private static final ZoneId UK = ZoneId.of("Europe/London");

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final DocumentRepository documentRepository;
    private final CaseBundleRepository caseBundleRepository;
    private final EntityManager entityManager;
    private final CoreCaseDataApi coreCaseDataApi;
    private final TransactionTemplate transaction;

    public CaseBundlePublisher(CaseDocumentClientApi caseDocumentClientApi,
                               AuthTokenGenerator authTokenGenerator,
                               @Qualifier("systemUpdateUserTokenProvider")
                               IdamTokenProvider systemUpdateUserTokenProvider,
                               DocumentRepository documentRepository,
                               CaseBundleRepository caseBundleRepository,
                               EntityManager entityManager,
                               CoreCaseDataApi coreCaseDataApi,
                               PlatformTransactionManager transactionManager) {
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.documentRepository = documentRepository;
        this.caseBundleRepository = caseBundleRepository;
        this.entityManager = entityManager;
        this.coreCaseDataApi = coreCaseDataApi;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public Object onCompleted(BundleJob job, BundleJobContext context, BundleRequest request,
                              BundleResult result) throws IOException {
        Optional<CaseBundleEntity> alreadyStored = caseBundleRepository.findByJobId(job.externalId());
        if (alreadyStored.isPresent()) {
            log.warn("Case bundle job {} was already stored as {}; not storing it again", job.externalId(),
                     alreadyStored.get().getId());
            return summary(alreadyStored.get());
        }
        UUID caseId = CaseBundleScope.caseId(context.parameters());
        Long caseReference = transaction.execute(status -> entityManager.find(PcsCaseEntity.class, caseId)
            .getCaseReference());
        Document uploaded = upload(result);
        attach(caseReference, uploaded, result);
        Instant selectedAt = job.claimedAt().orElse(job.submittedAt());

        CaseBundleEntity bundle = transaction.execute(status -> store(caseId, job, request, result,
                                                                       uploaded, selectedAt));
        log.info("Case bundle {} stored for case {}: {} pages, latest={}", bundle.getId(), caseReference,
                 result.pageCount(), bundle.isLatest());
        return summary(bundle);
    }

    private static Map<String, Object> summary(CaseBundleEntity bundle) {
        return Map.of(
            "caseBundleId", bundle.getId().toString(),
            "documentId", bundle.getDocument().getId().toString(),
            "pageCount", bundle.getPageCount(),
            "latest", bundle.isLatest()
        );
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

    private CaseBundleEntity store(UUID caseId, BundleJob job, BundleRequest request, BundleResult result,
                                    Document uploaded, Instant selectedAt) {
        // Serialises bundle stores per case, so deciding which bundle is latest cannot race. A plain
        // row lock: Hibernate's entity locking cannot follow pcs_case's secondary mappings.
        entityManager.createNativeQuery("select id from pcs_case where id = :caseId for update")
            .setParameter("caseId", caseId)
            .getSingleResult();
        PcsCaseEntity pcsCase = entityManager.getReference(PcsCaseEntity.class, caseId);

        DocumentEntity stitched = documentRepository.save(DocumentEntity.builder()
            .pcsCase(pcsCase)
            .type(DocumentType.CASE_BUNDLE)
            .categoryId(CaseBundleScope.BUNDLES_CATEGORY_ID)
            .documentId(documentId(uploaded))
            .url(uploaded.links.self.href)
            .binaryUrl(uploaded.links.binary.href)
            .fileName(result.artifact().fileName())
            .contentType(result.artifact().mediaType())
            .size(result.artifact().size())
            .issueDate(LocalDate.ofInstant(selectedAt, UK))
            .build());

        Optional<CaseBundleEntity> current = caseBundleRepository.findByPcsCase_IdAndLatestTrue(caseId);
        boolean latest = current.map(previous -> !previous.getDocumentsSelectedAt().isAfter(selectedAt))
            .orElse(true);
        if (latest && current.isPresent()) {
            current.get().setLatest(false);
            // Flush the demotion first: at most one latest bundle per case is enforced per statement.
            entityManager.flush();
        }

        return caseBundleRepository.save(CaseBundleEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .document(stitched)
            .jobId(job.externalId())
            .bundleTitle(request.title())
            .fileName(result.artifact().fileName())
            .stitchStatus(result.outcome().name())
            .pageCount(result.pageCount())
            .sha256(result.artifact().sha256())
            .folders(folders(request, result))
            .warnings(result.warnings().stream().map(warning -> warning.code() + ": " + warning.message()).toList())
            .hasTableOfContents(request.presentation().tableOfContents())
            .hasCoversheets(request.presentation().documentCoverSheets())
            .hasFolderCoversheets(request.presentation().sectionCoverSheets())
            .pageNumberFormat(request.presentation().pageNumbers().name())
            .documentsSelectedAt(selectedAt)
            .latest(latest)
            .build());
    }

    private List<CaseBundleFolder> folders(BundleRequest request, BundleResult result) {
        Map<String, DocumentResult> placements = result.documents().stream()
            .collect(Collectors.toMap(DocumentResult::documentId, Function.identity(), (a, b) -> a));
        Map<UUID, DocumentEntity> sources = documentRepository.findAllById(request.allDocuments().stream()
                .map(document -> UUID.fromString(document.id()))
                .toList())
            .stream()
            .collect(Collectors.toMap(DocumentEntity::getId, Function.identity()));

        List<CaseBundleFolder> folders = new ArrayList<>();
        List<BundleSection> sections = request.root().sections();
        for (int folderIndex = 0; folderIndex < sections.size(); folderIndex++) {
            BundleSection section = sections.get(folderIndex);
            List<CaseBundleDocument> documents = new ArrayList<>();
            for (int documentIndex = 0; documentIndex < section.documents().size(); documentIndex++) {
                var document = section.documents().get(documentIndex);
                DocumentEntity source = sources.get(UUID.fromString(document.id()));
                Optional<DocumentResult> placement = Optional.ofNullable(placements.get(document.id()));
                documents.add(new CaseBundleDocument(
                    UUID.fromString(document.id()),
                    document.title(),
                    source == null ? null : source.getDescription(),
                    source == null ? null : source.getUrl(),
                    document.date().orElse(null),
                    documentIndex,
                    placement.map(DocumentResult::startPage).orElse(null),
                    placement.map(DocumentResult::pageCount).orElse(null),
                    placement.map(DocumentResult::sha256).orElse(null)
                ));
            }
            folders.add(new CaseBundleFolder(section.title(), categoryId(section.title()), folderIndex, documents));
        }
        return folders;
    }

    private static String categoryId(String folderName) {
        for (CaseFileCategory category : CaseFileCategory.values()) {
            if (category.getLabel().equals(folderName)) {
                return category.getId();
            }
        }
        return null;
    }

    private static UUID documentId(Document document) {
        String self = document.links.self.href;
        return UUID.fromString(self.substring(self.lastIndexOf('/') + 1));
    }
}
