package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventAction;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventContext;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventResult;
import uk.gov.hmcts.ccd.sdk.SystemCaseEventService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessCodeGenerationServiceTest {

    @Mock
    private DefendantAccessCodeService defendantAccessCodeService;
    @Mock
    private SystemCaseEventService systemCaseEventService;

    private AccessCodeGenerationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AccessCodeGenerationService(defendantAccessCodeService, systemCaseEventService);
        lenient().doAnswer(invocation -> {
            SystemCaseEventAction<Object, Object> action = invocation.getArgument(3);
            action.execute(new SystemCaseEventContext<>(1L, null, null));
            return new SystemCaseEventResult(1L, 1L, false);
        }).when(systemCaseEventService).submit(anyLong(), any(), any(), any());
    }

    @Test
    void generatesForEachDefendantNeedingAccessCode() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(1L)).thenReturn(List.of(p1, p2));

        underTest.createAccessCodesForParties("1", true);

        verify(defendantAccessCodeService).generateForDefendant(1L, p1, true, true);
        verify(defendantAccessCodeService).generateForDefendant(1L, p2, true, true);
    }

    @Test
    void passesFinalAttemptFlagThrough() {
        UUID p1 = UUID.randomUUID();
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(1L)).thenReturn(List.of(p1));

        underTest.createAccessCodesForParties("1", false);

        verify(defendantAccessCodeService).generateForDefendant(1L, p1, true, false);
    }

    @Test
    void continuesAfterOneDefendantFailsThenFailsTheBatch() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(1L)).thenReturn(List.of(p1, p2));
        doThrow(new RuntimeException("docmosis down"))
            .when(defendantAccessCodeService).generateForDefendant(1L, p1, true, true);

        assertThrows(IllegalStateException.class, () -> underTest.createAccessCodesForParties("1", true));

        verify(defendantAccessCodeService).generateForDefendant(1L, p1, true, true);
        verify(defendantAccessCodeService).generateForDefendant(1L, p2, true, true);
    }

    @Test
    void doesNothingWhenNoDefendantsNeedAccessCode() {
        when(defendantAccessCodeService.findDefendantPartyIdsNeedingAccessCode(1L)).thenReturn(List.of());

        underTest.createAccessCodesForParties("1", true);

        verify(defendantAccessCodeService, never()).generateForDefendant(anyLong(), any(), anyBoolean(), anyBoolean());
    }
}
