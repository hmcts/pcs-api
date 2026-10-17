package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.BailiffNoteEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.repository.BailiffNoteRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BailiffNoteServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final Instant NOW = Instant.parse("2026-02-01T16:20:00Z");

    @Mock
    private CurrentEnforcementService currentEnforcementService;
    @Mock
    private BailiffNoteRepository bailiffNoteRepository;
    @Mock
    private SecurityContextService securityContextService;

    private BailiffNoteService underTest;

    @BeforeEach
    void setUp() {
        underTest = new BailiffNoteService(currentEnforcementService, bailiffNoteRepository, securityContextService,
                                           Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldSaveTheNoteAgainstTheCurrentEnforcementWithTheAuthorsIdamName() {
        EnforcementOrderEntity enforcement = enforcement();
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement));
        when(securityContextService.getCurrentUserDetails())
            .thenReturn(UserInfo.builder().givenName("Sam").familyName("Jones").name("Sam Jones").build());

        underTest.addNote(CASE_REFERENCE, "Rear gate is locked");

        ArgumentCaptor<BailiffNoteEntity> saved = ArgumentCaptor.forClass(BailiffNoteEntity.class);
        verify(bailiffNoteRepository).save(saved.capture());
        assertThat(saved.getValue().getEnforcementOrder()).isSameAs(enforcement);
        assertThat(saved.getValue().getNote()).isEqualTo("Rear gate is locked");
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getAuthorFirstName()).isEqualTo("Sam");
        assertThat(saved.getValue().getAuthorLastName()).isEqualTo("Jones");
    }

    @Test
    void shouldRefuseToSaveWhenTheClaimHasNoEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.addNote(CASE_REFERENCE, "note"))
            .isInstanceOf(NoEnforcementException.class);
        verify(bailiffNoteRepository, never()).save(any());
        verifyNoInteractions(securityContextService);
    }

    @Test
    void shouldReportWhetherTheClaimHasAnEnforcement() {
        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.of(enforcement()));
        assertThat(underTest.hasEnforcement(CASE_REFERENCE)).isTrue();

        when(currentEnforcementService.findCurrentEnforcement(CASE_REFERENCE)).thenReturn(Optional.empty());
        assertThat(underTest.hasEnforcement(CASE_REFERENCE)).isFalse();
    }

    @Test
    void shouldReturnTheCurrentEnforcementsNotesNewestFirst() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        EnforcementOrderEntity enforcement = enforcement();
        List<BailiffNoteEntity> notes = List.of(new BailiffNoteEntity(), new BailiffNoteEntity());
        when(currentEnforcementService.findCurrentEnforcement(pcsCase)).thenReturn(Optional.of(enforcement));
        when(bailiffNoteRepository.findByEnforcementOrderIdOrderByCreatedAtDesc(enforcement.getId()))
            .thenReturn(notes);

        assertThat(underTest.getCurrentEnforcementNotes(pcsCase)).isEqualTo(notes);
    }

    @Test
    void shouldReturnNoNotesWhenTheClaimHasNoEnforcement() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        when(currentEnforcementService.findCurrentEnforcement(pcsCase)).thenReturn(Optional.empty());

        assertThat(underTest.getCurrentEnforcementNotes(pcsCase)).isEmpty();
        verifyNoInteractions(bailiffNoteRepository);
    }

    private static EnforcementOrderEntity enforcement() {
        EnforcementOrderEntity enforcement = new EnforcementOrderEntity();
        enforcement.setId(UUID.randomUUID());
        return enforcement;
    }
}
