package uk.gov.hmcts.reform.pcs.ccd.event.citizen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CitizenCreateGenAppTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PartyService partyService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private GenAppRepository genAppRepository;

    @BeforeEach
    void setUp() {
        CitizenCreateGenApp underTest = new CitizenCreateGenApp(pcsCaseService, partyService,
                                                                securityContextService, genAppRepository);

        setEventUnderTest(underTest);
    }

    @Nested
    @DisplayName("Submit event tests")
    class SubmitTests {

        @Mock
        private PcsCaseEntity pcsCaseEntity;

        @BeforeEach
        void setUp() {
            given(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).willReturn(pcsCaseEntity);
        }

        @ParameterizedTest
        @EnumSource(value = GenAppType.class)
        void shouldSetGeneralApplicationType(GenAppType genAppType) {
            // Given
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(genAppType)
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            ArgumentCaptor<GenAppEntity> genAppEntityCaptor = ArgumentCaptor.forClass(GenAppEntity.class);
            verify(genAppRepository).save(genAppEntityCaptor.capture());

            assertThat(genAppEntityCaptor.getValue().getType()).isEqualTo(genAppType);
        }

        @Test
        void shouldSetInitialState() {
            // Given
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            ArgumentCaptor<GenAppEntity> genAppEntityCaptor = ArgumentCaptor.forClass(GenAppEntity.class);
            verify(genAppRepository).save(genAppEntityCaptor.capture());

            assertThat(genAppEntityCaptor.getValue().getState()).isEqualTo(GenAppState.SUBMITTED);
        }

        @Test
        void shouldSetApplicantParty() {
            // Given
            final PartyEntity applicantParty = mock(PartyEntity.class);

            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .applicationType(GenAppType.SUSPEND)
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            UUID currentUserId = UUID.randomUUID();
            given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
            given(partyService.getPartyEntityByIdamId(currentUserId, TEST_CASE_REFERENCE)).willReturn(applicantParty);

            // When
            callSubmitHandler(caseData);

            // Then
            ArgumentCaptor<GenAppEntity> genAppEntityCaptor = ArgumentCaptor.forClass(GenAppEntity.class);
            verify(genAppRepository).save(genAppEntityCaptor.capture());

            assertThat(genAppEntityCaptor.getValue().getParty()).isEqualTo(applicantParty);
        }

        @ParameterizedTest
        @EnumSource(YesOrNo.class)
        void shouldSetWith14DaysFlag(YesOrNo within14Days) {
            // Given
            CitizenGenAppRequest genAppRequest = CitizenGenAppRequest.builder()
                .within14Days(within14Days)
                .build();

            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(genAppRequest)
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            ArgumentCaptor<GenAppEntity> genAppEntityCaptor = ArgumentCaptor.forClass(GenAppEntity.class);
            verify(genAppRepository).save(genAppEntityCaptor.capture());

            assertThat(genAppEntityCaptor.getValue().getWithin14Days()).isEqualTo(within14Days);
        }

        @Test
        void shouldCreateGenAppAndAddToCaseEntity() {
            // Given
            PCSCase caseData = PCSCase.builder()
                .citizenGenAppRequest(CitizenGenAppRequest.builder().build())
                .build();

            // When
            callSubmitHandler(caseData);

            // Then
            ArgumentCaptor<GenAppEntity> genAppEntityCaptor = ArgumentCaptor.forClass(GenAppEntity.class);
            verify(genAppRepository).save(genAppEntityCaptor.capture());

            verify(pcsCaseEntity).addGenApp(same(genAppEntityCaptor.getValue()));
        }

    }
}
