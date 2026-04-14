package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
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

        pcsCaseEntity.setCaseFlags(List.of(createMockFlagsEntity()));

        // When
        caseFlagsView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNotNull(pcsCase.getCaseFlags());
        assertEquals(1, pcsCase.getCaseFlags().getDetails().size());
        assertEquals("CF0007", pcsCase.getCaseFlags().getDetails().get(0).getValue().getFlagCode());
    }

    @Test
    void shouldHandleNullCaseFlagsGracefully() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        caseFlagsView.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        Assertions.assertNull(pcsCase.getCaseFlags().getDetails());
    }

    private FlagDetailsEntity createMockFlagsEntity() {

        return FlagDetailsEntity.builder()
            .id(UUID.randomUUID())
            .flagCode("CF0007")
            .flagComment("Urgent case")
            .paths(List.of(createMockFlagPathEntity()))
            .build();
    }

    private FlagPathEntity createMockFlagPathEntity() {
        return FlagPathEntity.builder()
            .id(UUID.randomUUID())
            .path("Case")
            .build();
    }
}

