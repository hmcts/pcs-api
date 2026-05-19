package uk.gov.hmcts.reform.pcs.noc.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.idam.IdamService;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobEntity;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobStatus;
import uk.gov.hmcts.reform.pcs.noc.entity.NocSideEffectJobType;
import uk.gov.hmcts.reform.pcs.noc.entity.PartyRepresentationEntity;
import uk.gov.hmcts.reform.pcs.noc.entity.PartyRepresentationStatus;
import uk.gov.hmcts.reform.pcs.noc.exception.NocException;
import uk.gov.hmcts.reform.pcs.noc.model.NocAnswersRequest;
import uk.gov.hmcts.reform.pcs.noc.repository.PartyRepresentationRepository;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationUsersResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PcsNocServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String AUTHORISATION = "Bearer user-token";
    private static final String USER_ID = "requesting-user";
    private static final String INCOMING_ORG_ID = "ORG-INCOMING";
    private static final String INCUMBENT_ORG_ID = "ORG-OLD";
    private static final String INCOMING_USER_ID = "incoming-user";
    private static final String INCUMBENT_USER_ID = "old-user";

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private PartyRepresentationRepository partyRepresentationRepository;
    @Mock
    private OrganisationDetailsService organisationDetailsService;
    @Mock
    private IdamService idamService;
    @Mock
    private NocSideEffectService sideEffectService;
    @Mock
    private UserInfo userInfo;
    @Captor
    private ArgumentCaptor<PartyRepresentationEntity> representationCaptor;
    @Captor
    private ArgumentCaptor<NocSideEffectJobEntity> jobCaptor;

    private PcsNocService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PcsNocService(
            pcsCaseRepository,
            partyRepresentationRepository,
            organisationDetailsService,
            idamService,
            sideEffectService
        );

        lenient().when(idamService.validateAuthToken(AUTHORISATION)).thenReturn(new User("token", userInfo));
        lenient().when(userInfo.getUid()).thenReturn(USER_ID);
        lenient().when(organisationDetailsService.getOrganisationDetails(USER_ID))
            .thenReturn(organisationDetails(INCOMING_ORG_ID, "Incoming Solicitors"));
        lenient().when(organisationDetailsService.getOrganisationUsers(INCOMING_ORG_ID))
            .thenReturn(users(user(INCOMING_USER_ID, "incoming@example.com")));
        lenient().when(organisationDetailsService.getOrganisationUsers(INCUMBENT_ORG_ID))
            .thenReturn(users(user(INCUMBENT_USER_ID, "old@example.com")));
        lenient().when(sideEffectService.job(
            anyLong(),
            any(UUID.class),
            any(NocSideEffectJobType.class),
            nullable(String.class),
            nullable(String.class),
            anyString(),
            nullable(String.class),
            nullable(String.class),
            anyString()
        )).thenAnswer(invocation -> NocSideEffectJobEntity.builder()
            .caseReference(invocation.getArgument(0))
            .partyId(invocation.getArgument(1))
            .type(invocation.getArgument(2))
            .userId(invocation.getArgument(3))
            .organisationId(invocation.getArgument(4))
            .caseRole(invocation.getArgument(5))
            .email(invocation.getArgument(6))
            .detail(invocation.getArgument(7))
            .idempotencyKey(invocation.getArgument(8))
            .status(NocSideEffectJobStatus.PENDING)
            .build());
    }

    @Test
    void shouldRejectAnswersThatDoNotMatchADefendant() {
        stubCase(caseWith(defendant(UUID.randomUUID(), "Alice", "Akins")));

        Throwable throwable = catchThrowable(() -> underTest.verify(request("Jane", "Jones"), AUTHORISATION));

        assertThat(throwable)
            .isInstanceOf(NocException.class)
            .extracting("code")
            .isEqualTo("answers-not-matched-any-litigant");
    }

    @Test
    void shouldRejectAnswersThatMatchMultipleDefendants() {
        stubCase(caseWith(
            defendant(UUID.randomUUID(), "Alice", "Akins"),
            defendant(UUID.randomUUID(), "Alice", "Akins")
        ));

        Throwable throwable = catchThrowable(() -> underTest.verify(request("Alice", "Akins"), AUTHORISATION));

        assertThat(throwable)
            .isInstanceOf(NocException.class)
            .extracting("code")
            .isEqualTo("answers-not-identify-litigant");
    }

    @Test
    void shouldRejectVerifyWhenIncomingOrganisationAlreadyRepresentsMatchedDefendant() {
        PartyEntity defendant = defendant(UUID.randomUUID(), "Alice", "Akins");
        stubCase(caseWith(defendant));
        when(partyRepresentationRepository.existsByCaseReferenceAndPartyIdAndOrganisationIdAndStatus(
            CASE_REFERENCE,
            defendant.getId(),
            INCOMING_ORG_ID,
            PartyRepresentationStatus.ACTIVE
        )).thenReturn(true);

        Throwable throwable = catchThrowable(() -> underTest.verify(request("Alice", "Akins"), AUTHORISATION));

        assertThat(throwable)
            .isInstanceOf(NocException.class)
            .extracting("code")
            .isEqualTo("requesting-org-already-represents-party");
    }

    @Test
    void shouldAllowIncomingOrganisationRepresentingAnotherDefendant() {
        PartyEntity matchedDefendant = defendant(UUID.randomUUID(), "Alice", "Akins");
        PartyEntity otherDefendant = defendant(UUID.randomUUID(), "Bob", "Brown");
        stubCase(caseWith(matchedDefendant, otherDefendant));
        when(partyRepresentationRepository.findByCaseReferenceAndPartyIdAndStatus(
            CASE_REFERENCE,
            matchedDefendant.getId(),
            PartyRepresentationStatus.ACTIVE
        )).thenReturn(Optional.empty());

        underTest.submit(request("Alice", "Akins"), AUTHORISATION);

        verify(partyRepresentationRepository).save(representationCaptor.capture());
        assertThat(representationCaptor.getValue().getPartyId()).isEqualTo(matchedDefendant.getId());
    }

    @Test
    void shouldCreateRepresentationAndGrantIncomingUsersWhenDefendantIsUnrepresented() {
        PartyEntity defendant = defendant(UUID.randomUUID(), "Alice", "Akins");
        stubCase(caseWith(defendant));
        when(partyRepresentationRepository.findByCaseReferenceAndPartyIdAndStatus(
            CASE_REFERENCE,
            defendant.getId(),
            PartyRepresentationStatus.ACTIVE
        )).thenReturn(Optional.empty());

        underTest.submit(request("Alice", "Akins"), AUTHORISATION);

        verify(partyRepresentationRepository).save(representationCaptor.capture());
        PartyRepresentationEntity savedRepresentation = representationCaptor.getValue();
        assertThat(savedRepresentation.getPartyId()).isEqualTo(defendant.getId());
        assertThat(savedRepresentation.getOrganisationId()).isEqualTo(INCOMING_ORG_ID);
        assertThat(savedRepresentation.getStatus()).isEqualTo(PartyRepresentationStatus.ACTIVE);

        verify(sideEffectService, org.mockito.Mockito.times(2)).enqueue(jobCaptor.capture());
        assertThat(jobCaptor.getAllValues())
            .extracting(NocSideEffectJobEntity::getType)
            .containsExactly(NocSideEffectJobType.GRANT_CASE_ROLE, NocSideEffectJobType.AUDIT);
    }

    @Test
    void shouldRevokeIncumbentOrgWhenItNoLongerRepresentsAnotherDefendant() {
        PartyEntity defendant = defendant(UUID.randomUUID(), "Alice", "Akins");
        PartyRepresentationEntity incumbent = activeRepresentation(defendant.getId(), INCUMBENT_ORG_ID);
        stubCase(caseWith(defendant));
        when(partyRepresentationRepository.findByCaseReferenceAndPartyIdAndStatus(
            CASE_REFERENCE,
            defendant.getId(),
            PartyRepresentationStatus.ACTIVE
        )).thenReturn(Optional.of(incumbent));
        when(partyRepresentationRepository.findByCaseReferenceAndOrganisationIdAndCaseRoleAndStatus(
            CASE_REFERENCE,
            INCUMBENT_ORG_ID,
            PcsNocService.DEFENDANT_SOLICITOR_ROLE,
            PartyRepresentationStatus.ACTIVE
        )).thenReturn(List.of());

        underTest.submit(request("Alice", "Akins"), AUTHORISATION);

        assertThat(incumbent.getStatus()).isEqualTo(PartyRepresentationStatus.ENDED);
        assertThat(incumbent.getEndedAt()).isNotNull();

        verify(sideEffectService, org.mockito.Mockito.times(4)).enqueue(jobCaptor.capture());
        assertThat(jobCaptor.getAllValues())
            .extracting(NocSideEffectJobEntity::getType)
            .containsExactly(
                NocSideEffectJobType.GRANT_CASE_ROLE,
                NocSideEffectJobType.REVOKE_CASE_ROLE,
                NocSideEffectJobType.NOTIFY_CASE_ACCESS_REMOVED,
                NocSideEffectJobType.AUDIT
            );
    }

    @Test
    void shouldRetainIncumbentOrgCoarseAccessWhenItStillRepresentsAnotherDefendant() {
        PartyEntity defendant = defendant(UUID.randomUUID(), "Alice", "Akins");
        PartyEntity otherDefendant = defendant(UUID.randomUUID(), "Bob", "Brown");
        PartyRepresentationEntity incumbent = activeRepresentation(defendant.getId(), INCUMBENT_ORG_ID);
        PartyRepresentationEntity otherActiveRepresentation = activeRepresentation(
            otherDefendant.getId(),
            INCUMBENT_ORG_ID
        );
        stubCase(caseWith(defendant, otherDefendant));
        when(partyRepresentationRepository.findByCaseReferenceAndPartyIdAndStatus(
            CASE_REFERENCE,
            defendant.getId(),
            PartyRepresentationStatus.ACTIVE
        )).thenReturn(Optional.of(incumbent));
        when(partyRepresentationRepository.findByCaseReferenceAndOrganisationIdAndCaseRoleAndStatus(
            CASE_REFERENCE,
            INCUMBENT_ORG_ID,
            PcsNocService.DEFENDANT_SOLICITOR_ROLE,
            PartyRepresentationStatus.ACTIVE
        )).thenReturn(List.of(otherActiveRepresentation));

        underTest.submit(request("Alice", "Akins"), AUTHORISATION);

        verify(sideEffectService, org.mockito.Mockito.times(3)).enqueue(jobCaptor.capture());
        assertThat(jobCaptor.getAllValues())
            .extracting(NocSideEffectJobEntity::getType)
            .containsExactly(
                NocSideEffectJobType.GRANT_CASE_ROLE,
                NocSideEffectJobType.NOTIFY_PARTY_REPRESENTATION_RETAINED,
                NocSideEffectJobType.AUDIT
            );
    }

    @Test
    void shouldTreatDuplicateSubmitAsAlreadyAppliedWithoutDuplicatingRepresentation() {
        PartyEntity defendant = defendant(UUID.randomUUID(), "Alice", "Akins");
        stubCase(caseWith(defendant));
        when(partyRepresentationRepository.existsByCaseReferenceAndPartyIdAndOrganisationIdAndStatus(
            CASE_REFERENCE,
            defendant.getId(),
            INCOMING_ORG_ID,
            PartyRepresentationStatus.ACTIVE
        )).thenReturn(true);

        underTest.submit(request("Alice", "Akins"), AUTHORISATION);

        verify(partyRepresentationRepository, never()).save(any(PartyRepresentationEntity.class));
        verify(sideEffectService, never()).enqueue(any());
    }

    private void stubCase(PcsCaseEntity pcsCase) {
        when(pcsCaseRepository.findWithPartiesAndClaimPartiesByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(pcsCase));
    }

    private NocAnswersRequest request(String firstName, String lastName) {
        return new NocAnswersRequest(String.valueOf(CASE_REFERENCE), List.of(
            new uk.gov.hmcts.reform.pcs.noc.model.NocAnswer(
                PcsNocService.DEFENDANT_FIRST_NAME_QUESTION_ID,
                firstName
            ),
            new uk.gov.hmcts.reform.pcs.noc.model.NocAnswer(
                PcsNocService.DEFENDANT_LAST_NAME_QUESTION_ID,
                lastName
            )
        ));
    }

    private PcsCaseEntity caseWith(PartyEntity... parties) {
        return PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .parties(new HashSet<>(Set.of(parties)))
            .build();
    }

    private PartyEntity defendant(UUID id, String forename, String surname) {
        PartyEntity party = PartyEntity.builder()
            .id(id)
            .forename(forename)
            .surname(surname)
            .claimParties(new HashSet<>())
            .build();
        party.getClaimParties().add(ClaimPartyEntity.builder()
            .party(party)
            .role(PartyRole.DEFENDANT)
            .build());
        return party;
    }

    private PartyRepresentationEntity activeRepresentation(UUID partyId, String organisationId) {
        return PartyRepresentationEntity.builder()
            .caseReference(CASE_REFERENCE)
            .partyId(partyId)
            .partyRole(PartyRole.DEFENDANT)
            .organisationId(organisationId)
            .caseRole(PcsNocService.DEFENDANT_SOLICITOR_ROLE)
            .status(PartyRepresentationStatus.ACTIVE)
            .build();
    }

    private OrganisationDetailsResponse organisationDetails(String organisationId, String name) {
        return OrganisationDetailsResponse.builder()
            .organisationIdentifier(organisationId)
            .name(name)
            .build();
    }

    private OrganisationUsersResponse users(OrganisationUsersResponse.ProfessionalUser... users) {
        return OrganisationUsersResponse.builder()
            .users(List.of(users))
            .build();
    }

    private OrganisationUsersResponse.ProfessionalUser user(String userId, String email) {
        return OrganisationUsersResponse.ProfessionalUser.builder()
            .userIdentifier(userId)
            .email(email)
            .build();
    }
}
