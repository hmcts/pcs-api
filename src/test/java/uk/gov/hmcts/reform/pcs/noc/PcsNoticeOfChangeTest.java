package uk.gov.hmcts.reform.pcs.noc;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.AnswerBuilder;
import uk.gov.hmcts.ccd.sdk.api.ChallengeQuestion;
import uk.gov.hmcts.ccd.sdk.api.ChallengeQuestionField;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.NoticeOfChange;
import uk.gov.hmcts.ccd.sdk.api.TypedPropertyGetter;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswer;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswersRequest;
import uk.gov.hmcts.ccd.sdk.api.noc.NocAnswersResponse;
import uk.gov.hmcts.ccd.sdk.api.noc.NocError;
import uk.gov.hmcts.ccd.sdk.api.noc.NocOrganisation;
import uk.gov.hmcts.ccd.sdk.api.noc.NocSubmissionResponse;
import uk.gov.hmcts.ccd.sdk.api.noc.NocSubmitContext;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeRepository;
import uk.gov.hmcts.reform.pcs.ccd.task.NocAccessChangeTaskComponent;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;

@ExtendWith(MockitoExtension.class)
public class PcsNoticeOfChangeTest {

    private static final long TEST_CASE_REFERENCE = 1L;

    private static final String FEATURE_FLAG_DISABLED_CODE = "feature-disabled";

    private static final String FEATURE_FLAG_DISABLED_MESSAGE = "The Notice of change feature is "
        + "currently disabled";

    private static final String DUPLICATE_DEFENDANT_NAME_CODE = "duplicateDefendantName";

    private static final String DUPLICATE_DEFENDANT_NAME_MESSAGE = "A notice of change cannot be completed for this "
        + "defendant as there is more than one defendant with the same name on this case."
        + " Contact the issuing court for help.";

    private static final String ORG_ALREADY_REPRESENTS_PARTY_MESSAGE = "Your organisation already has access"
        + " to this case. "
        + "You or a colleague are already representing this client on this case."
        + " Contact the issuing court for help.";

    private static final String ORG_ALREADY_REPRESENTS_PARTY_CODE = "organisationAlreadyRepresents";


    private PcsNoticeOfChange pcsNoticeOfChange;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @Mock
    private LegalRepresentativeRepository legalRepresentativeRepository;

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    @Mock
    private SchedulerClient schedulerClient;

    @Mock
    private NocSubmitContext nocSubmitContext;

    @Mock
    private OrganisationDetailsResponse organisationDetailsResponse;

    @Mock
    private FeatureToggleService featureToggleService;

    @BeforeEach
    void setUp() {
        pcsNoticeOfChange = new PcsNoticeOfChange(pcsCaseRepository, legalRepresentativeRepository,
                                                  organisationDetailsService, schedulerClient, featureToggleService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void configure_CreatesChallengeQuestions() {
        // given
        ConfigBuilder<PCSCase, State, UserRole> configBuilder =
            (ConfigBuilder<PCSCase, State, UserRole>) mock(ConfigBuilder.class);
        var noticeOfChangeBuilder = mock(NoticeOfChange.NoticeOfChangeBuilder.class);
        var challengeQuestionBuilder = mock(ChallengeQuestion.ChallengeBuilder.class);
        var challengeQuestionFieldBuilder = mock(ChallengeQuestionField.QuestionBuilder.class);
        var answerBuilder = mock(AnswerBuilder.class);

        when(configBuilder.noticeOfChange()).thenReturn(noticeOfChangeBuilder);
        when(noticeOfChangeBuilder.challenge(anyString())).thenReturn(challengeQuestionBuilder);
        when(noticeOfChangeBuilder.validate(any())).thenReturn(noticeOfChangeBuilder);
        when(noticeOfChangeBuilder.submit(any())).thenReturn(noticeOfChangeBuilder);

        when(challengeQuestionBuilder.question(anyString(), anyString())).thenReturn(challengeQuestionFieldBuilder);
        when(challengeQuestionFieldBuilder.answer(any(UserRole.class))).thenReturn(answerBuilder);
        when(answerBuilder.complex(any())).thenReturn(answerBuilder);
        when(answerBuilder.field(any(TypedPropertyGetter.class))).thenReturn(challengeQuestionFieldBuilder);
        when(challengeQuestionFieldBuilder.done()).thenReturn(challengeQuestionBuilder);

        // when
        pcsNoticeOfChange.configure(configBuilder);

        // then
        verify(challengeQuestionBuilder).question("pcs-defendant-first-name", "Enter client first name");
        verify(challengeQuestionBuilder).question("pcs-defendant-last-name", "Enter client last name");

        verify(challengeQuestionFieldBuilder, times(2)).answer(UserRole.DEFENDANT_SOLICITOR);
    }

    @Test
    void validate_WithRelease1_2DAndRespondToClaimLrDisabled_ReturnErrorAnswerResponse() {
        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, null);

        // then
        assertEquals(FEATURE_FLAG_DISABLED_CODE, actual.code());
        assertEquals(FEATURE_FLAG_DISABLED_MESSAGE, actual.message());
    }

    @Test
    void validate_WithRelease1_2DEnabledAndRespondToClaimLrDisabled_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, null);

