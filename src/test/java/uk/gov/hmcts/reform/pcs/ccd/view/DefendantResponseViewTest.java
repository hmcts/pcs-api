package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantResponseViewTest {

    private static final long CASE_REFERENCE = 555L;

    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private DefendantAccessValidator accessValidator;
    @Mock
    private DefendantResponseService defendantResponseService;
    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private DefendantResponseView underTest;

    @Test
    void shouldSetClaimantServiceAddressOnResponseForCitizenWithSubmittedResponse() {
        UUID userId = UUID.randomUUID();
        stubCitizenUser(userId);

        PcsCaseEntity caseEntity = buildCaseEntityWithClaimantAddress();
        when(caseEntity.getCaseReference()).thenReturn(CASE_REFERENCE);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(true);

        PossessionClaimResponse response = PossessionClaimResponse.builder().build();
        when(defendantResponseService.getSubmittedResponse(CASE_REFERENCE)).thenReturn(response);

        PartyEntity claimant = caseEntity.getClaims().getFirst().getClaimParties().getFirst().getParty();
        AddressEntity addressEntity = claimant.getAddress();
        AddressUK mappedAddress = AddressUK.builder().build();
        when(addressMapper.toAddressUK(addressEntity)).thenReturn(mappedAddress);

        PCSCase pcsCase = PCSCase.builder().build();
        underTest.setCaseFields(pcsCase, caseEntity);

        assertThat(pcsCase.getPossessionClaimResponse()).isSameAs(response);
        assertThat(pcsCase.getPossessionClaimResponse().getClaimantServiceAddress()).isEqualTo(mappedAddress);

        verify(accessValidator).validateAndGetDefendant(caseEntity, userId);
    }

    @Test
    void shouldNotSetServiceAddressWhenClaimantHasNoAddress() {
        UUID userId = UUID.randomUUID();
        stubCitizenUser(userId);

        PcsCaseEntity caseEntity = buildCaseEntityWithClaimantIndividual();
        when(caseEntity.getCaseReference()).thenReturn(CASE_REFERENCE);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(true);

        PossessionClaimResponse response = PossessionClaimResponse.builder().build();
        when(defendantResponseService.getSubmittedResponse(CASE_REFERENCE)).thenReturn(response);

        PCSCase pcsCase = PCSCase.builder().build();
        underTest.setCaseFields(pcsCase, caseEntity);

        assertThat(pcsCase.getPossessionClaimResponse().getClaimantServiceAddress()).isNull();
        verify(addressMapper, never()).toAddressUK(any());
    }

    @Test
    void shouldNotLoadResponseWhenUserIsNotCitizen() {
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.PCS_SOLICITOR.getRole()));
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, caseEntity);

        assertThat(pcsCase.getPossessionClaimResponse()).isNull();
        verify(defendantResponseService, never()).hasSubmittedResponse(any(Long.class));
        verify(defendantResponseService, never()).getSubmittedResponse(any(Long.class));
    }

    @Test
    void shouldNotLoadResponseWhenCitizenHasNoSubmittedResponse() {
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);

        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        when(caseEntity.getCaseReference()).thenReturn(CASE_REFERENCE);
        when(defendantResponseService.hasSubmittedResponse(CASE_REFERENCE)).thenReturn(false);

        PCSCase pcsCase = PCSCase.builder().build();
        underTest.setCaseFields(pcsCase, caseEntity);

        assertThat(pcsCase.getPossessionClaimResponse()).isNull();
        verify(defendantResponseService, never()).getSubmittedResponse(any(Long.class));
        verify(accessValidator, never()).validateAndGetDefendant(any(), any());
    }

    private void stubCitizenUser(UUID userId) {
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
    }

    private static PcsCaseEntity buildCaseEntityWithClaimantAddress() {
        PartyEntity claimant = mock(PartyEntity.class);
        AddressEntity addressEntity = mock(AddressEntity.class);
        when(claimant.getAddress()).thenReturn(addressEntity);
        return buildCaseEntityWithClaimant(claimant);
    }

    private static PcsCaseEntity buildCaseEntityWithClaimantIndividual() {
        PartyEntity claimant = mock(PartyEntity.class);
        when(claimant.getAddress()).thenReturn(null);
        return buildCaseEntityWithClaimant(claimant);
    }

    private static PcsCaseEntity buildCaseEntityWithClaimant(PartyEntity claimant) {
        ClaimPartyEntity claimParty = mock(ClaimPartyEntity.class);
        when(claimParty.getRole()).thenReturn(PartyRole.CLAIMANT);
        when(claimParty.getParty()).thenReturn(claimant);

        ClaimEntity claimEntity = mock(ClaimEntity.class);
        when(claimEntity.getClaimParties()).thenReturn(List.of(claimParty));

        PcsCaseEntity caseEntity = mock(PcsCaseEntity.class);
        when(caseEntity.getClaims()).thenReturn(List.of(claimEntity));
        return caseEntity;
    }
}
