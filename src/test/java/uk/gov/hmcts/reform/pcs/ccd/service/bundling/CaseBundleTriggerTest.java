package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import uk.gov.hmcts.ccd.sdk.bundling.api.BundleExecutionContext;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJob;
import uk.gov.hmcts.ccd.sdk.bundling.job.BundleJobState;
import uk.gov.hmcts.ccd.sdk.bundling.job.OutboxBundleJobService;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class CaseBundleTriggerTest {

    private static final String[] DOCUMENT_PROPERTIES = {"description", "fileName", "removed"};

    @Mock
    private EntityManagerFactory entityManagerFactory;
    @Mock
    private ObjectProvider<OutboxBundleJobService> bundleJobsProvider;
    @Mock
    private OutboxBundleJobService bundleJobs;
    @Mock
    private EventSource session;
    @Mock
    private ActionQueue actionQueue;

    private CaseBundleTrigger underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseBundleTrigger(entityManagerFactory, bundleJobsProvider);
        lenient().when(session.getActionQueue()).thenReturn(actionQueue);
        lenient().when(bundleJobsProvider.getObject()).thenReturn(bundleJobs);
        lenient().when(bundleJobs.submitCoalesced(any(), any(), any())).thenAnswer(invocation -> job());
    }

    @Test
    void shouldListenForInsertsAndUpdates() {
        SessionFactoryImplementor sessionFactory = mock(SessionFactoryImplementor.class);
        ServiceRegistryImplementor serviceRegistry = mock(ServiceRegistryImplementor.class);
        EventListenerRegistry listenerRegistry = mock(EventListenerRegistry.class);
        when(entityManagerFactory.unwrap(SessionFactoryImplementor.class)).thenReturn(sessionFactory);
        when(sessionFactory.getServiceRegistry()).thenReturn(serviceRegistry);
        when(serviceRegistry.requireService(EventListenerRegistry.class)).thenReturn(listenerRegistry);

        underTest.register();

        verify(listenerRegistry).appendListeners(EventType.POST_INSERT, underTest);
        verify(listenerRegistry).appendListeners(EventType.POST_UPDATE, underTest);
        assertThat(underTest.requiresPostCommitHandling(mock(EntityPersister.class))).isFalse();
    }

    @Test
    void shouldQueueOneCoalescedJobForTheCaseJustBeforeCommit() {
        PcsCaseEntity pcsCase = pcsCase();

        underTest.onPostInsert(inserted(document(pcsCase)));
        verifyNoInteractions(bundleJobs);
        beforeCompletion().doBeforeTransactionCompletion(null);

        ArgumentCaptor<BundleExecutionContext> context = ArgumentCaptor.forClass(BundleExecutionContext.class);
        verify(bundleJobs).submitCoalesced(eq(CaseBundleScope.coalesceKey(pcsCase.getId())),
                                           eq(CaseBundleScope.selectorParameters(pcsCase.getId())),
                                           context.capture());
        assertThat(context.getValue().initiator()).contains(CaseBundleTrigger.INITIATOR);
        assertThat(context.getValue().caseReference()).contains("1234567812345678");
        assertThat(context.getValue().attributes())
            .containsEntry(CaseBundleScope.CASE_ID_PARAMETER, pcsCase.getId().toString());
    }

    @Test
    void shouldQueueEachCaseOnceInIdOrderHoweverManyDocumentsChange() {
        PcsCaseEntity later = pcsCase(new UUID(0, 2));
        PcsCaseEntity earlier = pcsCase(new UUID(0, 1));

        underTest.onPostInsert(inserted(document(later)));
        underTest.onPostInsert(inserted(document(earlier)));
        underTest.onPostInsert(inserted(document(later)));
        beforeCompletion().doBeforeTransactionCompletion(null);

        verify(actionQueue, times(1)).registerProcess(any(BeforeTransactionCompletionProcess.class));
        InOrder order = inOrder(bundleJobs);
        order.verify(bundleJobs).submitCoalesced(eq(CaseBundleScope.coalesceKey(earlier.getId())), any(), any());
        order.verify(bundleJobs).submitCoalesced(eq(CaseBundleScope.coalesceKey(later.getId())), any(), any());
        order.verifyNoMoreInteractions();
    }

    @Test
    void shouldIgnoreInsertsOfDocumentsOutsideTheBundle() {
        DocumentEntity bundle = document(pcsCase());
        bundle.setType(DocumentType.CASE_BUNDLE);
        DocumentEntity removed = document(pcsCase());
        removed.setRemoved(true);

        underTest.onPostInsert(inserted(bundle));
        underTest.onPostInsert(inserted(removed));
        underTest.onPostInsert(inserted(new GenAppEntity()));

        verifyNoInteractions(actionQueue);
    }

    @Test
    void shouldIgnoreADocumentWithNoCase() {
        underTest.onPostInsert(inserted(document(null)));

        verifyNoInteractions(actionQueue);
    }

    @Test
    void shouldQueueWhenAChangeAffectsTheBundle() {
        DocumentEntity document = document(pcsCase());

        underTest.onPostUpdate(updated(document, DOCUMENT_PROPERTIES, 2));

        verify(actionQueue).registerProcess(any(BeforeTransactionCompletionProcess.class));
    }

    @Test
    void shouldIgnoreChangesThatDoNotAffectTheBundle() {
        DocumentEntity document = document(pcsCase());
        DocumentEntity bundle = document(pcsCase());
        bundle.setType(DocumentType.CASE_BUNDLE);

        underTest.onPostUpdate(updated(document, DOCUMENT_PROPERTIES, 0));
        underTest.onPostUpdate(updated(document, DOCUMENT_PROPERTIES));
        underTest.onPostUpdate(updatedWithUnknownDirtiness(document));
        underTest.onPostUpdate(updated(bundle, DOCUMENT_PROPERTIES, 2));
        underTest.onPostUpdate(updated(new PcsCaseEntity(), new String[] {"caseReference"}, 0));

        verifyNoInteractions(actionQueue);
    }

    @Test
    void shouldQueueWhenAnApplicationOrCounterclaimIsIssued() {
        GenAppEntity genApp = new GenAppEntity();
        genApp.setPcsCase(pcsCase());
        CounterClaimEntity counterClaim = new CounterClaimEntity();
        counterClaim.setPcsCase(pcsCase());

        underTest.onPostUpdate(updated(genApp, new String[] {"applicationType", "state"}, 1));
        underTest.onPostUpdate(updated(counterClaim, new String[] {"status"}, 0));
        beforeCompletion().doBeforeTransactionCompletion(null);

        verify(bundleJobs, times(2)).submitCoalesced(any(), any(), any());
    }

    @Test
    void shouldIgnoreOtherApplicationAndCounterclaimChanges() {
        GenAppEntity genApp = new GenAppEntity();
        genApp.setPcsCase(pcsCase());
        CounterClaimEntity counterClaim = new CounterClaimEntity();
        counterClaim.setPcsCase(pcsCase());

        underTest.onPostUpdate(updated(genApp, new String[] {"applicationType", "state"}, 0));
        underTest.onPostUpdate(updated(counterClaim, new String[] {"status", "claimAmount"}, 1));

        verifyNoInteractions(actionQueue);
    }

    @Test
    void shouldNotLoadAnUninitialisedCaseToReadItsReference() {
        UUID caseId = UUID.randomUUID();
        PcsCaseEntity proxy = uninitialisedProxy(caseId);

        underTest.onPostInsert(inserted(document(proxy)));
        beforeCompletion().doBeforeTransactionCompletion(null);

        ArgumentCaptor<BundleExecutionContext> context = ArgumentCaptor.forClass(BundleExecutionContext.class);
        verify(bundleJobs).submitCoalesced(eq(CaseBundleScope.coalesceKey(caseId)), any(), context.capture());
        assertThat(context.getValue().caseReference()).isEmpty();
        verify(proxy, never()).getCaseReference();
    }

    @Test
    void shouldUseTheCaseReferenceOnceAnyChangeHasItLoaded() {
        UUID caseId = UUID.randomUUID();

        underTest.onPostInsert(inserted(document(uninitialisedProxy(caseId))));
        underTest.onPostInsert(inserted(document(pcsCase(caseId))));
        beforeCompletion().doBeforeTransactionCompletion(null);

        ArgumentCaptor<BundleExecutionContext> context = ArgumentCaptor.forClass(BundleExecutionContext.class);
        verify(bundleJobs).submitCoalesced(any(), any(), context.capture());
        assertThat(context.getValue().caseReference()).contains("1234567812345678");
    }

    @Test
    void shouldStartAfreshInTheSessionsNextTransactionAfterARollback() {
        underTest.onPostInsert(inserted(document(pcsCase())));
        afterCompletion().doAfterTransactionCompletion(false, null);

        underTest.onPostInsert(inserted(document(pcsCase())));

        verify(actionQueue, times(2)).registerProcess(any(BeforeTransactionCompletionProcess.class));
        verifyNoInteractions(bundleJobs);
    }

    @Test
    void shouldSubmitNothingTwice() {
        underTest.onPostInsert(inserted(document(pcsCase())));
        BeforeTransactionCompletionProcess beforeCompletion = beforeCompletion();

        beforeCompletion.doBeforeTransactionCompletion(null);
        beforeCompletion.doBeforeTransactionCompletion(null);

        verify(bundleJobs, times(1)).submitCoalesced(any(), any(), any());
    }

    private BeforeTransactionCompletionProcess beforeCompletion() {
        ArgumentCaptor<BeforeTransactionCompletionProcess> process =
            ArgumentCaptor.forClass(BeforeTransactionCompletionProcess.class);
        verify(actionQueue).registerProcess(process.capture());
        return process.getValue();
    }

    private AfterTransactionCompletionProcess afterCompletion() {
        ArgumentCaptor<AfterTransactionCompletionProcess> process =
            ArgumentCaptor.forClass(AfterTransactionCompletionProcess.class);
        verify(actionQueue).registerProcess(process.capture());
        return process.getValue();
    }

    private PostInsertEvent inserted(Object entity) {
        PostInsertEvent event = mock(PostInsertEvent.class);
        when(event.getEntity()).thenReturn(entity);
        lenient().when(event.getSession()).thenReturn(session);
        return event;
    }

    private PostUpdateEvent updated(Object entity, String[] propertyNames, int... dirty) {
        PostUpdateEvent event = updatedWithUnknownDirtiness(entity);
        EntityPersister persister = mock(EntityPersister.class);
        lenient().when(persister.getPropertyNames()).thenReturn(propertyNames);
        lenient().when(event.getPersister()).thenReturn(persister);
        lenient().when(event.getDirtyProperties()).thenReturn(dirty);
        return event;
    }

    private PostUpdateEvent updatedWithUnknownDirtiness(Object entity) {
        PostUpdateEvent event = mock(PostUpdateEvent.class);
        when(event.getEntity()).thenReturn(entity);
        lenient().when(event.getSession()).thenReturn(session);
        return event;
    }

    private static PcsCaseEntity pcsCase() {
        return pcsCase(UUID.randomUUID());
    }

    private static PcsCaseEntity pcsCase(UUID id) {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setId(id);
        pcsCase.setCaseReference(1234567812345678L);
        return pcsCase;
    }

    private static PcsCaseEntity uninitialisedProxy(UUID caseId) {
        PcsCaseEntity proxy = mock(PcsCaseEntity.class, withSettings().extraInterfaces(HibernateProxy.class));
        LazyInitializer lazyInitializer = mock(LazyInitializer.class);
        lenient().when(((HibernateProxy) proxy).asHibernateProxy()).thenReturn((HibernateProxy) proxy);
        lenient().when(((HibernateProxy) proxy).getHibernateLazyInitializer()).thenReturn(lazyInitializer);
        lenient().when(lazyInitializer.isUninitialized()).thenReturn(true);
        when(proxy.getId()).thenReturn(caseId);
        return proxy;
    }

    private static DocumentEntity document(PcsCaseEntity pcsCase) {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .documentId(UUID.randomUUID())
            .type(DocumentType.WITNESS_STATEMENT)
            .categoryId(CaseFileCategory.EVIDENCE.getId())
            .build();
    }

    private static BundleJob job() {
        Instant now = Instant.now();
        return new BundleJob(UUID.randomUUID(), BundleJobState.QUEUED, 0, now, now, Optional.empty(), 0,
                             Optional.empty(), Optional.empty(), Optional.empty());
    }
}
