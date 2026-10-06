package uk.gov.hmcts.reform.pcs.ccd.service.bundling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseBundleScopeTest {

    @Test
    void shouldIncludeAnOrdinaryCaseDocument() {
        assertThat(CaseBundleScope.belongsInBundle(document())).isTrue();
        assertThat(CaseBundleScope.isVisibleToInternalReaders(document())).isTrue();
    }

    @Test
    void shouldLeaveOutARemovedDocument() {
        DocumentEntity removed = document();
        removed.setRemoved(true);

        assertThat(CaseBundleScope.isBundleable(removed)).isTrue();
        assertThat(CaseBundleScope.belongsInBundle(removed)).isFalse();
        assertThat(CaseBundleScope.isVisibleToInternalReaders(removed)).isFalse();
    }

    @Test
    void shouldLeaveOutADocumentNotHeldInCdam() {
        DocumentEntity notInCdam = document();
        notInCdam.setDocumentId(null);

        assertThat(CaseBundleScope.isBundleable(notInCdam)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = DocumentType.class, names = {"CASE_BUNDLE", "DEFENDANT_ACCESS_CODE"})
    void shouldLeaveOutBundlesAndAccessCodeLetters(DocumentType type) {
        DocumentEntity excluded = document();
        excluded.setType(type);

        assertThat(CaseBundleScope.isBundleable(excluded)).isFalse();
    }

    @Test
    void shouldLeaveOutAnythingFiledUnderBundles() {
        DocumentEntity filedAsBundle = document();
        filedAsBundle.setCategoryId(CaseBundleScope.BUNDLES_CATEGORY_ID);

        assertThat(CaseBundleScope.isBundleable(filedAsBundle)).isFalse();
    }

    @Test
    void shouldHideApplicationDocumentsUntilTheApplicationIsIssued() {
        DocumentEntity pending = documentOf(genApp(GenAppState.PENDING_GEN_APP_ISSUED, VerticalYesNo.NO));
        DocumentEntity issued = documentOf(genApp(GenAppState.GEN_APP_ISSUED, VerticalYesNo.NO));

        assertThat(CaseBundleScope.isVisibleToInternalReaders(pending)).isFalse();
        assertThat(CaseBundleScope.isVisibleToInternalReaders(issued)).isTrue();
    }

    @Test
    void shouldShowWithoutNoticeApplicationDocumentsBeforeIssue() {
        DocumentEntity withoutNotice = documentOf(genApp(GenAppState.PENDING_GEN_APP_ISSUED, VerticalYesNo.YES));

        assertThat(CaseBundleScope.isVisibleToInternalReaders(withoutNotice)).isTrue();
    }

    @Test
    void shouldHideCounterclaimDocumentsUntilTheCounterclaimIsIssued() {
        DocumentEntity pending = document();
        pending.setCounterClaim(counterClaim(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED));
        DocumentEntity issued = document();
        issued.setCounterClaim(counterClaim(CounterClaimState.COUNTER_CLAIM_ISSUED));

        assertThat(CaseBundleScope.isVisibleToInternalReaders(pending)).isFalse();
        assertThat(CaseBundleScope.isVisibleToInternalReaders(issued)).isTrue();
    }

    @Test
    void shouldKeyJobsAndParametersByCase() {
        UUID caseId = UUID.randomUUID();

        assertThat(CaseBundleScope.coalesceKey(caseId)).isEqualTo("pcs-case-bundle:" + caseId);
        assertThat(CaseBundleScope.caseId(CaseBundleScope.selectorParameters(caseId))).isEqualTo(caseId);
    }

    @Test
    void shouldRejectAJobWithNoCase() {
        Map<String, String> noCase = Map.of();

        assertThatThrownBy(() -> CaseBundleScope.caseId(noCase))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(CaseBundleScope.CASE_ID_PARAMETER);
    }

    private static DocumentEntity document() {
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .documentId(UUID.randomUUID())
            .type(DocumentType.WITNESS_STATEMENT)
            .categoryId(CaseFileCategory.EVIDENCE.getId())
            .build();
    }

    private static DocumentEntity documentOf(GenAppEntity genApp) {
        DocumentEntity document = document();
        document.setGeneralApplication(genApp);
        return document;
    }

    private static GenAppEntity genApp(GenAppState state, VerticalYesNo withoutNotice) {
        GenAppEntity genApp = new GenAppEntity();
        genApp.setState(state);
        genApp.setWithoutNotice(withoutNotice);
        return genApp;
    }

    private static CounterClaimEntity counterClaim(CounterClaimState status) {
        CounterClaimEntity counterClaim = new CounterClaimEntity();
        counterClaim.setStatus(status);
        return counterClaim;
    }
}
