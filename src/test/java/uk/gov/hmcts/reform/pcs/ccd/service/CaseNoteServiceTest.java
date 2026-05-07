package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@ExtendWith(MockitoExtension.class)
public class CaseNoteServiceTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private Clock ukClock;

    @InjectMocks
    private CaseNoteService caseNoteService;

    private static final LocalDate FIXED_CURRENT_DATE = LocalDate.of(2025, 8, 27);

    @BeforeEach
    void setUp() {
        when(ukClock.instant()).thenReturn(FIXED_CURRENT_DATE.atTime(10, 20).atZone(UK_ZONE_ID).toInstant());
        when(ukClock.getZone()).thenReturn(UK_ZONE_ID);
    }

    @Test
    void shouldSaveNewCaseNoteWithNoPreexistingCaseNotes() {
        // Given
        ClaimEntity claimEntity = ClaimEntity.builder().build();
        List<ClaimEntity> claimEntities = new ArrayList<>();
        claimEntities.add(claimEntity);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity
            .builder()
            .claims(claimEntities)
            .build();

        String note = "Note";
        String name = "Name";

        UserInfo userInfo = UserInfo.builder()
            .name(name)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .note(note)
            .build();

        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        caseNoteService.addCaseNote(caseReference, pcsCase);

        // Then
        verify(claimRepository).save(claimEntity);
        assertThat(claimEntity.getCaseNotes().size()).isEqualTo(1);
        CaseNoteEntity caseNoteEntity = claimEntity.getCaseNotes().getFirst();
        assertThat(caseNoteEntity.getNote()).isEqualTo(note);
        assertThat(caseNoteEntity.getCreatedBy()).isEqualTo(name);
        assertThat(caseNoteEntity.getCreatedOn())
            .isEqualTo(LocalDateTime.of(2025, 8, 27, 10, 20));
    }

    @Test
    void shouldSaveNewCaseNoteWithPreexistingCaseNotes() {
        // Given
        String note1 = "Note 1";
        String name1 = "Name 1";
        LocalDateTime localDateTime1 = LocalDateTime.of(2025, 1, 1, 0, 0,0);
        CaseNoteEntity preExistingCaseNote = CaseNoteEntity.builder()
            .note(note1)
            .createdBy(name1)
            .createdOn(localDateTime1)
            .build();
        List<CaseNoteEntity> caseNoteEntities = new ArrayList<>();
        caseNoteEntities.add(preExistingCaseNote);
        ClaimEntity claimEntity = ClaimEntity.builder()
            .caseNotes(caseNoteEntities)
            .build();
        List<ClaimEntity> claimEntities = new ArrayList<>();
        claimEntities.add(claimEntity);

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity
            .builder()
            .claims(claimEntities)
            .build();

        String note2 = "Note 2";
        String name2 = "Name 2";

        UserInfo userInfo = UserInfo.builder()
            .name(name2)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .note(note2)
            .build();

        long caseReference = 12345L;
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        // When
        caseNoteService.addCaseNote(caseReference, pcsCase);

        // Then
        verify(claimRepository).save(claimEntity);
        assertThat(claimEntity.getCaseNotes().size()).isEqualTo(2);
        CaseNoteEntity caseNoteEntity1 = claimEntity.getCaseNotes().getFirst();
        assertThat(caseNoteEntity1.getNote()).isEqualTo(note1);
        assertThat(caseNoteEntity1.getCreatedBy()).isEqualTo(name1);
        assertThat(caseNoteEntity1.getCreatedOn()).isEqualTo(localDateTime1);

        CaseNoteEntity caseNoteEntity2 = claimEntity.getCaseNotes().getLast();
        assertThat(caseNoteEntity2.getNote()).isEqualTo(note2);
        assertThat(caseNoteEntity2.getCreatedBy()).isEqualTo(name2);
        assertThat(caseNoteEntity2.getCreatedOn())
            .isEqualTo(LocalDateTime.of(2025, 8, 27, 10, 20));
    }
}
