package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitDefendantResponseTest extends BaseEventTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private SecurityContextService securityContextService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new SubmitDefendantResponse(draftCaseDataService, pcsCaseService, securityContextService));
    }

    @Test
    void shouldPatchUnsubmittedEventData() {
        // Given Defendant response
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        callSubmitHandler(caseData);

        //Then - Verify filtered PCSCase (only defendantResponse) is saved
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.submitDefendantResponse)
        );

    }

    @Test
    void shouldPopulateDefendantResponseWhenUserIsMatchingDefendant() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        AddressUK expectedAddress = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        Defendant matchingDefendant = Defendant.builder()
            .idamUserId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .correspondenceAddress(expectedAddress)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .defendants(List.of(matchingDefendant))
            .build();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder().build();

        // When
        PCSCase result = callStartHandler(caseData);

        // Then
        assertThat(result.getDefendantResponse()).isNotNull();
        assertThat(result.getDefendantResponse().getParty()).isNotNull();
        assertThat(result.getDefendantResponse().getParty().getForename()).isEqualTo("John");
        assertThat(result.getDefendantResponse().getParty().getSurname()).isEqualTo("Doe");
        assertThat(result.getDefendantResponse().getParty().getContactAddress()).isEqualTo(expectedAddress);

        // Verify draft is created in start() with filtered PCSCase (only defendantResponse)
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.submitDefendantResponse)
        );
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoDefendantsFound() {
        // Given
        UUID defendantUserId = UUID.randomUUID();
        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .defendants(Collections.emptyList())
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder().build();

        // When/Then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No defendants associated with this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenDefendantsIsNull() {
        // Given
        UUID defendantUserId = UUID.randomUUID();
        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .defendants(null)
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder().build();

        // When/Then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No defendants associated with this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenUserIsNotDefendant() {
        // Given
        UUID defendantUserId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        UserInfo userInfo = UserInfo.builder()
            .uid(differentUserId.toString())
            .build();

        Defendant matchingDefendant = Defendant.builder()
            .idamUserId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .defendants(List.of(matchingDefendant))
            .build();

        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        PCSCase caseData = PCSCase.builder().build();

        // When/Then
        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
    }

    @Test
    void shouldNotSaveDraftWhenSubmitDraftIsYes() {
        // Given Defendant response
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        // When
        callSubmitHandler(caseData);

        //Then
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(PCSCase.class),
            eq(EventId.submitDefendantResponse)
        );
    }

    @Test
    void shouldNotSaveDraftWhenDefendantResponseIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .defendantResponse(null)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        callSubmitHandler(caseData);

        //Then
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.submitDefendantResponse)
        );
    }

    @Test
    void shouldNotSaveDraftWhenSubmitDraftIsNull() {
        // Given Defendant response
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(null)
            .build();

        // When
        callSubmitHandler(caseData);

        //Then
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE),
            any(),
            eq(EventId.submitDefendantResponse)
        );
    }

}
