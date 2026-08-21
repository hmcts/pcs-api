package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;

@ExtendWith(MockitoExtension.class)
class GenAppVisibilityServiceTest {

    private static final UUID CURRENT_USER_ID = UUID.randomUUID();
    private static final String TEST_ORG_ID = "ORG-123";

    @Mock(strictness = LENIENT)
    private LegalRepresentativeRepository legalRepresentativeRepository;
    @Mock(strictness = LENIENT)
    private OrganisationDetailsService organisationDetailsService;

    private GenAppVisibilityService underTest;

    @BeforeEach
    void setUp() {
        underTest = new GenAppVisibilityService(legalRepresentativeRepository, organisationDetailsService);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = GenAppState.class, mode = EnumSource.Mode.EXCLUDE, names = "GEN_APP_ISSUED")
    void shouldTreatUnissuedGenAppsAsNotVisible(GenAppState state) {
        // Given
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(state);

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(genAppEntity, CURRENT_USER_ID, List.of());

        // Then
        assertThat(genAppVisibleToUser).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = VerticalYesNo.class, names = {"NO"})
    void shouldTreatGenAppsWithNoticeAsVisible(VerticalYesNo isWithoutNotice) {
        // Given
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(GEN_APP_ISSUED);
        when(genAppEntity.getWithoutNotice()).thenReturn(isWithoutNotice);

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(genAppEntity, CURRENT_USER_ID, List.of());

        // Then
        assertThat(genAppVisibleToUser).isTrue();
    }

    @ParameterizedTest
    @MethodSource("withoutNoticeScenarios")
    void shouldBaseVisibilityOfWithoutNoticeGenAppsOnUserAndOrgId(UUID applicantUserId,
                                                                  String applicantOrgId,
                                                                  String currentUserOrgId,
                                                                  boolean isLegalRepresentativeLinkedToPartyAndActive,
                                                                  boolean expectedIsVisible) {
        // Given
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        PartyEntity applicantParty = mock(PartyEntity.class, withSettings().strictness(Strictness.LENIENT));
        when(genAppEntity.getState()).thenReturn(GEN_APP_ISSUED);
        when(genAppEntity.getWithoutNotice()).thenReturn(VerticalYesNo.YES);
        when(genAppEntity.getParty()).thenReturn(applicantParty);

        UUID applicantPartyId = UUID.randomUUID();
        when(applicantParty.getId()).thenReturn(applicantPartyId);
        when(applicantParty.getIdamId()).thenReturn(applicantUserId);
        when(applicantParty.getOrganisationId()).thenReturn(applicantOrgId);

        when(organisationDetailsService.getOrganisationIdentifier(CURRENT_USER_ID.toString()))
            .thenReturn(currentUserOrgId);

        when(legalRepresentativeRepository
                 .isLegalRepresentativeLinkedToPartyAndActive(CURRENT_USER_ID, applicantPartyId))
                .thenReturn(isLegalRepresentativeLinkedToPartyAndActive);

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(genAppEntity, CURRENT_USER_ID, List.of());

        // Then
        assertThat(genAppVisibleToUser).isEqualTo(expectedIsVisible);
    }

    @Test
    void shouldCacheOrgIdLookup() {
        // Given
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        PartyEntity applicantParty = mock(PartyEntity.class);
        when(genAppEntity.getState()).thenReturn(GEN_APP_ISSUED);
        when(genAppEntity.getWithoutNotice()).thenReturn(VerticalYesNo.YES);
        when(genAppEntity.getParty()).thenReturn(applicantParty);

        UUID applicantPartyId = UUID.randomUUID();
        when(applicantParty.getId()).thenReturn(applicantPartyId);

        // When
        underTest.isGenAppVisibleToUser(genAppEntity, CURRENT_USER_ID, List.of());
        underTest.isGenAppVisibleToUser(genAppEntity, CURRENT_USER_ID, List.of());
        underTest.isGenAppVisibleToUser(genAppEntity, CURRENT_USER_ID, List.of());

        // Then
        verify(organisationDetailsService, times(1))
            .getOrganisationIdentifier(CURRENT_USER_ID.toString());
    }

