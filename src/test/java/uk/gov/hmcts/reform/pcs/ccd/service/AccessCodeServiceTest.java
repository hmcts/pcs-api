package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessCodeServiceTest {

    @Mock
    private PartyAccessCodeRepository partyAccessCodeRepo;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    private AccessCodeService underTest;

    @Captor
    private ArgumentCaptor<Iterable<PartyAccessCodeEntity>> captor;

    @BeforeEach
    void setUp() {
        underTest = new AccessCodeService(partyAccessCodeRepo, pcsCaseRepository);
    }

    @Test
    void shouldCreatePartyAccessCodeEntity() {
        // Given
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);

        // When
        PartyAccessCodeEntity createdEntity = underTest.createPartyAccessCodeEntity(caseEntity, partyId);

        // Then
        assertThat(createdEntity.getPartyId()).isEqualTo(partyId);
        assertThat(createdEntity.getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(createdEntity.getCode()).isNotNull().hasSize(12);
        assertThat(createdEntity.getCode()).matches("[ABCDEFGHJKLMNPRSTVWXYZ23456789]{12}");
    }

    @Test
    void shouldCreateAccessCodeForDefendantWithoutExistingCode() {
        // Given
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(UUID.randomUUID());
        caseEntity.setDefendants(List.of(Defendant.builder().partyId(partyId).build()));

        when(pcsCaseRepository.findByCaseReference(1L)).thenReturn(Optional.of(caseEntity));
        when(partyAccessCodeRepo.findAllByPcsCase_Id(caseEntity.getId())).thenReturn(List.of());

        // When
        underTest.createAccessCodesForParties("1");

        // Then
        verify(partyAccessCodeRepo).saveAll(captor.capture());

        List<PartyAccessCodeEntity> savedEntities = new ArrayList<>();
        captor.getValue().forEach(savedEntities::add);

        assertThat(savedEntities).hasSize(1);

        PartyAccessCodeEntity savedEntity = savedEntities.get(0);
        assertThat(savedEntity.getPartyId()).isEqualTo(partyId);
        assertThat(savedEntity.getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(savedEntity.getCode()).isNotNull()
            .hasSize(12)
            .matches("[ABCDEFGHJKLMNPRSTVWXYZ23456789]{12}");
    }

    @Test
    void shouldThrowExceptionIfCaseNotFound() {
        // Given
        when(pcsCaseRepository.findByCaseReference(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CaseNotFoundException.class,
                     () -> underTest.createAccessCodesForParties("999"));
    }

    @Test
    void shouldNotCreateAccessCodeIfDefendantAlreadyHasCode() {
        // Given
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(UUID.randomUUID());
        caseEntity.setDefendants(List.of(Defendant.builder().partyId(partyId).build()));
        PartyAccessCodeEntity existingCodeEntity = mock(PartyAccessCodeEntity.class);

        when(pcsCaseRepository.findByCaseReference(2L)).thenReturn(Optional.of(caseEntity));
        when(partyAccessCodeRepo.findAllByPcsCase_Id(caseEntity.getId()))
            .thenReturn(List.of(existingCodeEntity));
        when(existingCodeEntity.getPartyId()).thenReturn(partyId);

        // When
        underTest.createAccessCodesForParties("2");

        // Then
        verify(partyAccessCodeRepo, never()).saveAll(any());
    }
}

