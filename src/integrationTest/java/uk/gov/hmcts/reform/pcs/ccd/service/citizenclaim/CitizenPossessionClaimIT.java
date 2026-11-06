package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.CitizenClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.RentHistoryLedgerEntryRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.RentHistoryPeriodRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.exception.DraftVersionConflictException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
@DisplayName("Citizen private landlord claim: create, draft, submit")
class CitizenPossessionClaimIT extends AbstractPostgresContainerIT {

    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String LONGEST_EMAIL = "x".repeat(244) + "@test.test";
    private static final String COMPLETE_CLAIM_JSON = "/citizenclaim/complete-citizen-possession-claim.json";
    private static final AddressUK PROPERTY_ADDRESS = AddressUK.builder()
        .addressLine1("1 Test Street").postTown("London").postCode("SW1A 1AA").build();

    @Autowired
    private CitizenPossessionClaimCaseService caseService;
    @Autowired
    private CitizenPossessionClaimSubmitService submitService;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private RentHistoryLedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private RentHistoryPeriodRepository periodRepository;
    @Autowired
    private CitizenClaimSubmissionRepository submissionRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("draftCaseDataObjectMapper")
    private ObjectMapper draftObjectMapper;

    @MockitoBean
    private SecurityContextService securityContextService;
    @MockitoBean
    private FeeService feeService;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private LocationReferenceService locationReferenceService;
    @MockitoBean
    private FeatureToggleService featureToggleService;
    @Autowired
    private CitizenMakePossessionClaim citizenMakePossessionClaim;

    @BeforeEach
    void setUp() {
        UserInfo citizen = UserInfo.builder().uid(USER_ID).roles(List.of("citizen")).build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(citizen);
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(securityContextService.toUserId(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(feeService.getFee(FeeType.CASE_ISSUE_FEE))
            .thenReturn(FeeDetails.builder().code("FEE0412").version(1).feeAmount(new BigDecimal("404.00")).build());
        PaymentServiceResponse serviceRequest = new PaymentServiceResponse();
        serviceRequest.setServiceRequestReference("2026-1762441234567");
        when(paymentService.createServiceRequest(any())).thenReturn(serviceRequest);
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
    }

    @Test
    @DisplayName("An unversioned draft save echoes the version it stored, and a submit echoing it is accepted")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldAcceptASubmitEchoingTheVersionAnUnversionedSaveReturned() {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        caseService.createCase(caseReference, PROPERTY_ADDRESS, CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build());
        Event<PCSCase, UserRole, State> event = configuredEvent();

        CitizenPossessionClaim answers = completeClaimOrThrow();
        answers.setDraftVersion(null);
        CaseDetails<PCSCase, State> details = CaseDetails.<PCSCase, State>builder()
            .id(caseReference)
            .data(PCSCase.builder().citizenPossessionClaim(answers).build())
            .build();
        MidEvent midEvent = event.getFields().getPagesToMidEvent().values().iterator().next();
        AboutToStartOrSubmitResponse<PCSCase, State> saved =
            (AboutToStartOrSubmitResponse<PCSCase, State>) midEvent.handle(details, null);
        assertThat(saved.getErrors()).isNullOrEmpty();
        Long echoedVersion = saved.getData().getCitizenPossessionClaim().getDraftVersion();
        assertThat(echoedVersion)
            .isEqualTo(caseService.loadDraft(caseReference).orElseThrow().getDraftVersion());

        PCSCase reviewed = PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaim.builder().draftVersion(echoedVersion).build())
            .build();
        SubmitResponse<State> submitted = event.getSubmitHandler()
            .submit(new EventPayload<>(caseReference, reviewed, null));

        assertThat(submitted.getErrors()).isNullOrEmpty();
        assertThat(submitted.getState()).isEqualTo(State.PENDING_CASE_ISSUED);
    }

    @Test
    @DisplayName("Email columns the citizen claim writes to hold an RFC 5321 length address")
    void shouldWidenEmailColumnsTo254() {
        String sql = """
            select table_name || '.' || column_name, character_maximum_length
            from information_schema.columns
            where table_schema = 'public'
              and (table_name, column_name) in (('party', 'email_address'), ('notice_of_possession', 'notice_details'))
            """;
        Map<String, Integer> widths = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            widths.put(rs.getString(1), rs.getInt(2));
        });

