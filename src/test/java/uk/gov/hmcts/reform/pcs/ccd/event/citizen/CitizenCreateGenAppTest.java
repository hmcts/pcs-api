package uk.gov.hmcts.reform.pcs.ccd.event.citizen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.CitizenGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

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
    private GenAppService genAppService;

    @BeforeEach
    void setUp() {
        CitizenCreateGenApp underTest = new CitizenCreateGenApp(pcsCaseService, partyService,
                                                                securityContextService, genAppService);

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

        @Test
        void shouldCreateGenAppWithCaseDataAndApplicantParty() {
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
            verify(genAppService).createGenAppEntity(caseData, pcsCaseEntity, applicantParty);
        }

    }
}
