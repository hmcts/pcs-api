package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CaseFlagsViewTest {

    private final CaseFlagsView caseFlagsView = new CaseFlagsView();

    @Test
    void shouldMapBasicCaseFlagFieldsWhenCaseFlagsExist() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        FlagsEntity flagsEntity = new FlagsEntity();
        flagsEntity.setFlagDetails(List.of(createMockFlagsEntity()));

        pcsCaseEntity.setCaseFlags(List.of(flagsEntity));

        // When
        caseFlagsView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNotNull(pcsCase.getCaseFlags());
        assertEquals(1, pcsCase.getCaseFlags().getDetails().size());
        assertEquals("FLAG_CODE", pcsCase.getCaseFlags().getDetails().get(0).getValue().getFlagCode());
    }

    @Test
    void shouldHandleNullCaseFlagsGracefully() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        caseFlagsView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        Assertions.assertNull(pcsCase.getCaseFlags());
    }

    private FlagDetailsEntity createMockFlagsEntity() {

        return FlagDetailsEntity.builder()
            .id(UUID.randomUUID())
            .flagCode("FLAG_CODE")
            .flagComment("FLAG_COMMENT")
            .build();
    }
}

