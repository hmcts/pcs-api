package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftCaseDataServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private DraftCaseJsonMerger draftCaseJsonMerger;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private DraftClearFieldsProcessor clearFieldsProcessor;
    @Captor
    private ArgumentCaptor<DraftCaseDataEntity> unsubmittedCaseDataEntityCaptor;

    private final EventId eventId = EventId.resumePossessionClaim;

    private DraftCaseDataService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftCaseDataService(
            draftCaseDataRepository,
            objectMapper,
            draftCaseJsonMerger,
            securityContextService,
            clearFieldsProcessor
        );

        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID.toString())
            .build();
        when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
    }

    @Test
    void shouldGetUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);
        PCSCase expectedUnsubmittedCaseData = mock(PCSCase.class);

        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenReturn(expectedUnsubmittedCaseData);

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(unsubmittedCaseData).contains(expectedUnsubmittedCaseData);
        verify(expectedUnsubmittedCaseData).setHasUnsubmittedCaseData(YesOrNo.YES);
    }

    @Test
    void shouldReturnEmptyWhenNoUnsubmittedCaseData() {
        // Given
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.empty());

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(unsubmittedCaseData).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnWhetherUnsubmittedCaseDataExists(boolean repositoryDataExists) {
        // Given
        when(draftCaseDataRepository.existsByCaseReferenceAndEventIdAndIdamUserId(CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(repositoryDataExists);

        // When
        boolean hasUnsubmittedCaseData = underTest.hasUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(hasUnsubmittedCaseData).isEqualTo(repositoryDataExists);
    }

    @Test
    void shouldSaveNewUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String caseDataJson = "case data json";
        PCSCase caseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(caseData)).thenReturn(caseDataJson);
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.empty());
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, caseData, eventId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getCaseData()).isEqualTo(caseDataJson);
        assertThat(savedEntity.getIdamUserId()).isEqualTo(USER_ID);
    }

    @Test
    void shouldPatchUnsubmittedCaseDataWithJson() {
        // Given
        String caseDataJson = "case data json";
        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.empty());
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        underTest.patchUnsubmittedCaseData(CASE_REFERENCE, eventId, caseDataJson, Optional.empty());

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getCaseData()).isEqualTo(caseDataJson);
    }

    @Test
    void shouldUpdateExistingUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String existingCaseDataJson = "existing case data json";
        String newCaseDataJson = "new case data json";
        String mergedCaseDataJson = "merged case data JSON";

        PCSCase newCaseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(newCaseData)).thenReturn(newCaseDataJson);

        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);
        when(draftCaseDataEntity.getCaseData()).thenReturn(existingCaseDataJson);

        when(draftCaseJsonMerger.mergeJson(existingCaseDataJson, newCaseDataJson)).thenReturn(mergedCaseDataJson);

        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, newCaseData, eventId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity).isSameAs(draftCaseDataEntity);
        verify(draftCaseDataEntity).setCaseData(mergedCaseDataJson);
    }

    @Test
    void shouldDeleteUnsubmittedDataByCaseReference() {
        // When
        underTest.deleteUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        verify(draftCaseDataRepository).deleteByCaseReferenceAndEventIdAndIdamUserId(CASE_REFERENCE, eventId, USER_ID);
    }

    @Test
    void shouldThrowExceptionForJsonExceptionWhenReading() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);

        when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(CASE_REFERENCE, eventId, USER_ID))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);

        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenThrow(jsonProcessingException);

        // Then
        assertThatThrownBy(() -> underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId))
            .isInstanceOf(UnsubmittedDataException.class)
            .hasMessage("Failed to read saved answers")
            .hasCause(jsonProcessingException);
    }

    @Test
    void shouldThrowExceptionForJsonExceptionWhenSaving() throws JsonProcessingException {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(caseData)).thenThrow(jsonProcessingException);

        // Then
        assertThatThrownBy(() -> underTest.patchUnsubmittedEventData(CASE_REFERENCE, caseData, eventId))
           .isInstanceOf(UnsubmittedDataException.class)
            .hasMessage("Failed to save answers")
            .hasCause(jsonProcessingException);

    }

    /**
     * Integration tests for clearFields functionality.
     * Tests use real ObjectMapper and DraftCaseJsonMerger to verify complete behavior.
     */
    @Nested
    class ClearFieldsIntegrationTests {

        private ObjectMapper realObjectMapper;
        private DraftCaseJsonMerger realMerger;
        private DraftCaseDataService serviceWithRealDependencies;

        @BeforeEach
        void setUpIntegration() {
            realObjectMapper = new ObjectMapper();
            realMerger = new DraftCaseJsonMerger(realObjectMapper);
            DraftClearFieldsProcessor realClearFieldsProcessor = new DraftClearFieldsProcessor(realObjectMapper);
            serviceWithRealDependencies = new DraftCaseDataService(
                draftCaseDataRepository,
                realObjectMapper,
                realMerger,
                securityContextService,
                realClearFieldsProcessor
            );

            UserInfo userInfo = UserInfo.builder()
                .uid(USER_ID.toString())
                .build();
            when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        }

        @Test
        void shouldRemoveFieldsInClearFieldsListWhilePreservingOtherFields() throws Exception {
            // GIVEN: Draft with multiple fields at different nesting levels
            String existingDraft = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "dateOfBirth": "1990-05-15",
                      "tenancyStartDate": "2020-01-01",
                      "contactByPhone": "YES",
                      "contactByText": "NO",
                      "freeLegalAdvice": "YES",
                      "householdCircumstances": {
                        "pension": "YES",
                        "pensionAmount": 100000,
                        "pensionFrequency": "MONTHLY",
                        "shareIncomeExpenseDetails": "Yes"
                      }
                    }
                  }
                }
                """;

            DraftCaseDataEntity existingEntity = new DraftCaseDataEntity();
            existingEntity.setCaseData(existingDraft);

            when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, EventId.respondPossessionClaim, USER_ID))
                .thenReturn(Optional.of(existingEntity));
            when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN: Update draft with clearFields list specifying nested paths to remove
            String updateJson = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "householdCircumstances": {
                        "pension": "NO"
                      }
                    }
                  }
                }
                """;

            ClearFieldsContext clearFieldsContext = new ClearFieldsContext(
                "possessionClaimResponse",
                List.of(
                    "defendantResponses.householdCircumstances.pensionAmount",
                    "defendantResponses.householdCircumstances.pensionFrequency"
                )
            );

            serviceWithRealDependencies.patchUnsubmittedCaseData(
                CASE_REFERENCE, EventId.respondPossessionClaim, updateJson, Optional.of(clearFieldsContext));

            // THEN: Verify specified fields removed, other fields preserved
            ArgumentCaptor<DraftCaseDataEntity> captor = ArgumentCaptor.forClass(DraftCaseDataEntity.class);
            verify(draftCaseDataRepository).save(captor.capture());

            String savedJson = captor.getValue().getCaseData();
            JsonNode savedData = realObjectMapper.readTree(savedJson);
            JsonNode responses = savedData.at("/possessionClaimResponse/defendantResponses");
            JsonNode hc = responses.at("/householdCircumstances");

            // Fields in clearFields list removed
            assertThat(hc.get("pension").asText()).isEqualTo("NO");
            assertThat(hc.has("pensionAmount")).isFalse();
            assertThat(hc.has("pensionFrequency")).isFalse();

            // Other fields preserved via merge
            assertThat(responses.get("dateOfBirth").asText()).isEqualTo("1990-05-15");
            assertThat(responses.get("tenancyStartDate").asText()).isEqualTo("2020-01-01");
            assertThat(responses.get("contactByPhone").asText()).isEqualTo("YES");
            assertThat(responses.get("contactByText").asText()).isEqualTo("NO");
            assertThat(responses.get("freeLegalAdvice").asText()).isEqualTo("YES");
            assertThat(hc.get("shareIncomeExpenseDetails").asText()).isEqualTo("Yes");
        }

        @Test
        void shouldOnlyClearSpecifiedFieldPathsInClearFieldsList() throws Exception {
            // GIVEN: Draft with multiple nested field groups
            String existingDraft = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "freeLegalAdvice": "YES",
                      "householdCircumstances": {
                        "pension": "YES",
                        "pensionAmount": 150000,
                        "pensionFrequency": "MONTHLY",
                        "incomeFromJobs": "YES",
                        "incomeFromJobsAmount": 200000,
                        "incomeFromJobsFrequency": "WEEKLY"
                      }
                    }
                  }
                }
                """;

            DraftCaseDataEntity existingEntity = new DraftCaseDataEntity();
            existingEntity.setCaseData(existingDraft);

            when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, EventId.respondPossessionClaim, USER_ID))
                .thenReturn(Optional.of(existingEntity));
            when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN: Clear only specific nested paths, not all related fields
            String updateJson = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "householdCircumstances": {
                        "pension": "NO"
                      }
                    }
                  }
                }
                """;

            ClearFieldsContext clearFieldsContext = new ClearFieldsContext(
                "possessionClaimResponse",
                List.of(
                    "defendantResponses.householdCircumstances.pensionAmount",
                    "defendantResponses.householdCircumstances.pensionFrequency"
                )
            );

            serviceWithRealDependencies.patchUnsubmittedCaseData(
                CASE_REFERENCE, EventId.respondPossessionClaim, updateJson, Optional.of(clearFieldsContext));

            // THEN: Verify only specified paths cleared, sibling field groups unaffected
            ArgumentCaptor<DraftCaseDataEntity> captor = ArgumentCaptor.forClass(DraftCaseDataEntity.class);
            verify(draftCaseDataRepository).save(captor.capture());

            String savedJson = captor.getValue().getCaseData();
            JsonNode savedData = realObjectMapper.readTree(savedJson);
            JsonNode hc = savedData.at("/possessionClaimResponse/defendantResponses/householdCircumstances");

            // Specified fields cleared
            assertThat(hc.get("pension").asText()).isEqualTo("NO");
            assertThat(hc.has("pensionAmount")).isFalse();
            assertThat(hc.has("pensionFrequency")).isFalse();

            // Sibling field group preserved
            assertThat(hc.get("incomeFromJobs").asText()).isEqualTo("YES");
            assertThat(hc.get("incomeFromJobsAmount").asInt()).isEqualTo(200000);
            assertThat(hc.get("incomeFromJobsFrequency").asText()).isEqualTo("WEEKLY");
        }

        @Test
        void shouldClearMultipleNestedFieldPathsInSingleDraftUpdate() throws Exception {
            // GIVEN: Draft with deeply nested structure and multiple field paths
            String existingDraft = """
                {
                  "possessionClaimResponse": {
                    "defendantContactDetails": {
                      "party": {
                        "firstName": "Arunkumar",
                        "lastName": "Kumar",
                        "phoneNumber": "07700 900 982",
                        "emailAddress": "test@example.com"
                      }
                    },
                    "defendantResponses": {
                      "dateOfBirth": "1990-05-15",
                      "tenancyStartDate": "2023-01-01",
                      "contactByPhone": "YES",
                      "freeLegalAdvice": "YES",
                      "householdCircumstances": {
                        "pension": "YES",
                        "pensionAmount": 150000,
                        "pensionFrequency": "MONTHLY",
                        "incomeFromJobs": "YES",
                        "incomeFromJobsAmount": 250000,
                        "incomeFromJobsFrequency": "WEEKLY",
                        "universalCredit": "YES",
                        "universalCreditAmount": 120000,
                        "universalCreditFrequency": "MONTHLY",
                        "otherBenefits": "YES",
                        "otherBenefitsAmount": 80000,
                        "otherBenefitsFrequency": "WEEKLY",
                        "moneyFromElsewhere": "YES",
                        "moneyFromElsewhereDetails": "Rental income",
                        "shareIncomeExpenseDetails": "Yes"
                      }
                    }
                  }
                }
                """;

            DraftCaseDataEntity existingEntity = new DraftCaseDataEntity();
            existingEntity.setCaseData(existingDraft);

            when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, EventId.respondPossessionClaim, USER_ID))
                .thenReturn(Optional.of(existingEntity));
            when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN: Clear 8 field paths in single draft update
            String updateJson = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "householdCircumstances": {
                        "pension": "NO",
                        "incomeFromJobs": "NO",
                        "universalCredit": "NO",
                        "otherBenefits": "NO",
                        "moneyFromElsewhere": "YES",
                        "moneyFromElsewhereDetails": "Updated rental income"
                      }
                    }
                  }
                }
                """;

            ClearFieldsContext clearFieldsContext = new ClearFieldsContext(
                "possessionClaimResponse",
                List.of(
                    "defendantResponses.householdCircumstances.pensionAmount",
                    "defendantResponses.householdCircumstances.pensionFrequency",
                    "defendantResponses.householdCircumstances.incomeFromJobsAmount",
                    "defendantResponses.householdCircumstances.incomeFromJobsFrequency",
                    "defendantResponses.householdCircumstances.universalCreditAmount",
                    "defendantResponses.householdCircumstances.universalCreditFrequency",
                    "defendantResponses.householdCircumstances.otherBenefitsAmount",
                    "defendantResponses.householdCircumstances.otherBenefitsFrequency"
                )
            );

            serviceWithRealDependencies.patchUnsubmittedCaseData(
                CASE_REFERENCE, EventId.respondPossessionClaim, updateJson, Optional.of(clearFieldsContext));

            // THEN: Verify all 8 paths cleared, nested structure preserved, sibling nodes unaffected
            ArgumentCaptor<DraftCaseDataEntity> captor = ArgumentCaptor.forClass(DraftCaseDataEntity.class);
            verify(draftCaseDataRepository).save(captor.capture());

            String savedJson = captor.getValue().getCaseData();
            JsonNode savedData = realObjectMapper.readTree(savedJson);
            JsonNode contact = savedData.at("/possessionClaimResponse/defendantContactDetails/party");
            JsonNode responses = savedData.at("/possessionClaimResponse/defendantResponses");
            JsonNode hc = responses.at("/householdCircumstances");

            // Sibling node at different depth preserved
            assertThat(contact.get("firstName").asText()).isEqualTo("Arunkumar");
            assertThat(contact.get("lastName").asText()).isEqualTo("Kumar");
            assertThat(contact.get("phoneNumber").asText()).isEqualTo("07700 900 982");

            // Parent level fields preserved
            assertThat(responses.get("dateOfBirth").asText()).isEqualTo("1990-05-15");
            assertThat(responses.get("tenancyStartDate").asText()).isEqualTo("2023-01-01");
            assertThat(responses.get("freeLegalAdvice").asText()).isEqualTo("YES");

            // 8 field paths cleared (4 field groups × 2 fields each)
            assertThat(hc.get("pension").asText()).isEqualTo("NO");
            assertThat(hc.has("pensionAmount")).isFalse();
            assertThat(hc.has("pensionFrequency")).isFalse();

            assertThat(hc.get("incomeFromJobs").asText()).isEqualTo("NO");
            assertThat(hc.has("incomeFromJobsAmount")).isFalse();
            assertThat(hc.has("incomeFromJobsFrequency")).isFalse();

            assertThat(hc.get("universalCredit").asText()).isEqualTo("NO");
            assertThat(hc.has("universalCreditAmount")).isFalse();
            assertThat(hc.has("universalCreditFrequency")).isFalse();

            assertThat(hc.get("otherBenefits").asText()).isEqualTo("NO");
            assertThat(hc.has("otherBenefitsAmount")).isFalse();
            assertThat(hc.has("otherBenefitsFrequency")).isFalse();

            // Fields not in clearFields list updated via merge
            assertThat(hc.get("moneyFromElsewhere").asText()).isEqualTo("YES");
            assertThat(hc.get("moneyFromElsewhereDetails").asText()).isEqualTo("Updated rental income");
            assertThat(hc.get("shareIncomeExpenseDetails").asText()).isEqualTo("Yes");
        }

        @Test
        void shouldAllowReenteringClearedFieldsWithNewValuesInSubsequentUpdates() throws Exception {
            // GIVEN: Sequential draft updates simulating field lifecycle
            when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, EventId.respondPossessionClaim, USER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new DraftCaseDataEntity()))
                .thenReturn(Optional.of(new DraftCaseDataEntity()));

            when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
                .thenAnswer(invocation -> {
                    DraftCaseDataEntity saved = invocation.getArgument(0);
                    DraftCaseDataEntity returnEntity = new DraftCaseDataEntity();
                    returnEntity.setCaseData(saved.getCaseData());
                    return returnEntity;
                });

            // UPDATE 1: Create draft with nested fields
            String call1Json = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "householdCircumstances": {
                        "pension": "YES",
                        "pensionAmount": 100000,
                        "pensionFrequency": "MONTHLY"
                      }
                    }
                  }
                }
                """;

            serviceWithRealDependencies.patchUnsubmittedCaseData(
                CASE_REFERENCE, EventId.respondPossessionClaim, call1Json, Optional.empty());

            ArgumentCaptor<DraftCaseDataEntity> captor1 = ArgumentCaptor.forClass(DraftCaseDataEntity.class);
            verify(draftCaseDataRepository).save(captor1.capture());
            String afterCall1 = captor1.getValue().getCaseData();

            // Setup for update 2
            DraftCaseDataEntity entityAfterCall1 = new DraftCaseDataEntity();
            entityAfterCall1.setCaseData(afterCall1);
            when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, EventId.respondPossessionClaim, USER_ID))
                .thenReturn(Optional.of(entityAfterCall1));

            // UPDATE 2: Clear nested fields using clearFields
            String call2Json = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "householdCircumstances": {
                        "pension": "NO"
                      }
                    }
                  }
                }
                """;

            ClearFieldsContext call2ClearContext = new ClearFieldsContext(
                "possessionClaimResponse",
                List.of(
                    "defendantResponses.householdCircumstances.pensionAmount",
                    "defendantResponses.householdCircumstances.pensionFrequency"
                )
            );

            serviceWithRealDependencies.patchUnsubmittedCaseData(
                CASE_REFERENCE, EventId.respondPossessionClaim, call2Json, Optional.of(call2ClearContext));

            ArgumentCaptor<DraftCaseDataEntity> captor2 = ArgumentCaptor.forClass(DraftCaseDataEntity.class);
            verify(draftCaseDataRepository, org.mockito.Mockito.times(2)).save(captor2.capture());
            String afterCall2 = captor2.getAllValues().get(1).getCaseData();

            // Setup for update 3
            DraftCaseDataEntity entityAfterCall2 = new DraftCaseDataEntity();
            entityAfterCall2.setCaseData(afterCall2);
            when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, EventId.respondPossessionClaim, USER_ID))
                .thenReturn(Optional.of(entityAfterCall2));

            // UPDATE 3: Re-add previously cleared fields with different values
            String call3Json = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "householdCircumstances": {
                        "pension": "YES",
                        "pensionAmount": 200000,
                        "pensionFrequency": "WEEKLY"
                      }
                    }
                  }
                }
                """;

            serviceWithRealDependencies.patchUnsubmittedCaseData(
                CASE_REFERENCE, EventId.respondPossessionClaim, call3Json, Optional.empty());

            // THEN: Verify cleared fields can be re-added with new values via merge
            ArgumentCaptor<DraftCaseDataEntity> captor3 = ArgumentCaptor.forClass(DraftCaseDataEntity.class);
            verify(draftCaseDataRepository, org.mockito.Mockito.times(3)).save(captor3.capture());

            String finalJson = captor3.getAllValues().get(2).getCaseData();
            JsonNode finalData = realObjectMapper.readTree(finalJson);
            JsonNode hc = finalData.at("/possessionClaimResponse/defendantResponses/householdCircumstances");

            assertThat(hc.get("pension").asText()).isEqualTo("YES");
            assertThat(hc.get("pensionAmount").asInt()).isEqualTo(200000);
            assertThat(hc.get("pensionFrequency").asText()).isEqualTo("WEEKLY");
        }

        @Test
        void shouldMergePartialUpdatesWithoutAffectingUnsentFields() throws Exception {
            // GIVEN: Draft with multiple field groups at same nesting level
            String existingDraft = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "dateOfBirth": "1990-05-15",
                      "tenancyStartDate": "2020-01-01",
                      "householdCircumstances": {
                        "pension": "YES",
                        "pensionAmount": 150000,
                        "pensionFrequency": "MONTHLY"
                      }
                    }
                  }
                }
                """;

            DraftCaseDataEntity existingEntity = new DraftCaseDataEntity();
            existingEntity.setCaseData(existingDraft);

            when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, EventId.respondPossessionClaim, USER_ID))
                .thenReturn(Optional.of(existingEntity));
            when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN: Merge partial update containing only subset of fields
            String updateJson = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "dateOfBirth": "1992-08-20",
                      "tenancyStartDate": "2021-06-15"
                    }
                  }
                }
                """;

            serviceWithRealDependencies.patchUnsubmittedCaseData(
                CASE_REFERENCE, EventId.respondPossessionClaim, updateJson, Optional.empty());

            // THEN: Verify sent fields merged, unsent fields preserved
            ArgumentCaptor<DraftCaseDataEntity> captor = ArgumentCaptor.forClass(DraftCaseDataEntity.class);
            verify(draftCaseDataRepository).save(captor.capture());

            String savedJson = captor.getValue().getCaseData();
            JsonNode savedData = realObjectMapper.readTree(savedJson);
            JsonNode responses = savedData.at("/possessionClaimResponse/defendantResponses");
            JsonNode hc = responses.at("/householdCircumstances");

            // Fields sent in update merged
            assertThat(responses.get("dateOfBirth").asText()).isEqualTo("1992-08-20");
            assertThat(responses.get("tenancyStartDate").asText()).isEqualTo("2021-06-15");

            // Fields not sent in update preserved
            assertThat(hc.get("pension").asText()).isEqualTo("YES");
            assertThat(hc.get("pensionAmount").asInt()).isEqualTo(150000);
            assertThat(hc.get("pensionFrequency").asText()).isEqualTo("MONTHLY");
        }

        @Test
        void shouldOnlyMergeUpdatesWhenClearFieldsIsEmpty() throws Exception {
            // GIVEN: Existing draft with nested field values
            String existingDraft = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "householdCircumstances": {
                        "pension": "YES",
                        "pensionAmount": 100000,
                        "pensionFrequency": "MONTHLY"
                      }
                    }
                  }
                }
                """;

            DraftCaseDataEntity existingEntity = new DraftCaseDataEntity();
            existingEntity.setCaseData(existingDraft);

            when(draftCaseDataRepository.findByCaseReferenceAndEventIdAndIdamUserId(
                CASE_REFERENCE, EventId.respondPossessionClaim, USER_ID))
                .thenReturn(Optional.of(existingEntity));
            when(draftCaseDataRepository.save(any(DraftCaseDataEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN: Update draft with empty clearFields list
            String updateJson = """
                {
                  "possessionClaimResponse": {
                    "defendantResponses": {
                      "householdCircumstances": {
                        "pension": "YES",
                        "pensionAmount": 150000
                      }
                    }
                  }
                }
                """;

            serviceWithRealDependencies.patchUnsubmittedCaseData(
                CASE_REFERENCE, EventId.respondPossessionClaim, updateJson, Optional.empty());

            // THEN: Verify update merged without field removal
            ArgumentCaptor<DraftCaseDataEntity> captor = ArgumentCaptor.forClass(DraftCaseDataEntity.class);
            verify(draftCaseDataRepository).save(captor.capture());

            String savedJson = captor.getValue().getCaseData();
            JsonNode savedData = realObjectMapper.readTree(savedJson);
            JsonNode hc = savedData.at("/possessionClaimResponse/defendantResponses/householdCircumstances");

            assertThat(hc.get("pension").asText()).isEqualTo("YES");
            assertThat(hc.get("pensionAmount").asInt()).isEqualTo(150000);
            assertThat(hc.get("pensionFrequency").asText()).isEqualTo("MONTHLY");
        }
    }
}
