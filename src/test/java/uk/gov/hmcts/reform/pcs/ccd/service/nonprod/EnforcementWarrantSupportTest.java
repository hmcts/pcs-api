package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcement.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.factory.ClaimantPartyFactory;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementWarrantSupportTest {

    private EnforcementWarrantSupport underTest;

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ClaimantPartyFactory claimantPartyFactory;
    @Mock(strictness = LENIENT)
    private SecurityContextService securityContextService;
    @Mock
    private ClaimService claimService;
    @Mock
    private CaseSupportHelper caseSupportHelper;
    @Mock
    private EnforcementOrderService enforcementOrderService;
    @Mock(strictness = LENIENT)
    private UserInfo userDetails;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void beforeEach() {
        when(securityContextService.getCurrentUserDetails()).thenReturn(userDetails);
        when(userDetails.getUid()).thenReturn(UUID.randomUUID().toString());
        when(userDetails.getSub()).thenReturn("test@example.com");

        underTest = new EnforcementWarrantSupport(draftCaseDataService, pcsCaseService, claimantPartyFactory,
                                                  securityContextService, claimService, caseSupportHelper,
                                                  enforcementOrderService, objectMapper);
    }

    @Test
    void shouldGenerateEnforcementWarrantCase() throws Exception {
        // Given
        long caseReference = 123456L;
        PCSCase caseData = Instancio.create(PCSCase.class);
        Resource resource = mock(Resource.class);

        String caseDataJson = objectMapper.writeValueAsString(caseData);
        String enforcementDataJson = objectMapper.writeValueAsString(caseData.getEnforcementOrder());

        ByteArrayInputStream stream1 = new ByteArrayInputStream(caseDataJson.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream stream2 = new ByteArrayInputStream(enforcementDataJson.getBytes(StandardCharsets.UTF_8));
        when(resource.getInputStream()).thenReturn(stream1).thenReturn(stream2);
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        PartyEntity claimantPartyEntity = mock(PartyEntity.class);
        ClaimEntity claimEntity = mock(ClaimEntity.class);
        when(caseSupportHelper.getNonProdResource(MakeAClaimCaseGenerationSupport.CASE_GENERATOR)).thenReturn(resource);
        when(draftCaseDataService.parseCaseDataJson(caseDataJson)).thenReturn(caseData);
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity);
        when(claimantPartyFactory.createAndPersistClaimantParty(any(), any())).thenReturn(claimantPartyEntity);
        when(claimService.createMainClaimEntity(caseData, claimantPartyEntity)).thenReturn(claimEntity);

        // When
        underTest.generate(caseReference, caseData, resource);

        // Then
        verify(pcsCaseService).createCase(anyLong(), any(), any());
        verify(enforcementOrderService).saveAndClearDraftData(anyLong(), any(EnforcementOrder.class));
    }

    @Test
    void shouldReturnCaseGeneratorLabelWhenGetLabelMethodIsCalled() {
        // When
        String result = underTest.getLabel();

        // Then
        assertThat(result).isEqualTo(EnforcementWarrantSupport.CASE_GENERATOR);
    }

    @Test
    void shouldReturnTrueWhenSupports() {
        // Given
        String label = EnforcementWarrantSupport.CASE_GENERATOR;

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
    void shouldParseCaseDataJson() throws IOException {
        // Given
        CaseSupportHelper helper = new CaseSupportHelper(null);
        String fileName = helper.generateNameFromLabel(EnforcementWarrantSupport.CASE_GENERATOR) + ".json";
        String jsonContent = StreamUtils.copyToString(
            getClass().getClassLoader().getResourceAsStream("nonprod/" + fileName),
            StandardCharsets.UTF_8
        );

        // When
        EnforcementOrder result = underTest.parseCaseDataJson(jsonContent);

        // Then
        assertThat(result).isNotNull();
    }

}
