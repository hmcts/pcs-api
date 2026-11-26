package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejectionException;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
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
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.ScheduleEvictionRepository;
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
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleEvictionServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final LocalDateTime EVICTION_DATE = LocalDateTime.of(2027, 6, 25, 10, 0);
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final String NOT_AVAILABLE = "Scheduling evictions is not available";
    private static final String NO_WARRANT_AWAITING_SCHEDULING = "There is no warrant awaiting scheduling on this case";
    private static final String WARRANT_NO_LONGER_AWAITING_SCHEDULING = "This warrant is no longer awaiting scheduling";
    private static final String DATE_REQUIRED = "Enter the date and time of the eviction";
    private static final String DISPENSE_WITH_SERVICE_REQUIRED =
        "Select whether the court has given permission to dispense with service";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private ScheduleEvictionRepository scheduleEvictionRepository;
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
    private EnforcementOrderMediator enforcementOrderMediator;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private ScheduleEvictionService underTest;

    @Test
    void shouldRejectStartWhenEvictionSchedulingIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.EVICTION_SCHEDULING)).thenReturn(false);

        assertRejected(() -> underTest.getStartDetails(CASE_REFERENCE), NOT_AVAILABLE);
        verifyNoInteractions(pcsCaseService);
    }

    @Test
    void shouldRejectSubmitWhenEvictionSchedulingIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.EVICTION_SCHEDULING)).thenReturn(false);
        ScheduleEvictionRequest request = new ScheduleEvictionRequest(APPLICATION_ID, VerticalYesNo.NO, EVICTION_DATE);

        assertRejected(() -> underTest.scheduleEviction(CASE_REFERENCE, request),
                       NOT_AVAILABLE);
        verifyNoInteractions(pcsCaseService, scheduleEvictionRepository);
    }

    @Test
    void shouldRejectStartWhenNoWarrantAwaitingScheduling() {
        givenCaseWithWarrant(EnforcementState.PENDING_CASE_ISSUED);

        assertRejected(() -> underTest.getStartDetails(CASE_REFERENCE),
                       NO_WARRANT_AWAITING_SCHEDULING);
    }

    @Test
    void shouldReturnCaseAndEvictionDetailsOnStart() {
        // Given
        PcsCaseEntity pcsCase = givenCaseWithWarrant(EnforcementState.AWAITING_SCHEDULING);
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(CASE_REFERENCE)).thenReturn("1234");
        AddressUK addressUk = AddressUK.builder().build();
        when(addressMapper.toAddressUK(pcsCase.getPropertyAddress())).thenReturn(addressUk);
        when(addressFormatter.formatShortAddress(addressUk, AddressFormatter.COMMA_DELIMITER))
            .thenReturn("1 Street, W3 7RX");
        when(caseNameFormatter.formatCaseName(pcsCase)).thenReturn("Claimant v Defendant");
        when(recipientAddressResolver.resolveDisplayName(any(PartyEntity.class)))
            .thenReturn("Claimant", "Defendant");

        // When
        ScheduleEvictionStart start = underTest.getStartDetails(CASE_REFERENCE);

        // Then
        assertThat(start.enforcementApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(start.propertyAddress()).isEqualTo("1 Street, W3 7RX");
        assertThat(start.claimantNames()).containsExactly("Claimant");
        assertThat(start.defendantNames()).containsExactly("Defendant");
    }

    @Test
    void shouldSaveScheduledEvictionAgainstWarrantInUkTime() {
        // Given
        PcsCaseEntity pcsCase = givenCaseWithWarrant(EnforcementState.AWAITING_SCHEDULING);
        EnforcementOrderEntity application = pcsCase.getEnforcementOrders().iterator().next();

        // When
        underTest.scheduleEviction(CASE_REFERENCE,
                                   new ScheduleEvictionRequest(APPLICATION_ID, VerticalYesNo.YES, EVICTION_DATE));

        // Then
        ArgumentCaptor<ScheduledEvictionEntity> saved = ArgumentCaptor.forClass(ScheduledEvictionEntity.class);
        verify(scheduleEvictionRepository).save(saved.capture());
        assertThat(saved.getValue().getEnfWarrant()).isSameAs(application.getWarrantDetails());
        assertThat(saved.getValue().getCourtGivenPermissionToDispenseWithService()).isEqualTo(VerticalYesNo.YES);
        assertThat(saved.getValue().getScheduledEvictionDate()).isEqualTo(Instant.parse("2027-06-25T09:00:00Z"));
        assertThat(application.getState()).isEqualTo(EnforcementState.AWAITING_PROPERTY_VISIT);
    }

    @Test
    void shouldRejectSubmitWhenNoWarrantAwaitingScheduling() {
        givenCaseWithWarrant(EnforcementState.AWAITING_PROPERTY_VISIT);

        ScheduleEvictionRequest request = new ScheduleEvictionRequest(APPLICATION_ID, VerticalYesNo.NO, EVICTION_DATE);

        assertRejected(() -> underTest.scheduleEviction(CASE_REFERENCE, request),
                       NO_WARRANT_AWAITING_SCHEDULING);
        verifyNoInteractions(scheduleEvictionRepository);
    }

    @Test
    void shouldRejectSubmitWithoutEvictionDate() {
        givenCaseWithWarrant(EnforcementState.AWAITING_SCHEDULING);

        assertRejected(() -> underTest.scheduleEviction(
                           CASE_REFERENCE, new ScheduleEvictionRequest(APPLICATION_ID, VerticalYesNo.NO, null)),
                       DATE_REQUIRED);
        verifyNoInteractions(scheduleEvictionRepository);
    }

    @Test
    void shouldRejectSubmitWithoutDispenseWithServiceAnswer() {
        givenCaseWithWarrant(EnforcementState.AWAITING_SCHEDULING);

        assertRejected(() -> underTest.scheduleEviction(
                           CASE_REFERENCE, new ScheduleEvictionRequest(APPLICATION_ID, null, EVICTION_DATE)),
                       DISPENSE_WITH_SERVICE_REQUIRED);
        verifyNoInteractions(scheduleEvictionRepository);
    }

    @Test
    void shouldRejectSubmitForAnotherWarrant() {
        PcsCaseEntity pcsCase = givenCaseWithWarrant(EnforcementState.AWAITING_SCHEDULING);

        assertRejected(() -> underTest.scheduleEviction(
                           CASE_REFERENCE, new ScheduleEvictionRequest(UUID.randomUUID(), VerticalYesNo.NO,
                                                                       EVICTION_DATE)),
                       WARRANT_NO_LONGER_AWAITING_SCHEDULING);
        verifyNoInteractions(scheduleEvictionRepository);
        assertThat(pcsCase.getEnforcementOrders().iterator().next().getState())
            .isEqualTo(EnforcementState.AWAITING_SCHEDULING);
    }

    private PcsCaseEntity givenCaseWithWarrant(EnforcementState state) {
        when(featureToggleService.isEnabled(FeatureFlag.EVICTION_SCHEDULING)).thenReturn(true);
        ClaimEntity claim = ClaimEntity.builder().build();
        claim.addParty(PartyEntity.builder().build(), PartyRole.CLAIMANT);
        claim.addParty(PartyEntity.builder().build(), PartyRole.DEFENDANT);

        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
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
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
        when(enforcementOrderMediator.getEnforcementOrder(pcsCase)).thenReturn(Optional.of(application));
        return pcsCase;
    }

    private static void assertRejected(Runnable call, String error) {
        ExternalRejectionException rejection = catchThrowableOfType(ExternalRejectionException.class, call::run);
        assertThat(rejection).isNotNull();
        assertThat(rejection.errors()).containsExactly(error);
    }
}
