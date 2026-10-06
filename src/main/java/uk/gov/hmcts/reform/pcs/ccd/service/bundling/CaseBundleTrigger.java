package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.AfterCompletionCallback;
import org.hibernate.engine.spi.TransactionCompletionCallbacks.BeforeCompletionCallback;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleExecutionContext;
import uk.gov.hmcts.ccd.sdk.bundling.job.OutboxBundleJobService;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Queues a case bundle regeneration whenever a document that belongs in the bundle is added to a
 * case, removed, restored or amended (type, folder, name, date), and when a general application
 * or counterclaim is issued (its documents become visible), however it happened. Documents reach
 * the case through many services and through cascades from the case, so this listens at the
 * persistence layer rather than in each
 * of them.
 *
 * <p>The outbox row is written just before the transaction commits, on the same connection, so the
 * job exists exactly when the document does: a rolled-back upload queues nothing. Jobs are
 * coalesced per case, so a claim with ten attachments, or ten uploads before the worker gets to
 * the first, produce one bundle that includes all of them.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "ccd.bundling.job", name = "enabled")
public class CaseBundleTrigger implements PostInsertEventListener, PostUpdateEventListener {

    static final String INITIATOR = "case-document-change";
    // DocumentEntity properties that change what the bundle contains or how it is laid out.
    private static final Set<String> BUNDLE_PROPERTIES = Set.of(
        "removed", "type", "categoryId", "fileName", "issueDate", "documentId", "generalApplication",
        "counterClaim", "contentType", "binaryUrl");
    // Issuing these makes their documents visible in case file view, and so in the bundle.
    private static final Set<String> GEN_APP_PROPERTIES = Set.of("state", "withoutNotice");
    private static final Set<String> COUNTER_CLAIM_PROPERTIES = Set.of("status");

    private final EntityManagerFactory entityManagerFactory;
    private final ObjectProvider<OutboxBundleJobService> bundleJobs;
    // Cases touched per session, flushed into the outbox once per transaction.
    private final Map<SharedSessionContractImplementor, Map<UUID, Long>> pendingBySession =
        Collections.synchronizedMap(new WeakHashMap<>());

    public CaseBundleTrigger(EntityManagerFactory entityManagerFactory,
                             ObjectProvider<OutboxBundleJobService> bundleJobs) {
        this.entityManagerFactory = entityManagerFactory;
        this.bundleJobs = bundleJobs;
    }

    @PostConstruct
    void register() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
            .getServiceRegistry()
            .requireService(EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (event.getEntity() instanceof DocumentEntity document && CaseBundleScope.belongsInBundle(document)) {
            queue(event.getSession(), document.getPcsCase());
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Object entity = event.getEntity();
        if (entity instanceof DocumentEntity document) {
            // A change that moves a document into or out of scope matters either way, so only
            // the bundle's own documents are ignored.
            if (document.getType() != DocumentType.CASE_BUNDLE && changed(event, BUNDLE_PROPERTIES)) {
                queue(event.getSession(), document.getPcsCase());
            }
        } else if (entity instanceof GenAppEntity genApp && changed(event, GEN_APP_PROPERTIES)) {
            queue(event.getSession(), genApp.getPcsCase());
        } else if (entity instanceof CounterClaimEntity counterClaim
            && changed(event, COUNTER_CLAIM_PROPERTIES)) {
            queue(event.getSession(), counterClaim.getPcsCase());
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private static boolean changed(PostUpdateEvent event, Set<String> properties) {
        int[] dirty = event.getDirtyProperties();
        if (dirty == null) {
            return false;
        }
        String[] names = event.getPersister().getPropertyNames();
        return Arrays.stream(dirty).anyMatch(index -> properties.contains(names[index]));
    }

    private void queue(SharedSessionContractImplementor session, PcsCaseEntity pcsCase) {
        if (pcsCase == null) {
            return;
        }
        UUID caseId = pcsCase.getId();
        // Read the reference only if the case is already loaded: never trigger a load mid-flush.
        Long caseReference = Hibernate.isInitialized(pcsCase) ? pcsCase.getCaseReference() : null;
        Map<UUID, Long> pending = pendingBySession.computeIfAbsent(session, s -> {
            var callbacks = session.getTransactionCompletionCallbacks();
            callbacks.registerCallback((BeforeCompletionCallback) completing -> submit(session));
            // A rolled-back transaction never reaches the before-completion callback.
            callbacks.registerCallback((AfterCompletionCallback) (success, completed) ->
                pendingBySession.remove(session));
            return new LinkedHashMap<>();
        });
        pending.merge(caseId, caseReference == null ? -1L : caseReference,
            (existing, latest) -> existing > 0 ? existing : latest);
    }

    private void submit(SharedSessionContractImplementor session) {
        Map<UUID, Long> pending = pendingBySession.remove(session);
        if (pending == null) {
            return;
        }
        OutboxBundleJobService jobs = bundleJobs.getObject();
        // Joining a waiting job locks its row until commit: submit in a fixed order so two
        // transactions touching the same cases cannot deadlock.
        new TreeMap<>(pending).forEach((caseId, caseReference) -> {
            BundleExecutionContext.Builder context = BundleExecutionContext.builder()
                .initiator(INITIATOR)
                .attribute(CaseBundleScope.CASE_ID_PARAMETER, caseId.toString());
            if (caseReference > 0) {
                context.caseReference(String.valueOf(caseReference));
            }
            var job = jobs.submitCoalesced(CaseBundleScope.coalesceKey(caseId),
                CaseBundleScope.selectorParameters(caseId), context.build());
            log.info("Case bundle regeneration queued for case {} as job {}", caseId, job.externalId());
        });
    }
}
