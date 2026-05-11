package uk.gov.hmcts.reform.pcs.testingsupport.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.service.PartyAccessCodeHashingService;
import uk.gov.hmcts.reform.pcs.testingsupport.endpoint.RegenerateAccessCodesResponse;
import uk.gov.hmcts.reform.pcs.testingsupport.endpoint.RegenerateAccessCodesResponse.DefendantPin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestingSupportAccessCodeServiceTest {

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private PartyAccessCodeRepository partyAccessCodeRepository;
    @Mock
    private PartyAccessCodeHashingService hashingService;

    @InjectMocks
    private TestingSupportAccessCodeService underTest;

    @Test
    void shouldReturnEmptyWhenCaseNotFound() {
        long caseReference = 1234567890123456L;
        when(pcsCaseRepository.findByCaseReference(caseReference)).thenReturn(Optional.empty());

        Optional<RegenerateAccessCodesResponse> result = underTest.regenerateAccessCodes(caseReference);

        assertThat(result).isEmpty();
        verify(partyAccessCodeRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldReturnEmptyPinsListWhenCaseHasNoAccessCodes() {
        long caseReference = 1234567890123456L;
        UUID caseId = UUID.randomUUID();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(caseId).caseReference(caseReference).build();

        when(pcsCaseRepository.findByCaseReference(caseReference)).thenReturn(Optional.of(pcsCase));
        when(partyAccessCodeRepository.findAllByPcsCase_Id(caseId)).thenReturn(List.of());

        Optional<RegenerateAccessCodesResponse> result = underTest.regenerateAccessCodes(caseReference);

        assertThat(result).isPresent();
        assertThat(result.get().pins()).isEmpty();
        verify(partyAccessCodeRepository).saveAll(List.of());
    }

    @Test
    void shouldAssignPinsInPartyIdSortedOrder() {
        long caseReference = 1234567890123456L;
        UUID caseId = UUID.randomUUID();
        UUID partyIdLast = UUID.fromString("ffffffff-0000-0000-0000-000000000000");
        UUID partyIdFirst = UUID.fromString("11111111-0000-0000-0000-000000000000");
        UUID partyIdMiddle = UUID.fromString("88888888-0000-0000-0000-000000000000");

        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(caseId).caseReference(caseReference).build();

        PartyAccessCodeEntity rowLast = PartyAccessCodeEntity.builder()
            .id(UUID.randomUUID()).partyId(partyIdLast).code("ORIGINAL_LAST").build();
        PartyAccessCodeEntity rowFirst = PartyAccessCodeEntity.builder()
            .id(UUID.randomUUID()).partyId(partyIdFirst).code("ORIGINAL_FIRST").build();
        PartyAccessCodeEntity rowMiddle = PartyAccessCodeEntity.builder()
            .id(UUID.randomUUID()).partyId(partyIdMiddle).code("ORIGINAL_MIDDLE").build();

        when(pcsCaseRepository.findByCaseReference(caseReference)).thenReturn(Optional.of(pcsCase));
        when(partyAccessCodeRepository.findAllByPcsCase_Id(caseId))
            .thenReturn(List.of(rowLast, rowFirst, rowMiddle));
        when(hashingService.encodeForStorage("DEFENDANTAAA")).thenReturn("encoded-aaa");
        when(hashingService.encodeForStorage("DEFENDANTAAB")).thenReturn("encoded-aab");
        when(hashingService.encodeForStorage("DEFENDANTAAC")).thenReturn("encoded-aac");

        Optional<RegenerateAccessCodesResponse> result = underTest.regenerateAccessCodes(caseReference);

        assertThat(result).isPresent();
        assertThat(result.get().pins())
            .containsExactly(
                new DefendantPin(partyIdFirst, "DEFENDANTAAA"),
                new DefendantPin(partyIdMiddle, "DEFENDANTAAB"),
                new DefendantPin(partyIdLast, "DEFENDANTAAC")
            );

        assertThat(rowFirst.getCode()).isEqualTo("encoded-aaa");
        assertThat(rowMiddle.getCode()).isEqualTo("encoded-aab");
        assertThat(rowLast.getCode()).isEqualTo("encoded-aac");
    }

    @Test
    void shouldPersistAllRowsAfterRegeneration() {
        long caseReference = 1234567890123456L;
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(caseId).caseReference(caseReference).build();
        PartyAccessCodeEntity row = PartyAccessCodeEntity.builder()
            .id(UUID.randomUUID()).partyId(partyId).code("ORIGINAL").build();

        when(pcsCaseRepository.findByCaseReference(caseReference)).thenReturn(Optional.of(pcsCase));
        when(partyAccessCodeRepository.findAllByPcsCase_Id(caseId)).thenReturn(List.of(row));
        when(hashingService.encodeForStorage("DEFENDANTAAA")).thenReturn("encoded-aaa");

        underTest.regenerateAccessCodes(caseReference);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PartyAccessCodeEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(partyAccessCodeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getCode()).isEqualTo("encoded-aaa");
    }

    @Test
    void shouldBeIdempotentAcrossMultipleCalls() {
        long caseReference = 1234567890123456L;
        UUID caseId = UUID.randomUUID();
        UUID partyIdA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000000");
        UUID partyIdB = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000000");

        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(caseId).caseReference(caseReference).build();

        when(pcsCaseRepository.findByCaseReference(caseReference)).thenReturn(Optional.of(pcsCase));
        when(partyAccessCodeRepository.findAllByPcsCase_Id(caseId))
            .thenReturn(List.of(
                PartyAccessCodeEntity.builder().partyId(partyIdA).build(),
                PartyAccessCodeEntity.builder().partyId(partyIdB).build()
            ))
            .thenReturn(List.of(
                PartyAccessCodeEntity.builder().partyId(partyIdA).build(),
                PartyAccessCodeEntity.builder().partyId(partyIdB).build()
            ));
        when(hashingService.encodeForStorage("DEFENDANTAAA")).thenReturn("encoded-aaa-a", "encoded-aaa-b");
        when(hashingService.encodeForStorage("DEFENDANTAAB")).thenReturn("encoded-aab-a", "encoded-aab-b");

        Optional<RegenerateAccessCodesResponse> firstCall = underTest.regenerateAccessCodes(caseReference);
        Optional<RegenerateAccessCodesResponse> secondCall = underTest.regenerateAccessCodes(caseReference);

        assertThat(firstCall).isPresent();
        assertThat(secondCall).isPresent();
        assertThat(firstCall.get().pins())
            .extracting(DefendantPin::partyId, DefendantPin::accessCode)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(partyIdA, "DEFENDANTAAA"),
                org.assertj.core.groups.Tuple.tuple(partyIdB, "DEFENDANTAAB")
            );
        assertThat(secondCall.get().pins()).isEqualTo(firstCall.get().pins());
    }

    @Test
    void shouldThrowWhenDefendantCountExceedsMax() {
        long caseReference = 1234567890123456L;
        UUID caseId = UUID.randomUUID();
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().id(caseId).caseReference(caseReference).build();

        List<PartyAccessCodeEntity> tooManyRows = IntStream.range(0, TestingSupportAccessCodeService.MAX_DEFENDANTS + 1)
            .mapToObj(i -> PartyAccessCodeEntity.builder().partyId(UUID.randomUUID()).build())
            .toList();

        when(pcsCaseRepository.findByCaseReference(caseReference)).thenReturn(Optional.of(pcsCase));
        when(partyAccessCodeRepository.findAllByPcsCase_Id(caseId)).thenReturn(tooManyRows);

        assertThatThrownBy(() -> underTest.regenerateAccessCodes(caseReference))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("max supported is " + TestingSupportAccessCodeService.MAX_DEFENDANTS);

        verify(partyAccessCodeRepository, never()).saveAll(anyList());
    }
}