        // then
        assertEquals(FEATURE_FLAG_DISABLED_CODE, actual.code());
        assertEquals(FEATURE_FLAG_DISABLED_MESSAGE, actual.message());
    }

    @Test
    void validate_WithRelease1_2Disabled_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, null);

        // then
        assertEquals(NocAnswersResponse.answersEmpty(), actual);
    }

    @Test
    void validate_WithNullAnswerRequest_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, null);

        // then
        assertEquals(NocAnswersResponse.answersEmpty(), actual);
    }

    @Test
    void validate_WithNullAnswers_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        // given
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, null);

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocAnswersResponse.answersEmpty(), actual);
    }

    @Test
    void validate_WithEmptyAnswers_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, Collections.emptyList());

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocError.ANSWERS_EMPTY.code(), actual.code());
        assertEquals(NocError.ANSWERS_EMPTY.message(), actual.message());
    }

    @Test
    void validate_WithAnswersExceedingLimit_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        NocAnswer answer = new NocAnswer("", "");
        NocAnswer answer2 = new NocAnswer("", "");
        NocAnswer answer3 = new NocAnswer("", "");
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE,
                                                                    List.of(answer, answer2, answer3));

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocError.ANSWERS_MISMATCH_QUESTIONS.code(), actual.code());
        assertEquals("The number of provided answers must match the number of questions "
                         + "- expected 2 answers, received 3", actual.message());
    }

    @Test
    void validate_WithNoAnswersForFirstQuestion_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        NocAnswer answer = new NocAnswer("", "");
        NocAnswer answer2 = new NocAnswer("", "");
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocError.NO_ANSWER_PROVIDED_FOR_QUESTION.code(), actual.code());
        assertEquals("No answer has been provided for question ID 'pcs-defendant-first-name'",
                     actual.message());
    }

    @Test
    void validate_WithNoAnswersForSecondQuestion_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", "");
        NocAnswer answer2 = new NocAnswer("", "");
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocError.NO_ANSWER_PROVIDED_FOR_QUESTION.code(), actual.code());
        assertEquals("No answer has been provided for question ID 'pcs-defendant-last-name'",
                     actual.message());
    }

    @Test
    void validate_WithCaseNotFound_ReturnException() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", "");
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", "");
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));

        // when / then
        assertThatThrownBy(() -> pcsNoticeOfChange.validate(nocSubmitContext, nocAnswersRequest))
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage("No case found with reference %s", TEST_CASE_REFERENCE);
    }

    @Test
    void validate_WithNoPartiesOnCase_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", "");
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", "");
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT.code(), actual.code());
        assertEquals(NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT.message(), actual.message());
    }

    @Test
    void validate_WithNoDefendantsOnCase_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", "");
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", "");
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        PartyEntity party = PartyEntity.builder()
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.CLAIMANT)
                                     .build()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(Set.of(party))
            .build();
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT.code(), actual.code());
        assertEquals(NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT.message(), actual.message());
    }

    @Test
    void validate_WithNameNotFoundForDefendantsOnCase_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        String firstName = "Dan";
        String lastName = "Tester";
        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", firstName);
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", lastName);
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        PartyEntity party = PartyEntity.builder()
            .firstName(firstName)
            .lastName("different")
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.DEFENDANT)
                                     .build()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(Set.of(party))
            .build();
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT.code(), actual.code());
        assertEquals(NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT.message(), actual.message());
    }

    @Test
    void validate_WithNameNotProvidedForDefendantsOnCase_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        String firstName = "Dan";
        String lastName = "Tester";
        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", firstName);
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", "");
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        PartyEntity party = PartyEntity.builder()
            .firstName(firstName)
            .lastName(lastName)
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.DEFENDANT)
                                     .build()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(Set.of(party))
            .build();
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT.code(), actual.code());
        assertEquals(NocError.ANSWERS_NOT_MATCHED_ANY_LITIGANT.message(), actual.message());
    }

    @Test
    void validate_WithMultipleSameNameDefendantsOnCase_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        String firstName = "Dan";
        String lastName = "Tester";
        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", firstName);
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", lastName);
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        PartyEntity party = PartyEntity.builder()
            .firstName(firstName)
            .lastName(lastName)
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.DEFENDANT)
                                     .build()))
            .build();
        PartyEntity party2 = PartyEntity.builder()
            .firstName(firstName)
            .lastName(lastName)
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.DEFENDANT)
                                     .build()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(Set.of(party, party2))
            .build();
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(null, nocAnswersRequest);

        // then
        assertEquals(DUPLICATE_DEFENDANT_NAME_CODE, actual.code());
        assertEquals(DUPLICATE_DEFENDANT_NAME_MESSAGE, actual.message());
    }

    @Test
    void validate_WithDefendantAlreadyRepresented_ReturnErrorAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        String firstName = "Dan";
        String lastName = "Tester";
        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", firstName);
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", lastName);
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder()
            .id(partyId)
            .firstName(firstName)
            .lastName(lastName)
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.DEFENDANT)
                                     .build()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(Set.of(party))
            .build();
        String userId = "123";
        String orgId = "org";
        when(nocSubmitContext.userId()).thenReturn(userId);
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        when(organisationDetailsService.getOrganisationDetails(userId)).thenReturn(organisationDetailsResponse);
        when(organisationDetailsResponse.getOrganisationIdentifier()).thenReturn(orgId);
        when(legalRepresentativeRepository.isRepresentativeOrganisationLinkedToPartyAndActive(orgId, partyId))
            .thenReturn(true);

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(nocSubmitContext, nocAnswersRequest);

        // then
        assertEquals(ORG_ALREADY_REPRESENTS_PARTY_CODE, actual.code());
        assertEquals(ORG_ALREADY_REPRESENTS_PARTY_MESSAGE, actual.message());
    }

    @Test
    void validate_WithDefendantNotAlreadyRepresented_ReturnVerifiedAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        String firstName = "Dan";
        String lastName = "Tester";
        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", firstName);
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", lastName);
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder()
            .id(partyId)
            .firstName(firstName)
            .lastName(lastName)
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.DEFENDANT)
                                     .build()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(Set.of(party))
            .build();
        String userId = "123";
        String orgId = "org";
        String orgName = "orgName";
        when(nocSubmitContext.userId()).thenReturn(userId);
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        when(organisationDetailsService.getOrganisationDetails(userId)).thenReturn(organisationDetailsResponse);
        when(organisationDetailsResponse.getOrganisationIdentifier()).thenReturn(orgId);
        when(organisationDetailsResponse.getName()).thenReturn(orgName);

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(nocSubmitContext, nocAnswersRequest);

        // then
        assertEquals("Notice of Change answers verified successfully", actual.statusMessage());
        NocOrganisation organisation = actual.organisation();
        assertEquals(orgId, organisation.organisationId());
        assertEquals(orgName, organisation.organisationName());
    }

    @Test
    void validate_WithDefendantNotAlreadyRepresentedNormalisesName_ReturnVerifiedAnswerResponse() {
        // given
        when(featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_2)).thenReturn(true);

        String firstName = " DaN ";
        String lastName = " TeStEr ";
        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", firstName);
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", lastName);
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder()
            .id(partyId)
            .firstName(firstName)
            .lastName(lastName)
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.DEFENDANT)
                                     .build()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(Set.of(party))
            .build();
        String userId = "123";
        String orgId = "org";
        String orgName = "orgName";
        when(nocSubmitContext.userId()).thenReturn(userId);
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        when(organisationDetailsService.getOrganisationDetails(userId)).thenReturn(organisationDetailsResponse);
        when(organisationDetailsResponse.getOrganisationIdentifier()).thenReturn(orgId);
        when(organisationDetailsResponse.getName()).thenReturn(orgName);

        // when
        NocAnswersResponse actual = pcsNoticeOfChange.validate(nocSubmitContext, nocAnswersRequest);

        // then
        assertEquals("Notice of Change answers verified successfully", actual.statusMessage());
        NocOrganisation organisation = actual.organisation();
        assertEquals(orgId, organisation.organisationId());
        assertEquals(orgName, organisation.organisationName());
    }

    @Test
    void submit_WithCaseNotFound_ThrowsException() {
        // given
        String firstName = "Dan";
        String lastName = "Tester";
        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", firstName);
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", lastName);
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        String userId = UUID.randomUUID().toString();

        // when / then
        assertThatThrownBy(() -> pcsNoticeOfChange.submit(nocSubmitContext, nocAnswersRequest))
            .isInstanceOf(CaseNotFoundException.class)
            .hasMessage("No case found with reference %s", TEST_CASE_REFERENCE);
    }


    @Test
    void submit_WithDefendantNotAlreadyRepresented_SchedulesAccessChangeTask() {
        // given
        String firstName = "Dan";
        String lastName = "Tester";
        NocAnswer answer = new NocAnswer("pcs-defendant-first-name", firstName);
        NocAnswer answer2 = new NocAnswer("pcs-defendant-last-name", lastName);
        NocAnswersRequest nocAnswersRequest = new NocAnswersRequest(TEST_CASE_REFERENCE, List.of(answer, answer2));
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder()
            .id(partyId)
            .firstName(firstName)
            .lastName(lastName)
            .claimParties(Set.of(ClaimPartyEntity.builder()
                                     .role(PartyRole.DEFENDANT)
                                     .build()))
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(Set.of(party))
            .caseReference(TEST_CASE_REFERENCE)
            .build();
        String userId = UUID.randomUUID().toString();
        when(nocSubmitContext.userId()).thenReturn(userId);
        when(pcsCaseRepository.findByCaseReference(TEST_CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        when(organisationDetailsService.getOrganisationDetails(userId)).thenReturn(organisationDetailsResponse);

        // when
        NocSubmissionResponse actual = pcsNoticeOfChange.submit(nocSubmitContext, nocAnswersRequest);

        // then
        assertEquals("Notice of request has been successfully submitted.", actual.statusMessage());
        assertEquals("APPROVED", actual.approvalStatus());
        assertEquals(DEFENDANT_SOLICITOR.getRole(), actual.caseRole());

        NocAccessChangeTaskData taskData = getCapturedRoleAssignmentTaskData();
        assertThat(taskData.getCaseReference()).isEqualTo(String.valueOf(TEST_CASE_REFERENCE));
        assertThat(taskData.getUserId()).isEqualTo(userId);
        assertThat(taskData.getPartyId()).isEqualTo(partyId.toString());
        assertThat(taskData.getOrganisationDetailsResponse()).isEqualTo(organisationDetailsResponse);
    }

    @SuppressWarnings("unchecked")
    private NocAccessChangeTaskData getCapturedRoleAssignmentTaskData() {
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());

        return captor.getAllValues().stream()
            .filter(t -> t.getTaskInstance().getTaskName()
                .equals(NocAccessChangeTaskComponent.NOC_ACCESS_CHANGE_TASK_DESCRIPTOR.getTaskName()))
            .map(SchedulableInstance::getTaskInstance)
            .map(TaskInstance::getData)
            .map(NocAccessChangeTaskData.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No noc access task found"));
    }

}
