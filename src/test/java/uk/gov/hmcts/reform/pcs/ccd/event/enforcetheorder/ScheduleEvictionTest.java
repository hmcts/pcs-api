package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalUser;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.scheduleeviction.ScheduleEvictionStart;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.ScheduledEvictionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.WarrantEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.event.EventStates;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.ScheduledEvictionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleEvictionTest extends BaseEventTest {

    private static final ExternalUser USER = new ExternalUser("user-id", List.of("caseworker-pcs"), null);
    private static final LocalDateTime EVICTION_DATE = LocalDateTime.of(2027, 6, 25, 10, 0);
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ScheduledEvictionRepository scheduledEvictionRepository;
    @Mock
    private CaseReferenceFormatter caseReferenceFormatter;
    @Mock
    private CaseNameFormatter caseNameFormatter;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private EnforcementOrderMediator enforcementOrderMediator;

    @InjectMocks
    private ScheduleEviction underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldBeConfiguredAsShowForRelease1bFeatureFlagAndScheduleEvictionShowCondition() {
        assertConfiguredShowConditions(ShowConditions.and(
            ShowConditions.featureFlagsEnabled(FeatureFlag.RELEASE_1B),
            "enfShowConditions.scheduleEviction=\"Yes\""
        ));
    }

    @Test
    void shouldBeAvailableInScheduleEvictionStates() {
        assertConfiguredForStates(EventStates.scheduleEviction());
    }

    @Test
    void shouldGrantBailiffAdmin() {
        assertGrants(UserRole.BAILIFF_ADMIN, Permission.CRU);
    }

    @Test
    void shouldRejectStartWhenRelease1bIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(false);

        assertThat(startRejection()).containsExactly(ScheduleEviction.NOT_AVAILABLE);
        verifyNoInteractions(pcsCaseService);
    }

    @Test
    void shouldRejectStartWhenNoWarrantAwaitingScheduling() {
        // Given
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(true);
        PcsCaseEntity pcsCase = caseWithWarrant(EnforcementState.PENDING_CASE_ISSUED);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCase);

        // When / Then
        assertThat(startRejection()).containsExactly(ScheduleEviction.NO_WARRANT_AWAITING_SCHEDULING);
    }

    @Test
    void shouldReturnCaseAndEvictionDetailsOnStart() {
        // Given
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(true);
        PcsCaseEntity pcsCase = caseWithWarrant(EnforcementState.AWAITING_SCHEDULING);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCase);
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(TEST_CASE_REFERENCE)).thenReturn("1234");
        AddressUK addressUk = AddressUK.builder().build();
        when(addressMapper.toAddressUK(pcsCase.getPropertyAddress())).thenReturn(addressUk);
        when(addressFormatter.formatShortAddress(addressUk, AddressFormatter.COMMA_DELIMITER))
            .thenReturn("1 Street, W3 7RX");
        when(caseNameFormatter.formatCaseName(pcsCase)).thenReturn("Claimant v Defendant");
        when(recipientAddressResolver.resolveDisplayName(any(PartyEntity.class)))
            .thenReturn("Claimant", "Defendant");

        // When
        ScheduleEvictionStart start = (ScheduleEvictionStart) ((ExternalStartResponse.Started<?>)
            configuredEvent.start(payload(), USER, null)).payload();

        // Then
        assertThat(start.enforcementApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(start.propertyAddress()).isEqualTo("1 Street, W3 7RX");
        assertThat(start.claimantNames()).containsExactly("Claimant");
        assertThat(start.defendantNames()).containsExactly("Defendant");
    }

    @Test
    void shouldSaveScheduledEvictionAgainstWarrantInUkTime() {
        // Given
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(true);
        PcsCaseEntity pcsCase = caseWithWarrant(EnforcementState.AWAITING_SCHEDULING);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCase);
        WarrantEntity warrant = pcsCase.getEnforcementOrders().iterator().next().getWarrantDetails();

        // When
        SubmitResponse<State> response = submit(
            new ScheduleEvictionRequest(APPLICATION_ID, VerticalYesNo.YES, EVICTION_DATE));

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        ArgumentCaptor<ScheduledEvictionEntity> saved = ArgumentCaptor.forClass(ScheduledEvictionEntity.class);
        verify(scheduledEvictionRepository).save(saved.capture());
        assertThat(saved.getValue().getEnfWarrant()).isSameAs(warrant);
        assertThat(saved.getValue().getCourtGivenPermissionToDispenseWithService()).isEqualTo(VerticalYesNo.YES);
        assertThat(saved.getValue().getScheduledEvictionDate()).isEqualTo(Instant.parse("2027-06-25T09:00:00Z"));
        assertThat(pcsCase.getEnforcementOrders().iterator().next().getState())
            .isEqualTo(EnforcementState.AWAITING_PROPERTY_VISIT);
    }

    @Test
    void shouldRejectSubmitWithoutEvictionDate() {
        givenCaseWithWarrantAwaitingScheduling();

        assertThat(submitRejection(new ScheduleEvictionRequest(APPLICATION_ID, VerticalYesNo.NO, null)))
            .containsExactly(ScheduleEviction.DATE_REQUIRED);
        verifyNoInteractions(scheduledEvictionRepository);
    }

    @Test
    void shouldRejectSubmitWithoutDispenseWithServiceAnswer() {
        givenCaseWithWarrantAwaitingScheduling();

        assertThat(submitRejection(new ScheduleEvictionRequest(APPLICATION_ID, null, EVICTION_DATE)))
            .containsExactly(ScheduleEviction.DISPENSE_WITH_SERVICE_REQUIRED);
        verifyNoInteractions(scheduledEvictionRepository);
    }

    @Test
    void shouldRejectSubmitForAnotherWarrant() {
        givenCaseWithWarrantAwaitingScheduling();

        assertThat(submitRejection(new ScheduleEvictionRequest(UUID.randomUUID(), VerticalYesNo.NO, EVICTION_DATE)))
            .containsExactly(ScheduleEviction.WARRANT_NO_LONGER_AWAITING_SCHEDULING);
        verifyNoInteractions(scheduledEvictionRepository);
    }

    private void givenCaseWithWarrantAwaitingScheduling() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1B)).thenReturn(true);
        PcsCaseEntity pcsCase = caseWithWarrant(EnforcementState.AWAITING_SCHEDULING);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCase);
    }

    private PcsCaseEntity caseWithWarrant(EnforcementState state) {
        ClaimEntity claim = ClaimEntity.builder().build();
        claim.addParty(PartyEntity.builder().build(), PartyRole.CLAIMANT);
        claim.addParty(PartyEntity.builder().build(), PartyRole.DEFENDANT);

        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .caseReference(TEST_CASE_REFERENCE)
            .propertyAddress(AddressEntity.builder().build())
            .claims(List.of(claim))
            .build();

        EnforcementOrderEntity application = EnforcementOrderEntity.builder()
            .id(APPLICATION_ID)
            .pcsCase(pcsCase)
            .state(state)
            .build();
        application.setWarrantDetails(WarrantEntity.builder()
                                          .enfApplication(application)
                                          .created(Instant.now())
                                          .build());
        pcsCase.getEnforcementOrders().add(application);
        when(enforcementOrderMediator.getEnforcementOrder(pcsCase)).thenReturn(Optional.of(application));
        return pcsCase;
    }

    private EventPayload<PCSCase, State> payload() {
        return new EventPayload<>(TEST_CASE_REFERENCE, null, null);
    }

    private SubmitResponse<State> submit(ScheduleEvictionRequest request) {
        return configuredEvent.submit(payload(), request, USER);
    }

    private List<String> startRejection() {
        return ((ExternalRejection<?>) configuredEvent.start(payload(), USER, null)).errors();
    }

    private List<String> submitRejection(ScheduleEvictionRequest request) {
        return submit(request).getErrors();
    }
}