    @ParameterizedTest
    @MethodSource("internalRoles")
    void shouldShowWithoutNoticeGenAppsToInternalUsers(UserRole internalRole) {
        // Given
        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getState()).thenReturn(GEN_APP_ISSUED);
        when(genAppEntity.getWithoutNotice()).thenReturn(VerticalYesNo.YES);

        // When
        boolean genAppVisibleToUser = underTest.isGenAppVisibleToUser(
            genAppEntity,
            null,
            List.of(internalRole.getRole())
        );

        // Then
        assertThat(genAppVisibleToUser).isTrue();
    }

    @Test
    void shouldTreatGenericPcsCaseworkerRoleAsInternalVisibilityRole() {
        // Given
        PartyEntity party = mock(PartyEntity.class);

        // When
        boolean documentVisibleToUser = underTest.isWithoutNoticeVisibleToUser(
            party,
            null,
            List.of(UserRole.PCS_CASE_WORKER.getRole())
        );

        // Then
        assertThat(documentVisibleToUser).isTrue();
    }

    @Test
    void shouldNotTreatSolicitorWithGenericPcsCaseworkerRoleAsInternalVisibilityRole() {
        // Given
        PartyEntity party = mock(PartyEntity.class);

        // When
        boolean documentVisibleToUser = underTest.isWithoutNoticeVisibleToUser(
            party,
            null,
            List.of(UserRole.PCS_CASE_WORKER.getRole(), UserRole.PCS_SOLICITOR.getRole())
        );

        // Then
        assertThat(documentVisibleToUser).isFalse();
    }

    @Test
    void shouldShowDocumentsLinkedToPendingWithoutNoticeGenAppsUsingPartyVisibilityRule() {
        // Given
        PartyEntity party = mock(PartyEntity.class);
        UUID partyId = UUID.randomUUID();
        when(party.getId()).thenReturn(partyId);
        when(party.getIdamId()).thenReturn(UUID.randomUUID());

        GenAppEntity genAppEntity = mock(GenAppEntity.class);
        when(genAppEntity.getWithoutNotice()).thenReturn(VerticalYesNo.YES);
        when(genAppEntity.getParty()).thenReturn(party);

        when(legalRepresentativeRepository
                 .isLegalRepresentativeLinkedToPartyAndActive(CURRENT_USER_ID, partyId))
            .thenReturn(true);

        // When
        boolean documentVisibleToUser = underTest.isGenAppDocumentVisibleToUser(
            genAppEntity,
            CURRENT_USER_ID,
            List.of()
        );

        // Then
        assertThat(documentVisibleToUser).isTrue();
    }

    @Test
    void shouldUseSamePartyVisibilityRuleForWithoutNoticeDocuments() {
        // Given
        PartyEntity party = mock(PartyEntity.class);
        UUID partyId = UUID.randomUUID();
        when(party.getId()).thenReturn(partyId);
        when(party.getIdamId()).thenReturn(UUID.randomUUID());
        when(legalRepresentativeRepository
                 .isLegalRepresentativeLinkedToPartyAndActive(CURRENT_USER_ID, partyId))
            .thenReturn(true);

        // When
        boolean documentVisibleToUser = underTest.isWithoutNoticeVisibleToUser(party, CURRENT_USER_ID, List.of());

        // Then
        assertThat(documentVisibleToUser).isTrue();
    }

    @Test
    void shouldHideNullGenAppDocument() {
        assertThat(underTest.isGenAppDocumentVisibleToUser(null, CURRENT_USER_ID, List.of())).isFalse();
    }

    @Test
    void shouldReturnVisibleGenAppsForUserUsingDefaultRoles() {
        // Given
        GenAppEntity visibleWithNoticeGenApp = createGenApp(
            GEN_APP_ISSUED,
            VerticalYesNo.NO,
            LocalDateTime.of(2026, 6, 1, 10, 0),
            null
        );
        GenAppEntity hiddenWithoutNoticeGenApp = createGenApp(
            GEN_APP_ISSUED,
            VerticalYesNo.YES,
            LocalDateTime.of(2026, 6, 2, 10, 0),
            PartyEntity.builder().idamId(UUID.randomUUID()).build()
        );
        GenAppEntity pendingGenApp = createGenApp(
            GenAppState.PENDING_GEN_APP_ISSUED,
            VerticalYesNo.NO,
            LocalDateTime.of(2026, 6, 3, 10, 0),
            null
        );

        // When
        List<GenAppEntity> visibleGenApps = underTest.getVisibleGenAppsToUser(
            List.of(pendingGenApp, hiddenWithoutNoticeGenApp, visibleWithNoticeGenApp),
            CURRENT_USER_ID
        );

        // Then
        assertThat(visibleGenApps).containsExactly(visibleWithNoticeGenApp);
    }

    @Test
    void shouldReturnVisibleGenAppsForUserUsingRoles() {
        // Given
        GenAppEntity olderWithoutNoticeGenApp = createGenApp(
            GEN_APP_ISSUED,
            VerticalYesNo.YES,
            LocalDateTime.of(2026, 6, 1, 10, 0),
            null
        );
        GenAppEntity newerWithoutNoticeGenApp = createGenApp(
            GEN_APP_ISSUED,
            VerticalYesNo.YES,
            LocalDateTime.of(2026, 6, 2, 10, 0),
            null
        );

        // When
        List<GenAppEntity> visibleGenApps = underTest.getVisibleGenAppsToUser(
            Arrays.asList(olderWithoutNoticeGenApp, null, newerWithoutNoticeGenApp),
            CURRENT_USER_ID,
            List.of(UserRole.JUDGE.getRole())
        );

        // Then
        assertThat(visibleGenApps).containsExactly(newerWithoutNoticeGenApp, olderWithoutNoticeGenApp);
    }

    @Test
    void shouldReturnEmptyVisibleGenAppsWhenInputIsNullOrEmpty() {
        assertThat(underTest.getVisibleGenAppsToUser(null, CURRENT_USER_ID)).isEmpty();
        assertThat(underTest.getVisibleGenAppsToUser(List.of(), CURRENT_USER_ID, List.of())).isEmpty();
    }

    private static Stream<Arguments> withoutNoticeScenarios() {
        UUID differentApplicantUserId = UUID.randomUUID();

        return Stream.of(
            Arguments.argumentSet(
                "current user is applicant",
                CURRENT_USER_ID,
                TEST_ORG_ID, // applicantOrgId
                null,        // currentUserOrgId
                false,       // isLegalRepresentativeLinkedToPartyAndActive
                true
            ),
            Arguments.argumentSet(
                "current user not applicant but is in same org",
                differentApplicantUserId,
                TEST_ORG_ID, // applicantOrgId
                TEST_ORG_ID, // currentUserOrgId
                false,       // isLegalRepresentativeLinkedToPartyAndActive
                true
            ),
            Arguments.argumentSet(
                "current user is LR of applicant",
                differentApplicantUserId,
                TEST_ORG_ID, // applicantOrgId
                null,        // currentUserOrgId
                true,        // isLegalRepresentativeLinkedToPartyAndActive
                true
            ),
            Arguments.argumentSet(
                "current user is not applicant, in any org or LR of applicant",
                differentApplicantUserId,
                TEST_ORG_ID,        // applicantOrgId
                null,               // currentUserOrgId
                false,              // isLegalRepresentativeLinkedToPartyAndActive
                false
            ),
            Arguments.argumentSet(
                "current user is not applicant or LR of applicant and applicant is not in a org",
                differentApplicantUserId,
                null,               // applicantOrgId
                TEST_ORG_ID,        // currentUserOrgId
                false,              // isLegalRepresentativeLinkedToPartyAndActive
                false
            ),
            Arguments.argumentSet(
                "current user is not applicant, in same org or LR of applicant",
                differentApplicantUserId,
                TEST_ORG_ID,        // applicantOrgId
                "different org ID", // currentUserOrgId
                false,              // isLegalRepresentativeLinkedToPartyAndActive
                false
            )
        );
    }

    private static Stream<UserRole> internalRoles() {
        return Stream.concat(
            Arrays.stream(CASEWORKER_ROLES),
            Arrays.stream(JUDICIAL_HISTORY_ROLES)
        ).distinct();
    }

    private static GenAppEntity createGenApp(GenAppState state,
                                             VerticalYesNo withoutNotice,
                                             LocalDateTime submittedDate,
                                             PartyEntity party) {
        return GenAppEntity.builder()
            .state(state)
            .withoutNotice(withoutNotice)
            .applicationSubmittedDate(submittedDate)
            .party(party)
            .build();
    }

}
