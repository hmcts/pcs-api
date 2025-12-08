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
    private ArgumentCaptor<PartyAccessCodeEntity> argumentCaptor;

    @BeforeEach
    void setUp() {
        underTest = new AccessCodeService(partyAccessCodeRepo, pcsCaseRepository);
    }

    @Test
    void shouldCreatePartyAccessCodeEntity() {
        //Given
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);

        //When
        underTest.createPartyAccessCodeEntity(caseEntity, partyId);

        //Then
        verify(partyAccessCodeRepo).save(argumentCaptor.capture());
        PartyAccessCodeEntity savedEntity = argumentCaptor.getValue();

        assertThat(savedEntity.getPartyId()).isEqualTo(partyId);
        assertThat(savedEntity.getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(savedEntity.getCode()).isNotNull().hasSize(12);
        assertThat(savedEntity.getCode()).matches("[ABCDEFGHJKLMNPRSTVWXYZ23456789]{12}");
    }

    @Test
    void shouldCreateAccessCodeForDefendantWithoutExistingCode() {
        //Given
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(UUID.randomUUID());
        caseEntity.setDefendants(List.of(Defendant.builder().partyId(partyId).build()));

        when(pcsCaseRepository.findByCaseReference(1L)).thenReturn(Optional.of(caseEntity));
        when(partyAccessCodeRepo.findByPcsCase_IdAndPartyId(caseEntity.getId(), partyId))
            .thenReturn(Optional.empty());

        //When
        underTest.createAccessCodesForDefendants("1");

        //Then
        verify(partyAccessCodeRepo).save(argumentCaptor.capture());
        PartyAccessCodeEntity saved = argumentCaptor.getValue();

        assertThat(saved.getPartyId()).isEqualTo(partyId);
        assertThat(saved.getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(saved.getCode()).isNotNull()
            .hasSize(12)
            .matches("[ABCDEFGHJKLMNPRSTVWXYZ23456789]{12}");
    }

    @Test
    void shouldThrowExceptionIfCaseNotFound() {
        //Given
        when(pcsCaseRepository.findByCaseReference(999L)).thenReturn(Optional.empty());

        //When & then
        assertThrows(
            CaseNotFoundException.class,
            () -> underTest.createAccessCodesForDefendants("999"));
    }

    @Test
    void shouldNotCreateAccessCodeIfDefendantAlreadyHasCode() {
        //Given
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(UUID.randomUUID());
        caseEntity.setDefendants(List.of(Defendant.builder().partyId(partyId).build()));

        when(pcsCaseRepository.findByCaseReference(2L)).thenReturn(Optional.of(caseEntity));
        when(partyAccessCodeRepo.findByPcsCase_IdAndPartyId(caseEntity.getId(), partyId))
            .thenReturn(Optional.of(new PartyAccessCodeEntity()));

        //When
        underTest.createAccessCodesForDefendants("2");

        //Then
        verify(partyAccessCodeRepo, never()).save(any());
    }

}
