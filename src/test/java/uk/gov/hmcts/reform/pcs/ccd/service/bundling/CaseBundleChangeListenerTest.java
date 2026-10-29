package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import uk.gov.hmcts.ccd.sdk.bundling.managed.CaseBundles;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import java.util.UUID;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseBundleChangeListenerTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private ObjectProvider<CaseBundles> provider;
    @Mock
    private CaseBundles bundles;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(CASE_ID).build();

    private CaseBundleChangeListener underTest;

    @BeforeEach
    void setUp() {
        lenient().when(provider.getIfAvailable()).thenReturn(bundles);
        underTest = new CaseBundleChangeListener(provider);
    }

    @Test
    void shouldQueueTheBundleWhenADocumentIsPersistedUpdatedOrRemoved() {
        DocumentEntity document = DocumentEntity.builder().pcsCase(pcsCase).build();

        underTest.persisted(document);
        underTest.updated(document);
        underTest.removed(document);

        verify(bundles, times(3)).changed(CaseBundleDefinition.NAME, CASE_ID.toString());
    }

    @Test
    void shouldNotQueueTheBundleWhenAnApplicationChangesWithoutChangingItsVisibility() {
        GenAppEntity application = GenAppEntity.builder().pcsCase(pcsCase).state(GenAppState.GEN_APP_ISSUED).build();
        underTest.loaded(application);

        application.setRank(2);
        underTest.updated(application);

        verify(bundles, never()).changed(CaseBundleDefinition.NAME, CASE_ID.toString());
    }

    @Test
    void shouldQueueTheBundleOnceWhenAnApplicationIsIssued() {
        GenAppEntity application = GenAppEntity.builder().pcsCase(pcsCase).withoutNotice(VerticalYesNo.NO).build();
        underTest.persisted(application);

        application.setState(GenAppState.GEN_APP_ISSUED);
        underTest.updated(application);
        underTest.updated(application);

        verify(bundles).changed(CaseBundleDefinition.NAME, CASE_ID.toString());
    }

    @Test
    void shouldQueueTheBundleWhenACounterclaimIsIssued() {
        CounterClaimEntity counterClaim = CounterClaimEntity.builder().pcsCase(pcsCase)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED).build();
        underTest.loaded(counterClaim);

        counterClaim.setStatus(CounterClaimState.COUNTER_CLAIM_ISSUED);
        underTest.updated(counterClaim);

        verify(bundles).changed(CaseBundleDefinition.NAME, CASE_ID.toString());
    }

    @Test
    void shouldQueueTheBundleWhenAnApplicationIsRemoved() {
        GenAppEntity application = GenAppEntity.builder().pcsCase(pcsCase).build();

        underTest.removed(application);

        verify(bundles).changed(CaseBundleDefinition.NAME, CASE_ID.toString());
    }

    @Test
    void shouldDoNothingWhenCaseBundlesAreOff() {
        when(provider.getIfAvailable()).thenReturn(null);

        underTest.persisted(DocumentEntity.builder().pcsCase(pcsCase).build());

        verifyNoInteractions(bundles);
    }
}
