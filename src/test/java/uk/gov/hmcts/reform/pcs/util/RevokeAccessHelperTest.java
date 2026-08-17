package uk.gov.hmcts.reform.pcs.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseRoleAssignmentService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeAccessHelperTest {

    @InjectMocks
    private RevokeAccessHelper revokeAccessHelper;

    @Mock
    private ClaimPartyLegalRepresentativeOrganisationRepository partyLegalRepOrgRepository;

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;

    @Mock
    private CaseRoleAssignmentService caseRoleAssignmentService;

    @Mock
    private PartyAccessCodeRepository partyAccessCodeRepository;

    @Captor
    private ArgumentCaptor<List<ClaimPartyLegalRepresentativeOrganisationEntity>> saveAllCaptor;

    @Test
    void revokeOrganisationAccessToRespondToClaim_WithOtherDefendants_DoNotRevokeRasRolesInvalidateAndEntities() {
        // given
        long caseReference = 123L;
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().caseReference(caseReference).build();
        LegalRepresentativeOrganisationEntity lro = LegalRepresentativeOrganisationEntity.builder()
            .id(1234)
            .build();

        PartyEntity defendant = PartyEntity.builder().id(partyId).build();

        var plro = ClaimPartyLegalRepresentativeOrganisationEntity.builder()
            .party(defendant)
            .legalRepresentativeOrganisation(lro)
            .active(YesOrNo.YES)
            .startDate(Instant.now())
            .build();

        when(partyLegalRepOrgRepository
                 .findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
                     defendant.getId(), lro.getId(), caseReference))
            .thenReturn(List.of(plro));

        // when
        revokeAccessHelper.revokeOrgAccessToRespondToClaim(caseEntity, lro, defendant);

        // then - draft deletion always called
        verify(draftCaseDataRepository).deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
            eq(caseReference),
            eq(EventId.respondPossessionClaim),
            eq(String.valueOf(lro.getOrganisationId())),
            eq(defendant.getId())
        );

        // revokeRasRole should NOT be called because organisation represents other defendants
        verify(caseRoleAssignmentService, never()).revokeCaseRole(
            eq(caseReference), anyString(), eq(UserRole.DEFENDANT_SOLICITOR));

        // entities should be invalidated and saved
        verify(partyLegalRepOrgRepository).saveAll(saveAllCaptor.capture());
        List<ClaimPartyLegalRepresentativeOrganisationEntity> saved = saveAllCaptor.getValue();
        assertNotNull(saved);
        assertEquals(1, saved.size());
        ClaimPartyLegalRepresentativeOrganisationEntity savedEntity = saved.getFirst();
        assertEquals(YesOrNo.NO, savedEntity.getActive());
        assertNotNull(savedEntity.getEndDate());
    }

    @Test
    void revokeOrganisationAccessToRespondToClaim_WithNoOtherDefendants_RevokeRasRolesInvalidateEntities() {
        // given
        long caseReference = 456L;
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity caseEntity = PcsCaseEntity.builder().caseReference(caseReference).build();

        ClaimPartyLegalRepresentativeOrganisationEntity plro1 =
            ClaimPartyLegalRepresentativeOrganisationEntity.builder().id(12345).build();
        ClaimPartyLegalRepresentativeOrganisationEntity plro2 =
            ClaimPartyLegalRepresentativeOrganisationEntity.builder().id(56789).build();

        LegalRepresentativeOrganisationEntity lro = LegalRepresentativeOrganisationEntity.builder()
            .id(123456)
            .claimPartyLegalRepresentativeOrganisationList(List.of(plro1, plro2))
            .build();

        PartyEntity defendant = PartyEntity.builder().id(partyId).build();

        var plro = ClaimPartyLegalRepresentativeOrganisationEntity.builder()
            .party(defendant)
            .legalRepresentativeOrganisation(lro)
            .active(YesOrNo.YES)
            .startDate(Instant.now())
            .build();

        when(partyLegalRepOrgRepository
                 .findAllActiveByPartyIdLegalRepresentativeOrganisationIdAndCase(
                     defendant.getId(), lro.getId(), caseReference))
            .thenReturn(List.of(plro));

        // when
        revokeAccessHelper.revokeOrgAccessToRespondToClaim(caseEntity, lro, defendant);

        // then - draft deletion called
        verify(draftCaseDataRepository).deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
            eq(caseReference),
            eq(EventId.respondPossessionClaim),
            eq(String.valueOf(lro.getOrganisationId())),
            eq(defendant.getId())
        );

        // revokeRasRole should be called for both legal representatives
        verify(caseRoleAssignmentService).revokeCaseRole(
            eq(caseReference), eq(plro1.toString()), eq(UserRole.DEFENDANT_SOLICITOR));
        verify(caseRoleAssignmentService).revokeCaseRole(
            eq(caseReference), eq(plro2.toString()), eq(UserRole.DEFENDANT_SOLICITOR));

        // entities should be invalidated and saved
        verify(partyLegalRepOrgRepository).saveAll(saveAllCaptor.capture());
        List<ClaimPartyLegalRepresentativeOrganisationEntity> saved = saveAllCaptor.getValue();
        assertNotNull(saved);
        assertEquals(1, saved.size());
        ClaimPartyLegalRepresentativeOrganisationEntity savedEntity = saved.getFirst();
        assertEquals(YesOrNo.NO, savedEntity.getActive());
        assertNotNull(savedEntity.getEndDate());
    }

    @Test
    void revokeDefendantsAccess_withIdamId_revokesRole_deletesDrafts_and_deletesAccessCode() {
        // given
        long caseReference = 789L;
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID idamId = UUID.randomUUID();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .id(caseId)
            .build();

        PartyEntity defendant = PartyEntity.builder()
            .id(partyId)
            .idamId(idamId)
            .build();

        // when
        revokeAccessHelper.revokeDefendantsAccessToRespondToClaim(caseEntity, defendant);

        // then
        verify(caseRoleAssignmentService).revokeCaseRole(
            eq(caseReference), eq(idamId.toString()), eq(UserRole.DEFENDANT));
        verify(draftCaseDataRepository).deleteByCaseReferenceAndEventIdAndIdamUserId(
            eq(caseReference), eq(EventId.respondPossessionClaim), eq(idamId));
        verify(partyAccessCodeRepository).deleteByPcsCase_IdAndPartyId(eq(caseEntity.getId()), eq(defendant.getId()));
    }

    @Test
    void revokeDefendantsAccess_withoutIdamId_onlyDeletesAccessCode() {
        // given
        long caseReference = 999L;
        UUID caseId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .caseReference(caseReference)
            .id(caseId)
            .build();

        PartyEntity defendant = PartyEntity.builder()
            .id(partyId)
            .idamId(null)
            .build();

        // when
        revokeAccessHelper.revokeDefendantsAccessToRespondToClaim(caseEntity, defendant);

        // then
        verify(caseRoleAssignmentService, never()).revokeCaseRole(
            eq(caseReference), anyString(), eq(UserRole.DEFENDANT));
        verify(draftCaseDataRepository, never()).deleteByCaseReferenceAndEventIdAndIdamUserId(
            eq(caseReference), eq(EventId.respondPossessionClaim), any());
        verify(partyAccessCodeRepository).deleteByPcsCase_IdAndPartyId(eq(caseEntity.getId()), eq(defendant.getId()));
    }
}
