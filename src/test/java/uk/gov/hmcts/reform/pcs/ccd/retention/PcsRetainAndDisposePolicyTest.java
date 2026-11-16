package uk.gov.hmcts.reform.pcs.ccd.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.repository.CcdCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PcsRetainAndDisposePolicyTest {

    private static final int DISCARD_AFTER_DAYS = 30;
    private static final int SQL_LIMIT = 200;
    private static final long CASE_REF = 1794_0000_0000_0001L;

    @Mock
    private CcdCaseRepository ccdCaseRepository;
    @Mock
    private CaseDeletionService caseDeletionService;
    @Mock
    private FeatureToggleService featureToggleService;

    private PcsRetainAndDisposePolicy underTest;

    @BeforeEach
    void setUp() {
        underTest = new PcsRetainAndDisposePolicy(
            ccdCaseRepository,
            caseDeletionService,
            featureToggleService,
            DISCARD_AFTER_DAYS,
            SQL_LIMIT
        );
    }

    @Test
    void shouldReturnConfiguredCaseType() {
        try (MockedStatic<CaseType> caseType = mockStatic(CaseType.class)) {
            caseType.when(CaseType::getCaseType).thenReturn("PCS");

            assertThat(underTest.caseTypes()).containsExactly("PCS");
        }
    }

    @Test
    void shouldReturnNoCandidatesWhenFeatureFlagDisabled() {
        when(featureToggleService.isEnabled(FeatureFlag.DELETE_UNISSUED_CLAIM)).thenReturn(false);

        assertThat(underTest.findCandidatesForDisposal()).isEmpty();
        verifyNoInteractions(ccdCaseRepository);
    }

    @Test
    void shouldReturnExpiredUnissuedCandidatesWhenFeatureFlagEnabled() {
        when(featureToggleService.isEnabled(FeatureFlag.DELETE_UNISSUED_CLAIM)).thenReturn(true);
        when(ccdCaseRepository.findExpiredDraftCases(DISCARD_AFTER_DAYS, SQL_LIMIT))
            .thenReturn(List.of(CASE_REF));

        assertThat(underTest.findCandidatesForDisposal()).containsExactly(CASE_REF);
        verify(ccdCaseRepository).findExpiredDraftCases(DISCARD_AFTER_DAYS, SQL_LIMIT);
    }

    @Test
    void shouldDeleteServiceOwnedDataOnDisposeWithoutTouchingCcdSchemaDirectly() {
        underTest.dispose(CASE_REF);

        verify(caseDeletionService).deleteServiceOwnedData(CASE_REF);
        verify(caseDeletionService, never()).deleteCaseData(CASE_REF);
        verify(caseDeletionService, never()).deleteCcdCase(CASE_REF);
    }
}
