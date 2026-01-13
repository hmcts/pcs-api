package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MakeAClaimCaseGenerationSupportTest {

    @InjectMocks
    private MakeAClaimCaseGenerationSupport underTest;

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock(strictness = LENIENT)
    private SecurityContextService securityContextService;
    @Mock(strictness = LENIENT)
    private UserInfo userDetails;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void beforeEach() {
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(userDetails.getUid()).thenReturn(UUID.randomUUID().toString());
        when(userDetails.getSub()).thenReturn("test@example.com");
    }

    @Test
    void shouldSuccessfullyGenerateMakeAClaimCaseWhenValidInputsProvided() throws Exception {
        // Given
        long caseReference = 123456L;
        PCSCase caseData = Instancio.create(PCSCase.class);
        Resource nonProdResource = mock(Resource.class);

        String jsonString = objectMapper.writeValueAsString(caseData);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);

        when(nonProdResource.getInputStream()).thenReturn(
            new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8))
        );
        when(draftCaseDataService.parseCaseDataJson(jsonString)).thenReturn(caseData);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);

        // When
        CaseSupportGenerationResponse result = underTest.generate(caseReference, caseData, nonProdResource);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.state()).isEqualTo(State.PENDING_CASE_ISSUED);
        assertThat(result.errors()).isEmpty();
        verify(draftCaseDataService).parseCaseDataJson(jsonString);
        verify(pcsCaseService).createCase(eq(caseReference), any(), any());
        verify(pcsCaseService).loadCase(caseReference);
        verify(pcsCaseService).mergeCaseData(pcsCaseEntity, caseData);
        verify(securityContextService).getCurrentUserDetails();
        verify(pcsCaseService).addClaimantPartyAndClaim(any(PcsCaseEntity.class), eq(caseData), eq(userDetails));
    }

    @Test
    void shouldThrowSupportExceptionWhenResourceInputStreamCannotBeRead() throws Exception {
        // Given
        long caseReference = 999L;
        PCSCase caseData = mock(PCSCase.class);
        Resource nonProdResource = mock(Resource.class);
        when(nonProdResource.getInputStream()).thenThrow(new IOException("Cannot read input stream"));

        // When / Then
        assertThatThrownBy(() -> underTest.generate(caseReference, caseData, nonProdResource))
            .isInstanceOf(TestCaseSupportException.class)
            .hasCauseInstanceOf(IOException.class)
            .hasRootCauseMessage("Cannot read input stream");

        verify(nonProdResource).getInputStream();
        verify(draftCaseDataService, never()).parseCaseDataJson(any());
        verify(pcsCaseService, never()).createCase(anyLong(), any(), any());
    }

    @Test
    void shouldReturnTrueWhenSupports() {
        // Given
        String label = MakeAClaimCaseGenerationSupport.CASE_GENERATOR;

        // When
        boolean result = underTest.supports(label);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSupportsMethodIsCalledWithDifferentLabel() {
        // Given
        String label = "Different Label";

        // When
        boolean result = underTest.supports(label);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnCaseGeneratorLabelWhenGetLabelMethodIsCalled() {
        // When
        String result = underTest.getLabel();

        // Then
        assertThat(result).isEqualTo(MakeAClaimCaseGenerationSupport.CASE_GENERATOR);
    }

}

