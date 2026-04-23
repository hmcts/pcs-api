package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.RefDataFlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.RefDataFlagsRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseFlagsViewTest {

    @Mock
    private RefDataFlagsRepository refDataFlagsRepository;

    @InjectMocks
    private CaseFlagsView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseFlagsView(refDataFlagsRepository);
    }

    @Test
    void shouldMapBasicCaseFlagFieldsWhenCaseFlagsAreNull() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNotNull(pcsCase.getCaseFlags());
        assertNotNull(pcsCase.getCaseFlags());
        assertNull(pcsCase.getCaseFlags().getDetails());

    }

    @Test
    void shouldMapBasicCaseFlagFieldsWhenCaseFlagsExist() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        pcsCaseEntity.setCaseFlags(List.of(createMockFlagsEntity()));

        when(refDataFlagsRepository.findByFlagCode(any())).thenReturn(Optional.of(createMockRefDataFlagsEntity()));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

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
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNull(pcsCase.getCaseFlags().getDetails());
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

    private RefDataFlagsEntity createMockRefDataFlagsEntity() {

        return RefDataFlagsEntity.builder()
            .flagCode("CF0007")
            .flagName("Urgent case")
            .build();
    }
}

