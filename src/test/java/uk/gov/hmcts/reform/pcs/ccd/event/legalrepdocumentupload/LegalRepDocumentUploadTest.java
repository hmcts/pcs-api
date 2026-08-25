package uk.gov.hmcts.reform.pcs.ccd.event.legalrepdocumentupload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DocumentUploadCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.LegalRepDocumentUploadDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.page.legalrepdocumentupload.LegalRepDocumentUploadConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoleService;
import uk.gov.hmcts.reform.pcs.ccd.service.UserRoles;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.genapp.GenAppVisibilityService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.LegalRepForDefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class LegalRepDocumentUploadTest extends BaseEventTest {

    private static final String ORGANISATION_ID = "orgId";

    @Mock
    private LegalRepDocumentUploadConfigurer legalRepDocumentUploadConfigurer;
    @Mock(strictness = LENIENT)
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private PcsCaseService pcsCaseService;
    @Mock
    private DocumentService documentService;
    @Mock
    private OrganisationService organisationService;
    @Mock(strictness = LENIENT)
    private GenAppVisibilityService genAppVisibilityService;
    @Mock(strictness = LENIENT)
    private LegalRepForDefendantAccessValidator legalRepForDefendantAccessValidator;
    @Mock
    private UserRoleService userRoleService;
    @Mock(strictness = LENIENT)
    private PartyService partyService;

    @InjectMocks
    private LegalRepDocumentUpload legalRepDocumentUpload;

    @BeforeEach
    void setUp() {
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        setEventUnderTest(legalRepDocumentUpload);
    }

    @Test
    void shouldConfigurePages() {
        verify(legalRepDocumentUploadConfigurer).configurePages(any());
    }

    @Nested
    @DisplayName("Start Handler")
    class StartHandlerTests {

        @Test
        void shouldBuildValidCategoriesWhenGenAppDatesExist() {
            stubUserRoles(DEFENDANT_SOLICITOR);
            LocalDateTime laterDate = LocalDateTime.of(2026, 4, 25, 10, 0);
            LocalDateTime earlierDate = LocalDateTime.of(2026, 4, 20, 10, 0);

            UUID earlierAdjournId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID laterAdjournId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID generalId = UUID.fromString("33333333-3333-3333-3333-333333333333");

            GenAppEntity earlierAdjournApp = GenAppEntity.builder()
                .id(earlierAdjournId)
                .type(GenAppType.ADJOURN)
                .applicationSubmittedDate(earlierDate)
                .build();

            GenAppEntity laterAdjournApp = GenAppEntity.builder()
                .id(laterAdjournId)
                .type(GenAppType.ADJOURN)
                .applicationSubmittedDate(laterDate)
                .build();

            GenAppEntity generalApp = GenAppEntity.builder()
                .id(generalId)
                .type(GenAppType.SOMETHING_ELSE)
                .applicationSubmittedDate(laterDate)
                .build();

            GenAppEntity generalAppWithNullDate = GenAppEntity.builder()
                .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .type(GenAppType.SOMETHING_ELSE)
                .applicationSubmittedDate(null)
                .build();

            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORGANISATION_ID);

            when(genAppVisibilityService.getVisibleGenAppsToUser(any(), any()))
                .thenReturn(List.of(earlierAdjournApp, laterAdjournApp, generalApp, generalAppWithNullDate));

            PCSCase result = callStartHandler(PCSCase.builder().build());

            assertThat(result.getLegalRepDocumentUploadDetails()).isNotNull();

            DynamicStringList categories =
                result.getLegalRepDocumentUploadDetails().getValidCategories();

            assertThat(categories).isNotNull();
            assertThat(categories.getListItems()).hasSize(4);

            assertThat(categories.getListItems())
                .extracting(DynamicStringListElement::getCode)
                .containsExactlyInAnyOrder(
                    DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name(),
                    earlierAdjournId.toString(),
                    laterAdjournId.toString(),
                    generalId.toString()
                );

            assertThat(categories.getListItems())
                .filteredOn(item -> item.getLabel().contains("adjourn the hearing"))
                .extracting(DynamicStringListElement::getLabel)
                .containsExactlyInAnyOrder(
                    "Yes, the documents I’m uploading relate to the application to adjourn the "
                        + "hearing - submitted on Monday 20 April 2026",
                    "Yes, the documents I’m uploading relate to the application to adjourn the "
                        + "hearing - submitted on Saturday 25 April 2026"
                );

            assertThat(categories.getListItems())
                .filteredOn(item -> item.getLabel().equals(
                    "Yes, the documents I’m uploading relate to an application submitted on "
                        + "Saturday 25 April 2026"))
                .extracting(DynamicStringListElement::getCode)
                .containsExactly(generalId.toString());

            assertThat(categories.getListItems())
                .filteredOn(item ->
                                DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name()
                                    .equals(item.getCode()))
                .extracting(DynamicStringListElement::getLabel)
                .containsExactly(
                    "No, the documents I’m uploading relate to the main claim or counterclaim"
                );

            assertThat(result.getLegalRepDocumentUploadDetails().getShowExistingApplicationPage())
                .isEqualTo(VerticalYesNo.YES);

        }

        @Test
        void shouldKeepOnlyMainClaimOrCounterclaimWhenNoGenAppDatesAvailable() {
            stubUserRoles(DEFENDANT_SOLICITOR);
            PCSCase result = callStartHandler(PCSCase.builder().build());

            assertThat(result.getLegalRepDocumentUploadDetails()).isNotNull();
            DynamicStringList categories = result.getLegalRepDocumentUploadDetails().getValidCategories();
            assertThat(categories).isNotNull();
            assertThat(categories.getListItems()).hasSize(1);
            assertThat(categories.getListItems().getFirst().getCode())
                .isEqualTo(DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name());
        }

        @Test
        void shouldReturnNullForLatestGenAppDateWhenGenAppsIsNull() {
            stubUserRoles(DEFENDANT_SOLICITOR);
            PCSCase result = callStartHandler(PCSCase.builder().build());

            assertThat(result.getLegalRepDocumentUploadDetails()).isNotNull();
            DynamicStringList categories = result.getLegalRepDocumentUploadDetails().getValidCategories();
            assertThat(categories).isNotNull();
            assertThat(categories.getListItems()).hasSize(1);
            assertThat(categories.getListItems().getFirst().getCode())
                .isEqualTo(DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name());
        }

        @Test
        void shouldSetWalesFlagForWales() {
            // Given
            stubUserRoles(DEFENDANT_SOLICITOR);
            when(pcsCaseEntity.getLegislativeCountry()).thenReturn(WALES);

            // When
            PCSCase result = callStartHandler(PCSCase.builder().build());

            // Then
            assertThat(result.getLegalRepDocumentUploadDetails().getIsWales()).isEqualTo(VerticalYesNo.YES);
        }

        @ParameterizedTest
        @EnumSource(value = LegislativeCountry.class, names = "WALES", mode = EnumSource.Mode.EXCLUDE)
        void shouldSetNotWalesFlagForOtherCountries(LegislativeCountry legislativeCountry) {
            // Given
            stubUserRoles(DEFENDANT_SOLICITOR);
            when(pcsCaseEntity.getLegislativeCountry()).thenReturn(legislativeCountry);;

            // When
            PCSCase result = callStartHandler(PCSCase.builder().build());

            // Then
            assertThat(result.getLegalRepDocumentUploadDetails().getIsWales()).isEqualTo(VerticalYesNo.NO);
        }

        @Test
        void shouldSetPartyTypeFieldForClaimant() {
            // Given
            stubUserRoles(CLAIMANT_SOLICITOR);

            // When
            PCSCase result = callStartHandler(PCSCase.builder().build());

            // Then
            assertThat(result.getLegalRepDocumentUploadDetails().getPartyType()).isEqualTo(PartyType.CLAIMANT);
        }

        @Test
        void shouldSetPartyTypeFieldForDefendant() {
            // Given
            stubUserRoles(DEFENDANT_SOLICITOR);

            // When
            PCSCase result = callStartHandler(PCSCase.builder().build());

            // Then
            assertThat(result.getLegalRepDocumentUploadDetails().getPartyType()).isEqualTo(PartyType.DEFENDANT);
        }
    }

    @Test
    void shouldReturnNullForUnmappedCategoryType() {
        assertThat(legalRepDocumentUpload.mapCategoryToGenAppType(
            DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM))
            .isNull();
    }

    @Test
    void shouldReturnEmptyForUnmappedCategory() {
        assertThat(legalRepDocumentUpload.findGenAppsForCategory(
            PcsCaseEntity.builder().build(),
            ORGANISATION_ID,
            DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM))
            .isEmpty();
    }

    @Nested
    @DisplayName("Submit Handler")
    class SubmitHandlerTests {

        @Mock
        private PartyEntity primaryClaimantParty;
        @Mock
        private PartyEntity defendantParty;

        @BeforeEach
        void setUp() {
            when(partyService.getPrimaryClaimantPartyEntity(pcsCaseEntity)).thenReturn(primaryClaimantParty);

            when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, ORGANISATION_ID))
                .thenReturn(List.of(defendantParty));
        }

        @Test
        void shouldReturnErrorIfLrRepresentsMultipleDefendants() {
            // Given
            stubUserRoles(DEFENDANT_SOLICITOR);
            when(legalRepForDefendantAccessValidator.validateAndGetDefendants(pcsCaseEntity, ORGANISATION_ID))
                .thenReturn(List.of(defendantParty, mock(PartyEntity.class)));

            PCSCase pcsCase = PCSCase.builder().build();

            // When
            SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

            // Then
            assertThat(submitResponse.getErrors())
                .contains("Uploading documents for multiple parties is not supported");
        }

        @ParameterizedTest
        @MethodSource("documentUploadScenarios")
        void shouldUploadLegalRepDocument(String selectedCode,
                                          UUID genAppId,
                                          boolean isDefendantLR,
                                          boolean shouldSelectGenApp) {

            // Given
            stubUserRoles(isDefendantLR ? DEFENDANT_SOLICITOR : CLAIMANT_SOLICITOR);

            LegalRepDocument legalRepDocument = LegalRepDocument.builder()
                .document(mock(Document.class))
                .build();

            final List<LegalRepDocument> legalRepDocList = List.of(legalRepDocument);

            final LegalRepDocumentUploadDetails legalRepDocumentUploadDetails = LegalRepDocumentUploadDetails.builder()
                .validCategories(DynamicStringList.builder()
                                     .value(DynamicStringListElement.builder().code(selectedCode).build())
                                     .build())
                .legalRepDocuments(wrapListItems(legalRepDocList))
                .build();

            GenAppEntity visibleGenApp1 = mock(GenAppEntity.class);
            GenAppEntity visibleGenApp2 = mock(GenAppEntity.class);
            GenAppEntity expectedSelectedGenAppEntity = null;

            if (shouldSelectGenApp) {
                when(visibleGenApp2.getId()).thenReturn(genAppId);
                expectedSelectedGenAppEntity = visibleGenApp2;
            }

            Set<GenAppEntity> allGenApps = Set.of(mock(GenAppEntity.class), mock(GenAppEntity.class));
            List<GenAppEntity> visibleGenApps = List.of(visibleGenApp1, visibleGenApp2);

            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORGANISATION_ID);

            when(pcsCaseEntity.getGenApps()).thenReturn(allGenApps);
            when(genAppVisibilityService.getVisibleGenAppsToUser(allGenApps, ORGANISATION_ID))
                .thenReturn(visibleGenApps);

            PCSCase pcsCase = PCSCase.builder()
                .legalRepDocumentUploadDetails(legalRepDocumentUploadDetails)
                .build();

            // When
            callSubmitHandler(pcsCase);

            // Then
            PartyEntity expectedUploadingParty = isDefendantLR ? defendantParty : primaryClaimantParty;

            verify(documentService)
                .createDocumentEntitiesFromLegalRepDocuments(legalRepDocList,
                                                             pcsCaseEntity,
                                                             expectedUploadingParty,
                                                             expectedSelectedGenAppEntity);
        }

        private static Stream<Arguments> documentUploadScenarios() {
            UUID existingGenAppId = UUID.randomUUID();

            return Stream.of(
                Arguments.argumentSet("Defendant LR, no selected category",
                                      null, // Selected code
                                      existingGenAppId,
                                      true, // isDefendantLR
                                      false // Should select gen app
                ),
                Arguments.argumentSet("Defendant LR, main claim selected",
                                      DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name(), // Selected code
                                      existingGenAppId,
                                      true, // isDefendantLR
                                      false // Should select gen app
                ),
                Arguments.argumentSet("Defendant LR, gen app selected",
                                      existingGenAppId.toString(), // Selected code
                                      existingGenAppId,
                                      true, // isDefendantLR
                                      true  // Should select gen app
                ),
                Arguments.argumentSet("Claimant LR, no selected category",
                                      null, // Selected code
                                      existingGenAppId,
                                      false, // isDefendantLR
                                      false  // Should select gen app
                ),
                Arguments.argumentSet("Claimant LR, main claim selected",
                                      DocumentUploadCategory.MAIN_CLAIM_OR_COUNTERCLAIM.name(), // Selected code
                                      existingGenAppId,
                                      false, // isDefendantLR
                                      false  // Should select gen app
                ),
                Arguments.argumentSet("Claimant LR, gen app selected",
                                      existingGenAppId.toString(), // Selected code
                                      existingGenAppId,
                                      false, // isDefendantLR
                                      true  // Should select gen app
                )
            );
        }

        @ParameterizedTest
        @EnumSource(value = UserRole.class, names = {"CLAIMANT_SOLICITOR", "DEFENDANT_SOLICITOR"})
        void shouldReturnErrorWhenAtLeastOneLegalRepDocumentIsNull(UserRole userRole) {
            stubUserRoles(userRole);

            LegalRepDocument nullLegalRepDocument = null;
            LegalRepDocument validLegalRepDocument = LegalRepDocument.builder()
                .document(new Document())
                .build();

            List<LegalRepDocument> legalRepDocList = Stream.of(
                nullLegalRepDocument,
                validLegalRepDocument
            ).toList();

            PCSCase pcsCase = PCSCase.builder()
                .legalRepDocumentUploadDetails(LegalRepDocumentUploadDetails.builder()
                                                   .legalRepDocuments(wrapListItems(legalRepDocList))
                                                   .build())
                .build();

            SubmitResponse<State> submitResponse = callSubmitHandler(pcsCase);

            assertThat(submitResponse.getErrors().contains("Your files were not submitted. Try again."));
        }

    }

    private void stubUserRoles(UserRole... roles) {
        List<String> rolesList = Arrays.stream(roles)
            .map(UserRole::getRole)
            .toList();

        UserRoles userRoles = mock(UserRoles.class);
        when(userRoles.roles()).thenReturn(rolesList);
        when(userRoleService.getCurrentUserCaseRoles(TEST_CASE_REFERENCE)).thenReturn(userRoles);
    }

}
