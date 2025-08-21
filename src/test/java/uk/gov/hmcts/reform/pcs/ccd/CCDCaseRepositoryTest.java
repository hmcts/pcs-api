package uk.gov.hmcts.reform.pcs.ccd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimHistoryRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimListRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimPaymentTabRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.GenAppHistoryRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.GenAppListRenderer;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.roles.api.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CCDCaseRepositoryTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private ClaimPaymentTabRenderer claimPaymentTabRenderer;
    @Mock
    private ClaimListRenderer claimListRenderer;
    @Mock
    private ClaimHistoryRenderer claimHistoryRenderer;
    @Mock
    private GenAppListRenderer genAppListRenderer;
    @Mock
    private ClaimService claimService;
    @Mock
    private GenAppService genAppService;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private UserInfoService userInfoService;
    @Mock
    private GenAppHistoryRenderer genAppHistoryRenderer;

    private CCDCaseRepository underTest;

    @BeforeEach
    void setUp() {

        when(userInfoService.getCurrentUserInfo()).thenReturn(mock(UserInfo.class));

        underTest = new CCDCaseRepository(pcsCaseRepository, securityContextService, modelMapper,
                                          claimPaymentTabRenderer, claimListRenderer, claimHistoryRenderer,
                                          genAppListRenderer, claimService, genAppService, userInfoService,
                                          genAppHistoryRenderer);

    }

    @Test
    void shouldThrowExceptionForUnknownCaseReference() {
        // Given
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        Throwable throwable = catchThrowable(() -> underTest.getCase(CASE_REFERENCE));

        // Then
        assertThat(throwable)
                .isInstanceOf(CaseNotFoundException.class)
                .hasMessage("No case found with reference %s", CASE_REFERENCE);
    }

    @Test
    void shouldReturnCaseWithNoPropertyAddress() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getPaymentStatus()).thenReturn(PaymentStatus.UNPAID);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getPropertyAddress()).isNull();
    }

    @Test
    void shouldRenderClaimPaymentMarkdownWhenPaymentStatusIsNotNull() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getPaymentStatus()).thenReturn(PaymentStatus.UNPAID);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        String expectedPaymentMarkdown = "payment markdown";
        when(claimPaymentTabRenderer.render(CASE_REFERENCE, PaymentStatus.UNPAID)).thenReturn(expectedPaymentMarkdown);

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getClaimPaymentTabMarkdown()).isEqualTo(expectedPaymentMarkdown);
    }

    @Test
    void shouldNotRenderClaimPaymentMarkdownWhenNoPaymentStatus() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getPaymentStatus()).thenReturn(null); // Stated for clarity

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getClaimPaymentTabMarkdown()).isNull();
        verify(claimPaymentTabRenderer, never()).render(anyLong(), any());
    }

    @Test
    void shouldMapPropertyAddress() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(pcsCaseEntity.getPropertyAddress()).thenReturn(addressEntity);
        when(pcsCaseEntity.getPaymentStatus()).thenReturn(PaymentStatus.UNPAID);
        AddressUK addressUK = stubAddressEntityModelMapper(addressEntity);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getPropertyAddress()).isEqualTo(addressUK);
    }

    @Test
    void shouldMapPartyEntity() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        PartyEntity partyEntity = mock(PartyEntity.class);
        when(pcsCaseEntity.getParties()).thenReturn(Set.of(partyEntity));

        Party party = mock(Party.class);

        when(modelMapper.map(partyEntity, Party.class)).thenReturn(party);
        when(pcsCaseEntity.getPaymentStatus()).thenReturn(PaymentStatus.UNPAID);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE);

        // Then
        List<ListValue<Party>> mappedParties = pcsCase.getParties();
        assertThat(mappedParties).hasSize(1);
        assertThat(mappedParties.get(0).getValue()).isSameAs(party);
    }

    @Test
    void shouldMapPreActionProtocolCompletedWhenYes() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getPreActionProtocolCompleted()).thenReturn(true);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldMapPreActionProtocolCompletedWhenNo() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getPreActionProtocolCompleted()).thenReturn(false);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldMapPreActionProtocolCompletedAsNullWhenNull() {
        // Given
        PcsCaseEntity pcsCaseEntity = mock(PcsCaseEntity.class);
        when(pcsCaseEntity.getPreActionProtocolCompleted()).thenReturn(null);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // When
        PCSCase pcsCase = underTest.getCase(CASE_REFERENCE);

        // Then
        assertThat(pcsCase.getPreActionProtocolCompleted()).isNull();
    }

    private AddressUK stubAddressEntityModelMapper(AddressEntity addressEntity) {
        AddressUK addressUK = mock(AddressUK.class);
        when(modelMapper.map(addressEntity, AddressUK.class)).thenReturn(addressUK);
        return addressUK;
    }

}
