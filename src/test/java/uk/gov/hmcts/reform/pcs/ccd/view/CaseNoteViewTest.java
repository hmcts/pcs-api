package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseNote;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseNoteEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CaseNoteViewTest {

    private CaseNoteView caseNoteView;

    @BeforeEach
    void setUp() {
        caseNoteView = new CaseNoteView();
    }

    @Test
    void shouldMapCaseNoteEntityToCaseNoteInPCSCase() {
        // Given
        String note = "Note";
        String name = "Name";
        LocalDateTime localDateTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        CaseNoteEntity caseNoteEntity = CaseNoteEntity.builder()
            .note(note)
            .createdBy(name)
            .createdOn(localDateTime)
            .build();
        List<CaseNoteEntity> caseNoteEntities = new ArrayList<>();
        caseNoteEntities.add(caseNoteEntity);
        ClaimEntity claimEntity = ClaimEntity.builder()
            .caseNotes(caseNoteEntities)
            .build();
        List<ClaimEntity> claimEntities = new ArrayList<>();
        claimEntities.add(claimEntity);
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .claims(claimEntities)
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        // When
        caseNoteView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        List<ListValue<CaseNote>> caseNotes = pcsCase.getCaseNotes();
        assertThat(caseNotes.size()).isEqualTo(1);

        CaseNote caseNote = caseNotes.getFirst().getValue();
        assertThat(caseNote.getNote()).isEqualTo(note);
        assertThat(caseNote.getCreatedBy()).isEqualTo(name);
        assertThat(caseNote.getCreatedOn()).isEqualTo(localDateTime);
    }
}