        assertThat(widths).containsOnly(
            Map.entry("party.email_address", 254),
            Map.entry("notice_of_possession.notice_details", 254));
    }

    private Event<PCSCase, UserRole, State> configuredEvent() {
        ConfigBuilderImpl<PCSCase, State, UserRole> configBuilder = new ConfigBuilderImpl<>(
            new ResolvedCCDConfig<>(PCSCase.class, State.class, UserRole.class, Map.of(), ImmutableSet.of()));
        citizenMakePossessionClaim.configureDecentralised(configBuilder);
        return configBuilder.build().getEvents().get(EventId.citizenMakePossessionClaim.name());
    }

    private CitizenPossessionClaim completeClaimOrThrow() {
        try {
            return completeClaim();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("A citizen's draft survives a save and load and its final submit maps onto the claim domain")
    void shouldCreateSaveDraftAndSubmit() throws Exception {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        CitizenClaimEligibility eligibility = CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build();

        caseService.createCase(caseReference, PROPERTY_ADDRESS, eligibility);
        PcsCaseEntity createdCase = caseService.loadCase(caseReference);
        assertThat(caseService.isCurrentUserClaimCreator(createdCase)).isTrue();
        CitizenPossessionClaim started = caseService.loadOrInitialiseDraft(createdCase);
        assertThat(started.getEligibility()).isEqualTo(eligibility);
        assertThat(started.getPropertyAddress().getPostCode()).isEqualTo(PROPERTY_ADDRESS.getPostCode());

        CitizenPossessionClaim answers = completeClaim();
        answers.setDraftVersion(started.getDraftVersion());
        answers.getClaimant().setEmailAddress(LONGEST_EMAIL);
        Long savedVersion = caseService.saveDraft(caseReference, answers);
        CitizenPossessionClaim draft = caseService.loadDraft(caseReference).orElseThrow();
        assertThat(draft.getDraftVersion()).isEqualTo(savedVersion).isNotEqualTo(started.getDraftVersion());
        assertThat(draft.getClaimant()).isEqualTo(answers.getClaimant());
        assertThat(draft.getPropertyAddress()).isNull();

        CitizenPossessionClaim stale = completeClaim();
        stale.setDraftVersion(started.getDraftVersion());
        assertThatThrownBy(() -> caseService.saveDraft(caseReference, stale))
            .isInstanceOf(DraftVersionConflictException.class);

        final CitizenPossessionClaimSubmitService.SubmittedClaim submitted = submitService.submit(createdCase, draft);
        caseService.deleteDraft(caseReference);
        entityManager.flush();
        entityManager.clear();

        assertThat(submitted.serviceRequestReference()).isEqualTo("2026-1762441234567");
        assertThat(caseService.loadDraft(caseReference)).isEmpty();

        PcsCaseEntity pcsCase = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();
        assertThat(pcsCase.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
        assertThat(pcsCase.getTenancyLicence().getType()).isEqualTo(CombinedLicenceType.ASSURED_TENANCY);

        ClaimEntity claim = pcsCase.getClaims().getFirst();
        assertThat(claim.getClaimantType()).isEqualTo(ClaimantType.PRIVATE_LANDLORD);
        assertThat(claim.getClaimGrounds()).extracting(ClaimGroundEntity::getCode)
            .containsExactlyInAnyOrder("GROUND_1A", "GROUND_8", "GROUND_10", "GROUND_14");
        assertThat(claim.getRentArrears().getTotalRentArrears()).isEqualByComparingTo("5000.00");
        assertThat(claim.getClaimParties()).extracting(ClaimPartyEntity::getRole).containsExactlyInAnyOrder(
            PartyRole.CLAIMANT, PartyRole.DEFENDANT, PartyRole.DEFENDANT, PartyRole.UNDERLESSEE_OR_MORTGAGEE);
        assertThat(claim.getClaimParties())
            .filteredOn(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT)
            .singleElement()
            .satisfies(claimant -> {
                assertThat(claimant.getParty().getIdamId()).isEqualTo(USER_ID);
                assertThat(claimant.getParty().getFirstName()).isEqualTo("Jane");
                assertThat(claimant.getParty().getEmailAddress()).isEqualTo(LONGEST_EMAIL);
            });
        assertThat(claim.getClaimDocuments()).hasSize(3);

        assertThat(ledgerEntryRepository.findByClaimId(claim.getId())).hasSize(2);
        assertThat(periodRepository.findByClaimId(claim.getId())).hasSize(1);
        assertThat(submissionRepository.findByClaimId(claim.getId())).singleElement()
            .satisfies(row -> assertThat(row.getSubmittedClaim()).contains("tom.tenant@example.com"));
    }

    private CitizenPossessionClaim completeClaim() throws Exception {
        try (InputStream json = getClass().getResourceAsStream(COMPLETE_CLAIM_JSON)) {
            return draftObjectMapper.readValue(json, PCSCase.class).getCitizenPossessionClaim();
        }
    }

}
