package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.AgreementClaimantLegalRep;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.StatementOfTruthRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.mapper.StatementOfTruthMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthServiceTest {

    @InjectMocks
    private StatementOfTruthService statementOfTruthService;

    @Mock
    private StatementOfTruthRepository statementOfTruthRepository;

    @Mock
    private StatementOfTruthMapper statementOfTruthMapper;

    @Mock
    private PCSCase pcsCase;

    @Mock
    private StatementOfTruthDetails statementOfTruthDetails;

    @Nested
    class PcsCaseTests {
        @Test
        void shouldReturnNullWhenStatementOfTruthIsNull() {
            // Given
            when(pcsCase.getStatementOfTruth()).thenReturn(null);

            // When
            StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

            // Then
            assertThat(result).isNull();
        }

        @Test
        void shouldReturnNullWhenStatementOfTruthCompletedByIsNull() {
            // Given
            when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
            when(statementOfTruthDetails.getCompletedBy()).thenReturn(null);

            // When
            StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

            // Then
            assertThat(result).isNull();
        }

        @Test
        void shouldBuildStatementOfTruthForClaimant() {
            // Given
            when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
            when(statementOfTruthDetails.getCompletedBy()).thenReturn(CLAIMANT);
            when(statementOfTruthDetails.getAgreementClaimant())
                    .thenReturn(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE));
            when(statementOfTruthDetails.getFullNameParty()).thenReturn("John Smith");
            when(statementOfTruthDetails.getPositionParty()).thenReturn("Director");

            // When
            StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCompletedBy()).isEqualTo(CLAIMANT);
            assertThat(result.getAccepted()).isEqualTo(YesOrNo.YES);
            assertThat(result.getFullName()).isEqualTo("John Smith");
            assertThat(result.getPositionHeld()).isEqualTo("Director");
            assertThat(result.getFirmName()).isNull();
        }

        @Test
        void shouldBuildStatementOfTruthForLegalRepresentative() {
            // Given
            when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
            when(statementOfTruthDetails.getCompletedBy())
                    .thenReturn(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
            when(statementOfTruthDetails.getAgreementClaimantLegalRep())
                    .thenReturn(List.of(AgreementClaimantLegalRep.AGREED));
            when(statementOfTruthDetails.getFullNameLegalRep()).thenReturn("Jane Doe");
            when(statementOfTruthDetails.getFirmNameLegalRep()).thenReturn("Smith & Co Solicitors");
            when(statementOfTruthDetails.getPositionLegalRep()).thenReturn("Partner");

            // When
            StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
            assertThat(result.getAccepted()).isEqualTo(YesOrNo.YES);
            assertThat(result.getFullName()).isEqualTo("Jane Doe");
            assertThat(result.getFirmName()).isEqualTo("Smith & Co Solicitors");
            assertThat(result.getPositionHeld()).isEqualTo("Partner");
        }

        @Test
        void shouldNotSetAcceptedFlagWhenClaimantDidNotAgree() {
            // Given
            when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
            when(statementOfTruthDetails.getCompletedBy()).thenReturn(CLAIMANT);
            when(statementOfTruthDetails.getAgreementClaimant()).thenReturn(List.of());

            // When
            StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

            // Then
            assertThat(result.getAccepted()).isNull();
        }

        @Test
        void shouldNotSetAcceptedFlagWhenLegalRepDidNotAgree() {
            // Given
            when(pcsCase.getStatementOfTruth()).thenReturn(statementOfTruthDetails);
            when(statementOfTruthDetails.getCompletedBy()).thenReturn(LEGAL_REPRESENTATIVE);
            when(statementOfTruthDetails.getAgreementClaimantLegalRep()).thenReturn(List.of());

            // When
            StatementOfTruthEntity result = statementOfTruthService.createStatementOfTruthEntity(pcsCase);

            // Then
            assertThat(result.getAccepted()).isNull();
        }
    }

    @Nested
    class EnforcementTests {

        @Test
        void shouldProcessStatementOfTruth() {
            // Given
            StatementOfTruthDetailsEnforcement statementOfTruthDetailsEnforcement
                    = new StatementOfTruthDetailsEnforcement();
            EnforcementOrderEntity enforcementOrderEntity = new EnforcementOrderEntity();
            StatementOfTruthEntity statementOfTruthEntity = new StatementOfTruthEntity();
            StatementOfTruthEntity savedStatementOfTruthEntity = new StatementOfTruthEntity();

            when(statementOfTruthMapper.toEntity(statementOfTruthDetailsEnforcement))
                    .thenReturn(statementOfTruthEntity);
            when(statementOfTruthRepository.save(statementOfTruthEntity))
                    .thenReturn(savedStatementOfTruthEntity);

            // When
            statementOfTruthService.processStatementOfTruth(statementOfTruthDetailsEnforcement, enforcementOrderEntity);

            // Then
            assertThat(enforcementOrderEntity.getStatementOfTruth()).isEqualTo(savedStatementOfTruthEntity);
            verify(statementOfTruthRepository).save(statementOfTruthEntity);
        }
    }
}

