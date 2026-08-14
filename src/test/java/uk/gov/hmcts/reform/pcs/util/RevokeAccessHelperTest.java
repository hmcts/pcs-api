package uk.gov.hmcts.reform.pcs.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyLegalRepresentativeOrganisationRepository;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeAccessHelperTest {

    private static final String ORGANISATION_ID = "ORG-123";

    @InjectMocks
    private RevokeAccessHelper revokeAccessHelper;

    @Mock
    private ClaimPartyLegalRepresentativeOrganisationRepository partyLegalRepOrgRepository;

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;

    @Mock
    private PartyAccessCodeRepository partyAccessCodeRepository;

    @Captor
    private ArgumentCaptor<ClaimPartyLegalRepresentativeOrganisationEntity> saveCaptor;

    @Test
    void revokeOrganisationAccessToRespondToClaim_WithActivePartyLink_DeletesDraftAndDeactivatesLink() {
        // given
        long caseReference = 123L;
        UUID partyId = UUID.randomUUID();

        UserInfo user = UserInfo.builder().uid(UUID.randomUUID().toString()).build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder().caseReference(caseReference).build();

        LegalRepresentativeOrganisationEntity legalRepOrganisation = LegalRepresentativeOrganisationEntity.builder()
            .id(1)
            .organisationId(ORGANISATION_ID)
            .build();

        PartyEntity defendant = PartyEntity.builder().id(partyId).build();

        ClaimPartyLegalRepresentativeOrganisationEntity activeLink =
            ClaimPartyLegalRepresentativeOrganisationEntity.builder()
                .party(defendant)
                .legalRepresentativeOrganisation(legalRepOrganisation)
                .active(YesOrNo.YES)
                .startDate(Instant.now())
                .build();

        when(partyLegalRepOrgRepository.findByPartyIdAndLegalRepresentativeOrganisation_OrganisationIdAndActive(
            defendant.getId(), ORGANISATION_ID, YesOrNo.YES
        )).thenReturn(Optional.of(activeLink));

        // when
        revokeAccessHelper.revokeOrganisationAccessToRespondToClaim(caseEntity, legalRepOrganisation, defendant, user);

        // then - the organisation's draft response for this defendant is deleted
        verify(draftCaseDataRepository).deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
            eq(caseReference),
            eq(EventId.respondPossessionClaim),
            eq(ORGANISATION_ID),
            eq(defendant.getId())
        );

        // and the link between the defendant and the organisation is deactivated
        verify(partyLegalRepOrgRepository).save(saveCaptor.capture());

        ClaimPartyLegalRepresentativeOrganisationEntity saved = saveCaptor.getValue();
        assertThat(saved.getActive()).isEqualTo(YesOrNo.NO);
        assertThat(saved.getEndDate()).isNotNull();
    }

    @Test
    void revokeOrganisationAccessToRespondToClaim_WithNoActivePartyLink_DeletesDraftButSavesNothing() {
        // given
        long caseReference = 321L;
        UUID partyId = UUID.randomUUID();

        UserInfo user = UserInfo.builder().uid(UUID.randomUUID().toString()).build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder().caseReference(caseReference).build();

        LegalRepresentativeOrganisationEntity legalRepOrganisation = LegalRepresentativeOrganisationEntity.builder()
            .id(2)
            .organisationId(ORGANISATION_ID)
            .build();

        PartyEntity defendant = PartyEntity.builder().id(partyId).build();

        // no active link remains between this defendant and the organisation
        when(partyLegalRepOrgRepository.findByPartyIdAndLegalRepresentativeOrganisation_OrganisationIdAndActive(
            defendant.getId(), ORGANISATION_ID, YesOrNo.YES
        )).thenReturn(Optional.empty());

        // when
        revokeAccessHelper.revokeOrganisationAccessToRespondToClaim(caseEntity, legalRepOrganisation, defendant, user);

        // then - the draft is still deleted
        verify(draftCaseDataRepository).deleteByCaseReferenceAndEventIdAndLegalRepresentativeOrganisationIdAndPartyId(
            eq(caseReference),
            eq(EventId.respondPossessionClaim),
            eq(ORGANISATION_ID),
            eq(defendant.getId())
        );

        // but nothing is deactivated or saved
        verify(partyLegalRepOrgRepository, never()).save(any());
    }

    @Test
    void revokeDefendantsAccess_withIdamId_deletesDrafts_deletesAccessCode_andClearsIdamId() {
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
        verify(draftCaseDataRepository).deleteByCaseReferenceAndEventIdAndIdamUserId(
            eq(caseReference), eq(EventId.respondPossessionClaim), eq(idamId));
        verify(partyAccessCodeRepository).deleteByPcsCase_IdAndPartyId(eq(caseEntity.getId()), eq(defendant.getId()));

        assertThat(defendant.getIdamId()).isNull();
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

        // then - no draft deletion is attempted for a defendant that never signed in
        verify(draftCaseDataRepository, never()).deleteByCaseReferenceAndEventIdAndIdamUserId(
            eq(caseReference), eq(EventId.respondPossessionClaim), any());
        verify(partyAccessCodeRepository).deleteByPcsCase_IdAndPartyId(eq(caseEntity.getId()), eq(defendant.getId()));
    }
}
