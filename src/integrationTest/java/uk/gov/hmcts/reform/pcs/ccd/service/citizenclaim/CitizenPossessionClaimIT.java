package uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim;

import com.fasterxml.jackson.databind.JsonNode;
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
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimAdditionalClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimEligibility;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimNoticeMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentPeriodEntry;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimRentProofType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimTenancyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimWalesGroundReason;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimantKind;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.ClaimantOrgNameListCreator;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeServiceMethodEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.citizenclaim.RentHistoryPeriodEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.PackRecipientResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ResolvedRecipient;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.CitizenClaimSubmissionRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.citizenclaim.RentHistoryPeriodRepository;
import uk.gov.hmcts.reform.pcs.ccd.view.CaseListView;
import uk.gov.hmcts.reform.pcs.ccd.view.PartiesView;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.exception.DraftVersionConflictException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
    private CitizenPossessionClaimApplicableAnswers applicableAnswers;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private DraftCaseDataService draftCaseDataService;
    @Autowired
    private RentHistoryPeriodRepository periodRepository;
    @Autowired
    private CitizenClaimSubmissionRepository submissionRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PartiesView partiesView;
    @Autowired
    private CaseListView caseListView;
    @Autowired
    private ClaimantInformationTabDetailsBuilder claimantInformationTabDetailsBuilder;
    @Autowired
    private ClaimantOrgNameListCreator claimantOrgNameListCreator;
    @Autowired
    private PackRecipientResolver packRecipientResolver;
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
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build(), LegislativeCountry.ENGLAND);
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
    @DisplayName("Confirmed sections survive a draft save and reload, and an emptied set saves as empty")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldRoundTripConfirmedSectionsThroughTheDraftSave() {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        caseService.createCase(caseReference, PROPERTY_ADDRESS, CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build(), LegislativeCountry.ENGLAND);
        MidEvent midEvent = configuredEvent().getFields().getPagesToMidEvent().values().iterator().next();

        CitizenPossessionClaim started = caseService.loadOrInitialiseDraft(caseService.loadCase(caseReference));
        CitizenPossessionClaim confirmed = completeClaimOrThrow();
        confirmed.setDraftVersion(started.getDraftVersion());
        confirmed.setConfirmedSections(Set.of(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY));
        AboutToStartOrSubmitResponse<PCSCase, State> saved = (AboutToStartOrSubmitResponse<PCSCase, State>)
            midEvent.handle(draftSaveDetails(caseReference, confirmed), null);

        assertThat(saved.getErrors()).isNullOrEmpty();
        assertThat(saved.getData().getCitizenPossessionClaim().getConfirmedSections())
            .containsExactlyInAnyOrder(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY);
        CitizenPossessionClaim reloaded = caseService.loadOrInitialiseDraft(caseService.loadCase(caseReference));
        assertThat(reloaded.getConfirmedSections())
            .containsExactlyInAnyOrder(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY);
        assertThat(draftJson(caseReference).at("/citizenPossessionClaim/confirmedSections"))
            .extracting(JsonNode::asText)
            .containsExactlyInAnyOrder("claimant-cya", "tenancy-cya");

        CitizenPossessionClaim emptied = reloaded.toBuilder().confirmedSections(Set.of()).build();
        AboutToStartOrSubmitResponse<PCSCase, State> cleared = (AboutToStartOrSubmitResponse<PCSCase, State>)
            midEvent.handle(draftSaveDetails(caseReference, emptied), null);

        assertThat(cleared.getErrors()).isNullOrEmpty();
        assertThat(caseService.loadDraft(caseReference).orElseThrow().getConfirmedSections()).isEmpty();
        assertThat(draftJson(caseReference).at("/citizenPossessionClaim/confirmedSections").isArray()).isTrue();
    }

    @Test
    @DisplayName("A draft patch leaves confirmed sections it does not mention alone and clears them when sent empty")
    void shouldPatchConfirmedSectionsOnlyWhenSent() {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        caseService.createCase(caseReference, PROPERTY_ADDRESS, CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build(), LegislativeCountry.ENGLAND);

        draftCaseDataService.patchUnsubmittedEventData(caseReference, draftWithSections(
            Set.of(CitizenClaimSection.CLAIMANT)), EventId.citizenMakePossessionClaim);
        draftCaseDataService.patchUnsubmittedEventData(caseReference, draftWithSections(null),
                                                       EventId.citizenMakePossessionClaim);
        assertThat(caseService.loadDraft(caseReference).orElseThrow().getConfirmedSections())
            .containsExactly(CitizenClaimSection.CLAIMANT);

        draftCaseDataService.patchUnsubmittedEventData(caseReference, draftWithSections(Set.of()),
                                                       EventId.citizenMakePossessionClaim);
        assertThat(caseService.loadDraft(caseReference).orElseThrow().getConfirmedSections()).isEmpty();
    }

    private static CaseDetails<PCSCase, State> draftSaveDetails(long caseReference, CitizenPossessionClaim claim) {
        return CaseDetails.<PCSCase, State>builder()
            .id(caseReference)
            .data(PCSCase.builder().citizenPossessionClaim(claim).build())
            .build();
    }

    private static PCSCase draftWithSections(Set<CitizenClaimSection> sections) {
        return PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaim.builder().confirmedSections(sections).build())
            .build();
    }

    private JsonNode draftJson(long caseReference) {
        String caseData = jdbcTemplate.queryForObject(
            "select case_data::text from draft.draft_case_data where case_reference = ? and event_id = ?",
            String.class, caseReference, EventId.citizenMakePossessionClaim.name());
        try {
            return draftObjectMapper.readTree(caseData);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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

        caseService.createCase(caseReference, PROPERTY_ADDRESS, eligibility, LegislativeCountry.ENGLAND);
        PcsCaseEntity createdCase = caseService.loadCase(caseReference);
        assertThat(caseService.isCurrentUserClaimCreator(createdCase)).isTrue();
        CitizenPossessionClaim started = caseService.loadOrInitialiseDraft(createdCase);
        assertThat(started.getEligibility())
            .isEqualTo(eligibility.toBuilder().legislativeCountry(LegislativeCountry.ENGLAND).build());
        assertThat(started.getPropertyAddress().getPostCode()).isEqualTo(PROPERTY_ADDRESS.getPostCode());

        CitizenPossessionClaim answers = completeClaim();
        answers.setDraftVersion(started.getDraftVersion());
        answers.getClaimant().setEmailAddress(LONGEST_EMAIL);
        answers.setConfirmedSections(Set.of(CitizenClaimSection.CLAIMANT));
        Long savedVersion = caseService.saveDraft(caseReference, answers);
        CitizenPossessionClaim draft = caseService.loadDraft(caseReference).orElseThrow();
        assertThat(draft.getDraftVersion()).isEqualTo(savedVersion).isNotEqualTo(started.getDraftVersion());
        assertThat(draft.getClaimant()).isEqualTo(answers.getClaimant());
        assertThat(draft.getPropertyAddress()).isNull();

        CitizenPossessionClaim stale = completeClaim();
        stale.setDraftVersion(started.getDraftVersion());
        assertThatThrownBy(() -> caseService.saveDraft(caseReference, stale))
            .isInstanceOf(DraftVersionConflictException.class);

        final CitizenPossessionClaimSubmitService.SubmittedClaim submitted =
            submitService.submit(createdCase, draft, applicableAnswers.of(draft));
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
            PartyRole.CLAIMANT, PartyRole.DEFENDANT, PartyRole.DEFENDANT);
        assertThat(claim.getUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getNoticeOfPossession().getNoticeDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(claim.getClaimParties())
            .filteredOn(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT)
            .singleElement()
            .satisfies(claimant -> {
                assertThat(claimant.getParty().getIdamId()).isEqualTo(USER_ID);
                assertThat(claimant.getParty().getFirstName()).isEqualTo("Jane");
                assertThat(claimant.getParty().getEmailAddress()).isEqualTo(LONGEST_EMAIL);
            });
        assertThat(claim.getClaimDocuments()).hasSize(3);

        PCSCase viewed = PCSCase.builder().build();
        partiesView.setCaseFields(viewed, pcsCase);
        caseListView.setCaseFields(viewed);
        assertThat(viewed.getClaimantNames()).isEqualTo("Jane Landlord");
        assertThat(claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(viewed).getClaimantName())
            .isEqualTo("Jane Landlord");
        assertThat(claimantOrgNameListCreator.createClaimantOrgNameList(viewed))
            .extracting(ListValue::getValue)
            .containsExactly("Jane Landlord");

        assertThat(periodRepository.findByClaimId(claim.getId()))
            .extracting(RentHistoryPeriodEntity::getRank, RentHistoryPeriodEntity::getPeriodStart)
            .containsExactlyInAnyOrder(
                tuple(1, LocalDate.of(2026, 9, 1)),
                tuple(2, LocalDate.of(2026, 9, 1)),
                tuple(3, LocalDate.of(2026, 10, 1)));
        assertThat(pcsCase.getTenancyLicence().getRentDueDay()).isEqualTo(1);
        assertThat(submissionRepository.findByClaimId(claim.getId())).singleElement()
            .satisfies(row -> assertThat(row.getSubmittedClaim())
                .contains("tom.tenant@example.com")
                .doesNotContain("confirmedSections"));
    }

    @Test
    @DisplayName("An issued citizen claim posts each defendant the claim form, the uploads and their access code, "
        + "and posts the claimant nothing")
    void shouldPostTheCitizenClaimPackToDefendantsOnly() throws Exception {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        caseService.createCase(caseReference, PROPERTY_ADDRESS, CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build(), LegislativeCountry.ENGLAND);
        CitizenPossessionClaim answers = completeClaim();
        submitService.submit(caseService.loadCase(caseReference), answers, applicableAnswers.of(answers));

        PcsCaseEntity submittedCase = caseService.loadCase(caseReference);
        ClaimEntity submittedClaim = submittedCase.getClaims().getFirst();
        submittedClaim.setClaimFormDocument(packDocument(submittedCase, DocumentType.CLAIM, null));
        List<PartyEntity> defendants = submittedClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .map(ClaimPartyEntity::getParty)
            .toList();
        defendants.forEach(defendant ->
            submittedCase.addDocument(packDocument(submittedCase, DocumentType.DEFENDANT_ACCESS_CODE, defendant)));
        pcsCaseRepository.saveAndFlush(submittedCase);
        entityManager.clear();

        List<ResolvedRecipient> recipients = packRecipientResolver.resolveClaimRecipients(submittedCase.getId());

        assertThat(recipients).hasSize(defendants.size()).allSatisfy(recipient -> {
            assertThat(recipient.letterType()).isEqualTo(LetterType.DEFENDANT_CLAIM_PACK);
            assertThat(recipient.includesHlpasLeaflet()).isTrue();
            assertThat(recipient.documents()).extracting(DocumentEntity::getType).containsExactly(
                DocumentType.CLAIM, DocumentType.TENANCY_AGREEMENT, DocumentType.POSSESSION_NOTICE,
                DocumentType.OTHER, DocumentType.DEFENDANT_ACCESS_CODE);
            assertThat(recipient.documents().getLast().getParty().getId()).isEqualTo(recipient.recipient().getId());
        });
        assertThat(recipients).extracting(recipient -> recipient.recipient().getId())
            .containsExactlyInAnyOrderElementsOf(defendants.stream().map(PartyEntity::getId).toList());
    }

    private static DocumentEntity packDocument(PcsCaseEntity pcsCase, DocumentType type, PartyEntity party) {
        return DocumentEntity.builder()
            .pcsCase(pcsCase)
            .party(party)
            .type(type)
            .documentId(UUID.randomUUID())
            .fileName(type.name() + ".pdf")
            .build();
    }

    private CitizenPossessionClaim completeClaim() throws Exception {
        try (InputStream json = getClass().getResourceAsStream(COMPLETE_CLAIM_JSON)) {
            return draftObjectMapper.readValue(json, PCSCase.class).getCitizenPossessionClaim();
        }
    }

    @Test
    @DisplayName("A submit records every notice method and the rent answers in their own columns")
    void shouldWriteTheNoticeMethodsAndRentAnswersToTheirColumns() throws Exception {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        caseService.createCase(caseReference, PROPERTY_ADDRESS,
                               CitizenClaimEligibility.builder().claimantType(ClaimantType.PRIVATE_LANDLORD).build(),
                               LegislativeCountry.ENGLAND);

        CitizenPossessionClaim answers = completeClaim();
        answers.getNotice().setNoticeMethods(Set.of(CitizenClaimNoticeMethod.EMAIL,
                                                    CitizenClaimNoticeMethod.PERSONALLY_HANDED));
        answers.getNotice().setHandedToName("Tom Tenant");
        answers.getNotice().setHandedDate(LocalDate.of(2026, 8, 2));
        answers.getRentArrears().setProofType(CitizenClaimRentProofType.TABLE);
        answers.getRentArrears().setRentHistoryTable(List.of(ListValue.<CitizenClaimRentPeriodEntry>builder()
            .value(CitizenClaimRentPeriodEntry.builder()
                       .periodStart(LocalDate.of(2026, 9, 1))
                       .dateDue(LocalDate.of(2026, 9, 15))
                       .amountDue(new BigDecimal("1250.00"))
                       .build())
            .build()));
        answers.getRentArrears().setRentDueDay(15);

        PcsCaseEntity createdCase = caseService.loadCase(caseReference);
        submitService.submit(createdCase, answers, applicableAnswers.of(answers));
        entityManager.flush();
        entityManager.clear();

        UUID claimId = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow()
            .getClaims().getFirst().getId();
        Map<String, Object> notice = jdbcTemplate.queryForMap(
            "SELECT id, serving_method, notice_date, notice_details FROM notice_of_possession WHERE claim_id = ?",
            claimId);
        assertThat(notice.get("serving_method")).isEqualTo("PERSONALLY_HANDED");
        assertThat(notice.get("notice_date")).hasToString("2026-08-02");
        assertThat(notice.get("notice_details")).isEqualTo("Tom Tenant");

        List<Map<String, Object>> methods = jdbcTemplate.queryForList(
            "SELECT method, served_date, details, display_order FROM notice_service_method "
                + "WHERE notice_of_possession_id = ? ORDER BY display_order", notice.get("id"));
        assertThat(methods).extracting(row -> row.get("method"), row -> String.valueOf(row.get("served_date")),
                                       row -> row.get("details"), row -> row.get("display_order"))
            .containsExactly(
                tuple("PERSONALLY_HANDED", "2026-08-02", "Tom Tenant", 1),
                tuple("EMAIL", "2026-08-01", "tom.tenant@example.com", 2));

        Map<String, Object> rentArrears = jdbcTemplate.queryForMap(
            "SELECT arrears_at_notice_date, total_rent_arrears_date, proof_type FROM rent_arrears WHERE claim_id = ?",
            claimId);
        assertThat((BigDecimal) rentArrears.get("arrears_at_notice_date")).isEqualByComparingTo("3750.00");
        assertThat(rentArrears.get("total_rent_arrears_date")).hasToString("2026-11-01");
        assertThat(rentArrears.get("proof_type")).isEqualTo("TABLE");

        Integer rentDueDay = jdbcTemplate.queryForObject(
            "SELECT t.rent_due_day FROM tenancy_licence t JOIN pcs_case c ON c.id = t.case_id "
                + "WHERE c.case_reference = ?", Integer.class, caseReference);
        assertThat(rentDueDay).isEqualTo(15);

        PcsCaseEntity reloaded = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();
        assertThat(reloaded.getClaims().getFirst().getNoticeOfPossession().getServiceMethods())
            .extracting(NoticeServiceMethodEntity::getMethod)
            .containsExactly(NoticeServiceMethod.PERSONALLY_HANDED, NoticeServiceMethod.EMAIL);
        assertThat(reloaded.getTenancyLicence().getRentDueDay()).isEqualTo(15);
    }


    @Test
    @DisplayName("A claim with another claimant gives the claim two claimant parties, the claimant first")
    void shouldSubmitAClaimWithAnAdditionalClaimant() throws Exception {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        caseService.createCase(caseReference, PROPERTY_ADDRESS,
                               CitizenClaimEligibility.builder().claimantType(ClaimantType.PRIVATE_LANDLORD).build(),
                               LegislativeCountry.ENGLAND);
        CitizenPossessionClaim answers = completeClaim();
        answers.getClaimant().setMultipleClaimants(VerticalYesNo.YES);
        answers.setAdditionalClaimants(List.of(ListValue.<CitizenClaimAdditionalClaimant>builder()
            .id(UUID.randomUUID().toString())
            .value(CitizenClaimAdditionalClaimant.builder()
                       .individualOrOrganisation(CitizenClaimantKind.INDIVIDUAL)
                       .firstName("Sam").lastName("Sharer")
                       .address(PROPERTY_ADDRESS)
                       .contactByPhone(VerticalYesNo.NO).contactByText(VerticalYesNo.NO)
                       .build())
            .build()));

        PcsCaseEntity createdCase = caseService.loadCase(caseReference);
        submitService.submit(createdCase, answers, applicableAnswers.of(answers));
        entityManager.flush();
        entityManager.clear();

        ClaimEntity claim = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow()
            .getClaims().getFirst();
        assertThat(claim.getClaimParties())
            .filteredOn(party -> party.getRole() == PartyRole.CLAIMANT)
            .extracting(party -> party.getParty().getOrgName())
            .containsExactlyInAnyOrder("Jane Landlord", "Sam Sharer");
    }

    @Test
    @DisplayName("A claim on an excluded tenancy files its grounds as the introductory, demoted and other journey does")
    void shouldSubmitAnExcludedTenancyClaimWithAndWithoutGrounds() throws Exception {
        long withGrounds = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        long withoutGrounds = withGrounds + 1;
        for (long caseReference : new long[] {withGrounds, withoutGrounds}) {
            caseService.createCase(caseReference, PROPERTY_ADDRESS,
                                   CitizenClaimEligibility.builder().claimantType(ClaimantType.PRIVATE_LANDLORD)
                                       .build(),
                                   LegislativeCountry.ENGLAND);
            CitizenPossessionClaim answers = completeClaim();
            answers.getTenancy().setTenancyType(CitizenClaimTenancyType.EXCLUDED);
            boolean hasGrounds = caseReference == withGrounds;
            answers.getGrounds().setHasExcludedGrounds(VerticalYesNo.from(hasGrounds));
            answers.getGrounds().setExcludedGrounds(hasGrounds
                ? Set.of(IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS) : null);
            answers.getGrounds().setExcludedReason("The occupier is a lodger");

            PcsCaseEntity createdCase = caseService.loadCase(caseReference);
            submitService.submit(createdCase, answers, applicableAnswers.of(answers));
            entityManager.flush();
            entityManager.clear();
        }

        assertThat(pcsCaseRepository.findByCaseReference(withGrounds).orElseThrow()
                       .getClaims().getFirst().getClaimGrounds())
            .extracting(ClaimGroundEntity::getCategory, ClaimGroundEntity::getCode, ClaimGroundEntity::getReason)
            .containsExactly(tuple(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER, "ABSOLUTE_GROUNDS",
                                   "The occupier is a lodger"));
        assertThat(pcsCaseRepository.findByCaseReference(withoutGrounds).orElseThrow()
                       .getClaims().getFirst().getClaimGrounds())
            .extracting(ClaimGroundEntity::getCategory, ClaimGroundEntity::getCode, ClaimGroundEntity::getReason)
            .containsExactly(tuple(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS, "NO_GROUNDS",
                                   "The occupier is a lodger"));
    }

    @Test
    @DisplayName("A claim for a property in Wales maps its occupation contract onto the Wales tenancy and documents")
    void shouldSubmitAWelshClaimWithItsOccupationContract() throws Exception {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        caseService.createCase(caseReference, PROPERTY_ADDRESS, CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build(), LegislativeCountry.WALES);
        PcsCaseEntity createdCase = caseService.loadCase(caseReference);
        assertThat(caseService.loadOrInitialiseDraft(createdCase).getEligibility().getLegislativeCountry())
            .isEqualTo(LegislativeCountry.WALES);

        CitizenPossessionClaim answers =
            CitizenPossessionClaimCaseService.withCaseCountry(completeClaim(), createdCase);
        CitizenClaimTenancy tenancy = answers.getTenancy();
        tenancy.setOccupationContractType(OccupationLicenceTypeWales.OTHER);
        tenancy.setOtherOccupationContractDetails("Supported standard contract");
        tenancy.setHasEnergyPerformanceCertificate(VerticalYesNo.YES);
        tenancy.setEnergyPerformanceDocuments(List.of(requiredDocument("epc.pdf")));
        tenancy.setHasGasSafetyReport(VerticalYesNo.NO);
        tenancy.setNoGasSafetyReportReason("The property has no gas supply");
        tenancy.setHasElectricalInstallationConditionReport(VerticalYesNo.YES);
        tenancy.setElectricalInstallationDocuments(List.of(requiredDocument("eicr.pdf")));
        CitizenClaimGrounds grounds = answers.getGrounds();
        grounds.setHasRentArrearsGrounds(VerticalYesNo.YES);
        grounds.setRentArrearsGrounds(null);
        grounds.setHasOtherGrounds(null);
        grounds.setGroundReasons(null);
        grounds.setWalesDiscretionaryGrounds(Set.of(DiscretionaryGroundWales.RENT_ARREARS_S157,
                                                    DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160));
        grounds.setWalesMandatoryGrounds(Set.of(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199));
        grounds.setOtherGrounds(Set.of(PrivateLandlordAssuredGround.OTHER));
        grounds.setOtherGroundsDetails("Another reason");
        grounds.setWalesGroundReasons(List.of(
            walesReason(DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160.name(), "Building works planned"),
            walesReason(MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199.name(), "Break clause used")));
        CitizenPossessionClaim applicable = applicableAnswers.of(answers);
        assertThat(applicable.getTenancy().getTenancyType()).isNull();

        submitService.submit(createdCase, answers, applicable);
        entityManager.flush();
        entityManager.clear();

        PcsCaseEntity pcsCase = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();
        assertThat(pcsCase.getLegislativeCountry()).isEqualTo(LegislativeCountry.WALES);
        assertThat(pcsCase.getTenancyLicence().getType()).isEqualTo(CombinedLicenceType.OTHER);
        assertThat(pcsCase.getTenancyLicence().getOtherTypeDetails()).isEqualTo("Supported standard contract");
        assertThat(pcsCase.getTenancyLicence().getStartDate()).isEqualTo(LocalDate.of(2023, 4, 1));
        assertThat(pcsCase.getTenancyLicence().getHasCopyOfTenancyLicence()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getClaims().getFirst().getClaimDocuments())
            .extracting(claimDocument -> claimDocument.getDocument().getType())
            .contains(DocumentType.OCCUPATION_LICENCE, DocumentType.ENERGY_PERFORMANCE_CERTIFICATE,
                      DocumentType.EICR_REPORT)
            .doesNotContain(DocumentType.TENANCY_AGREEMENT, DocumentType.GAS_SAFETY_CERTIFICATE);

        ClaimEntity claim = pcsCase.getClaims().getFirst();
        assertThat(claim.getEnergyPerformanceCertificateProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(claim.getGasSafetyReportProvided()).isEqualTo(VerticalYesNo.NO);
        assertThat(claim.getNoGasSafetyReportReason()).isEqualTo("The property has no gas supply");
        assertThat(claim.getElectricalInstallationConditionProvided()).isEqualTo(VerticalYesNo.YES);
        assertThat(claim.getClaimGrounds())
            .extracting(ClaimGroundEntity::getCode, ClaimGroundEntity::getReason, ClaimGroundEntity::getIsRentArrears)
            .containsExactlyInAnyOrder(
                tuple("RENT_ARREARS_S157", null, true),
                tuple("ESTATE_MANAGEMENT_GROUNDS_S160", null, false),
                tuple("OTHER_ESTATE_MANAGEMENT_REASONS", "Building works planned", false),
                tuple("LANDLORD_BREAK_CLAUSE_S199", "Break clause used", false),
                tuple("OTHER", null, false));
        assertThat(claim.getRentArrears()).isNotNull();
    }

    private static ListValue<Document> requiredDocument(String filename) {
        return ListValue.<Document>builder()
            .value(Document.builder()
                       .url("http://dm-store/documents/" + UUID.randomUUID())
                       .binaryUrl("http://dm-store/documents/" + UUID.randomUUID() + "/binary")
                       .filename(filename)
                       .build())
            .build();
    }

    private static ListValue<CitizenClaimWalesGroundReason> walesReason(String ground, String reason) {
        return ListValue.<CitizenClaimWalesGroundReason>builder()
            .value(CitizenClaimWalesGroundReason.builder().ground(ground).reason(reason).build())
            .build();
    }

    @Test
    @DisplayName("A claim for a property whose court is not in the pilot is given that court and its region")
    void shouldGiveAClaimOutsideThePilotItsCourtAndRegion() throws Exception {
        long caseReference = 1_700_000_000_000_000L + (System.nanoTime() % 1_000_000_000L);
        AddressUK nonPilotProperty = AddressUK.builder()
            .addressLine1("1 Oxford Road").postTown("Manchester").postCode("M13 9PL").build();
        assertThat(caseService.checkProperty(nonPilotProperty, null))
            .isEqualTo(CitizenPropertyCheck.accepted(LegislativeCountry.ENGLAND));
        caseService.createCase(caseReference, nonPilotProperty, CitizenClaimEligibility.builder()
            .claimantType(ClaimantType.PRIVATE_LANDLORD).build(), LegislativeCountry.ENGLAND);
        when(locationReferenceService.getCourtVenues(List.of(144641)))
            .thenReturn(List.of(draftObjectMapper.readValue("{\"region_id\": \"4\"}", CourtVenue.class)));

        CitizenPossessionClaim answers = completeClaim();
        submitService.submit(caseService.loadCase(caseReference), answers, applicableAnswers.of(answers));
        entityManager.flush();
        entityManager.clear();

        PcsCaseEntity pcsCase = pcsCaseRepository.findByCaseReference(caseReference).orElseThrow();
        assertThat(pcsCase.getCaseManagementLocation()).isEqualTo(144641);
        assertThat(pcsCase.getBaseLocation()).isEqualTo(144641);
        assertThat(pcsCase.getRegionId()).isEqualTo(4);
    }

}
