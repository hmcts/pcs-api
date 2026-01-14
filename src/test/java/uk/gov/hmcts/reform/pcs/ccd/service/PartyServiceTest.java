package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENT_SET_NAME_PLACEHOLDER;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.wrapListItems;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private ModelMapper modelMapper;
    @Mock(strictness = LENIENT)
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Captor
    private ArgumentCaptor<PartyEntity> partyEntityCaptor;
    @Captor
    private ArgumentCaptor<List<PartyEntity>> partyEntityListCaptor;

    private PartyService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PartyService(partyRepository, modelMapper);
    }

    @Nested
    @DisplayName("Claimant tests")
    class ClaimantTests {

        @BeforeEach
        void setUp() {
            stubPlaceholderDefendant();
        }

        private void stubPlaceholderDefendant() {
            DefendantDetails defendantDetails = mock(DefendantDetails.class);
            when(pcsCase.getDefendant1()).thenReturn(defendantDetails);
        }

        @Test
        void shouldThrowExceptionForNullClaimant() {
            // Given
            when(pcsCase.getClaimantInformation()).thenReturn(null);

            // When
            Throwable throwable = catchThrowable(() -> underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity));

            // Then
            assertThat(throwable)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Claimant must be provided");
        }

        @Test
        void shouldSaveCreatedClaimant() {
            // Given
            String expectedClaimantName = "Claimant name";

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantContactEmail("test@test.com")
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder()
                .claimantName(expectedClaimantName)
                .isClaimantNameCorrect(VerticalYesNo.YES)
                .build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(partyRepository).save(partyEntityCaptor.capture());
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getOrgName()).isEqualTo(expectedClaimantName);
        }

        @Test
        void shouldUseClaimantNameIfCorrect() {
            // Given
            String expectedClaimantName = "Claimant name";
            String overriddenName = "Overridden name";

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantContactEmail("test@test.com")
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder()
                .claimantName(expectedClaimantName)
                .overriddenClaimantName(overriddenName)
                .isClaimantNameCorrect(VerticalYesNo.YES)
                .build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getOrgName()).isEqualTo(expectedClaimantName);
            assertThat(createdClaimant.getNameOverridden()).isEqualTo(YesOrNo.NO);

            verify(pcsCaseEntity).addParty(createdClaimant);
            verify(partyRepository).save(createdClaimant);
        }

        @Test
        void shouldCreateClaimantWithOverriddenName() {
            // Given
            String claimantName = "Claimant name";
            String expectedOverriddenName = "Overridden name";

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantContactEmail("test@test.com")
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder()
                .claimantName(claimantName)
                .overriddenClaimantName(expectedOverriddenName)
                .isClaimantNameCorrect(VerticalYesNo.NO)
                .build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getOrgName()).isEqualTo(expectedOverriddenName);
            assertThat(createdClaimant.getNameOverridden()).isEqualTo(YesOrNo.YES);

            verify(pcsCaseEntity).addParty(createdClaimant);
        }

        @Test
        void shouldUseOrganisationAddressWhenNoOverriddenContactAddress() {
            // Given
            AddressUK organisationAddress = mock(AddressUK.class);
            AddressEntity mappedAddress = mock(AddressEntity.class);
            when(modelMapper.map(organisationAddress, AddressEntity.class)).thenReturn(mappedAddress);

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .organisationAddress(organisationAddress)
                .claimantContactEmail("test@test.com")
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder()
                .isClaimantNameCorrect(VerticalYesNo.YES)
                .claimantName("Claimant name")
                .build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getAddress()).isSameAs(mappedAddress);
            verify(pcsCaseEntity).addParty(createdClaimant);
        }

        @Test
        void shouldUseOverriddenClaimantAddress() {
            // Given
            AddressUK overriddenAddress = mock(AddressUK.class);
            AddressEntity mappedAddress = mock(AddressEntity.class);
            when(modelMapper.map(overriddenAddress, AddressEntity.class)).thenReturn(mappedAddress);

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .overriddenClaimantContactAddress(overriddenAddress)
                .organisationAddress(mock(AddressUK.class)) // should be ignored when overridden present
                .claimantContactEmail("test@test.com")
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder()
                .isClaimantNameCorrect(VerticalYesNo.NO)
                .build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getAddress()).isSameAs(mappedAddress);
            verify(pcsCaseEntity).addParty(createdClaimant);
        }

        @Test
        void shouldUseClaimantEmailAddressIfOverriddenNotProvided() {
            // Given
            String expectedEmailAddress = "test@test.com";

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantContactEmail(expectedEmailAddress)
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder().build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getEmailAddress()).isEqualTo(expectedEmailAddress);
            verify(pcsCaseEntity).addParty(createdClaimant);
        }

        @Test
        void shouldUseOverriddenClaimantEmailAddress() {
            // Given
            String expectedEmailAddress = "overridden@test.com";

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantContactEmail("original@test.com")
                .overriddenClaimantContactEmail(expectedEmailAddress)
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder().build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getEmailAddress()).isEqualTo(expectedEmailAddress);
            verify(pcsCaseEntity).addParty(createdClaimant);
        }

        @Test
        void shouldFallbackToClaimantEmailWhenOverriddenEmailIsBlank() {
            // Given
            String claimantEmail = "test@test.com";

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantContactEmail(claimantEmail)
                .overriddenClaimantContactEmail("   ")
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder().build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getEmailAddress()).isEqualTo(claimantEmail);
        }

        @Test
        void shouldUseClaimantPhoneNumerWhenProvided() {
            // Given
            String expectedPhoneNumber = "some phone number";

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantProvidePhoneNumber(VerticalYesNo.YES)
                .claimantContactPhoneNumber(expectedPhoneNumber)
                .claimantContactEmail("test@test.com")
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder().build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getPhoneNumber()).isEqualTo(expectedPhoneNumber);
            assertThat(createdClaimant.getPhoneNumberProvided()).isEqualTo(VerticalYesNo.YES);
            verify(pcsCaseEntity).addParty(createdClaimant);
        }

        @Test
        void shouldNotUseClaimantPhoneNumerWhenNotProvided() {
            // Given
            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .claimantContactPhoneNumber("some phone number")
                .claimantContactEmail("test@test.com")
                .build();

            ClaimantInformation claimantInformation = ClaimantInformation.builder().build();

            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.CLAIMANT));
            PartyEntity createdClaimant = partyEntityCaptor.getValue();

            assertThat(createdClaimant.getPhoneNumber()).isNull();
            assertThat(createdClaimant.getPhoneNumberProvided()).isEqualTo(VerticalYesNo.NO);
            verify(pcsCaseEntity).addParty(createdClaimant);
        }
    }

    @Nested
    @DisplayName("Defendant tests")
    class DefendantTests {

        @BeforeEach
        void setUp() {
            stubPlaceholderClaimant();
        }

        private void stubPlaceholderClaimant() {
            ClaimantInformation claimantInformation = mock(ClaimantInformation.class);
            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantContactEmail("test@test.com")
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);
        }

        @Test
        void shouldThrowExceptionForNullDefendant1() {
            // Given
            when(pcsCase.getDefendant1()).thenReturn(null);

            // When
            Throwable throwable = catchThrowable(() -> underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity));

            // Then
            assertThat(throwable)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Defendant 1 must be provided");
        }

        @ParameterizedTest(name = ARGUMENT_SET_NAME_PLACEHOLDER)
        @MethodSource("singleDefendantScenarios")
        void shouldBuildListWithSingleDefendant(DefendantDetails defendant1, PartyEntity expectedPartyEntity) {
            // Given
            when(pcsCase.getDefendant1()).thenReturn(defendant1);
            when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.NO);

            AddressUK correspondenceAddress = defendant1.getCorrespondenceAddress();
            if (correspondenceAddress != null) {
                AddressEntity mapped = mock(AddressEntity.class);
                when(modelMapper.map(correspondenceAddress, AddressEntity.class)).thenReturn(mapped);
                expectedPartyEntity.setAddress(mapped);
            }

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.DEFENDANT));
            PartyEntity createdDefendant = partyEntityCaptor.getValue();

            assertThat(createdDefendant)
                .usingRecursiveComparison()
                .isEqualTo(expectedPartyEntity);

            verify(pcsCaseEntity).addParty(createdDefendant);
        }

        @Test
        void shouldBuildListWithMultipleDefendants() {
            // Given
            AddressUK defendant1Address = mock(AddressUK.class);
            AddressEntity mappedDefendant1Address = mock(AddressEntity.class);
            when(modelMapper.map(defendant1Address, AddressEntity.class)).thenReturn(mappedDefendant1Address);

            DefendantDetails defendant1Details = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("defendant 1 first name")
                .lastName(("defendant 1 last name"))
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsPossession(VerticalYesNo.NO)
                .correspondenceAddress(defendant1Address)
                .build();

            DefendantDetails defendant2Details = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("defendant 2 first name")
                .lastName(("defendant 2 last name"))
                .addressKnown(VerticalYesNo.NO)
                .build();

            DefendantDetails defendant3Details = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsPossession(VerticalYesNo.YES)
                .build();

            when(pcsCase.getDefendant1()).thenReturn(defendant1Details);

            List<DefendantDetails> additionalDefendantsDetails = List.of(defendant2Details, defendant3Details);
            when(pcsCase.getAdditionalDefendants()).thenReturn(wrapListItems(additionalDefendantsDetails));
            when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.YES);

            PartyEntity expectedDefendant1 = PartyEntity.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("defendant 1 first name")
                .lastName("defendant 1 last name")
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsProperty(VerticalYesNo.NO)
                .address(mappedDefendant1Address)
                .build();

            PartyEntity expectedDefendant2 = PartyEntity.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("defendant 2 first name")
                .lastName("defendant 2 last name")
                .addressKnown(VerticalYesNo.NO)
                .build();

            PartyEntity expectedDefendant3 = PartyEntity.builder()
                .nameKnown(VerticalYesNo.NO)
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsProperty(VerticalYesNo.YES)
                .build();

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity, times(3))
                .addParty(partyEntityCaptor.capture(), eq(PartyRole.DEFENDANT));

            List<PartyEntity> createdDefendants = partyEntityCaptor.getAllValues();

            assertThat(createdDefendants)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expectedDefendant1, expectedDefendant2, expectedDefendant3);

            verify(partyRepository).saveAll(partyEntityListCaptor.capture());

            assertThat(partyEntityListCaptor.getValue())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expectedDefendant1, expectedDefendant2, expectedDefendant3);
        }

        @Test
        void shouldIgnoreMultipleDefendantsIfAdditionalDefendantsNotIndicated() {
            // Given
            AddressUK defendant1Address = mock(AddressUK.class);
            AddressEntity mappedDefendant1Address = mock(AddressEntity.class);
            when(modelMapper.map(defendant1Address, AddressEntity.class)).thenReturn(mappedDefendant1Address);

            DefendantDetails defendant1Details = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("defendant 1 first name")
                .lastName(("defendant 1 last name"))
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsPossession(VerticalYesNo.NO)
                .correspondenceAddress(defendant1Address)
                .build();

            DefendantDetails defendant2Details = DefendantDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("defendant 2 first name")
                .lastName(("defendant 2 last name"))
                .addressKnown(VerticalYesNo.NO)
                .build();

            when(pcsCase.getDefendant1()).thenReturn(defendant1Details);
            when(pcsCase.getAdditionalDefendants()).thenReturn(wrapListItems(List.of(defendant2Details)));
            when(pcsCase.getAddAnotherDefendant()).thenReturn(VerticalYesNo.NO);

            PartyEntity expectedDefendant1 = PartyEntity.builder()
                .nameKnown(VerticalYesNo.YES)
                .firstName("defendant 1 first name")
                .lastName("defendant 1 last name")
                .addressKnown(VerticalYesNo.YES)
                .addressSameAsProperty(VerticalYesNo.NO)
                .address(mappedDefendant1Address)
                .build();

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.DEFENDANT));

            assertThat(partyEntityCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedDefendant1);
        }

        private static Stream<Arguments> singleDefendantScenarios() {
            AddressUK correspondenceAddress = mock(AddressUK.class);

            return Stream.of(
                argumentSet(
                    "Name and address not known",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.NO)
                        .addressKnown(VerticalYesNo.NO)
                        .build(),
                    PartyEntity.builder()
                        .nameKnown(VerticalYesNo.NO)
                        .addressKnown(VerticalYesNo.NO)
                        .build()
                ),
                argumentSet(
                    "Name known, address not known",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("expected first name")
                        .lastName(("expected last name"))
                        .addressKnown(VerticalYesNo.NO)
                        .build(),
                    PartyEntity.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("expected first name")
                        .lastName("expected last name")
                        .addressKnown(VerticalYesNo.NO)
                        .build()
                ),
                argumentSet(
                    "Name not known, address same as property",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.NO)
                        .addressKnown(VerticalYesNo.YES)
                        .addressSameAsPossession(VerticalYesNo.YES)
                        .build(),
                    PartyEntity.builder()
                        .nameKnown(VerticalYesNo.NO)
                        .addressKnown(VerticalYesNo.YES)
                        .addressSameAsProperty(VerticalYesNo.YES)
                        .build()
                ),
                argumentSet(
                    "Name known, different correspondence address",
                    DefendantDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("expected first name")
                        .lastName(("expected last name"))
                        .addressKnown(VerticalYesNo.YES)
                        .addressSameAsPossession(VerticalYesNo.NO)
                        .correspondenceAddress(correspondenceAddress)
                        .build(),
                    PartyEntity.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .firstName("expected first name")
                        .lastName("expected last name")
                        .addressKnown(VerticalYesNo.YES)
                        .addressSameAsProperty(VerticalYesNo.NO)
                        .build()
                )
            );
        }
    }

    @Nested
    @DisplayName("Underlessee tests")
    class UnderlesseeTests {

        @BeforeEach
        void setUp() {
            stubPlaceholderClaimant();
            stubPlaceholderDefendant();
        }

        private void stubPlaceholderClaimant() {
            ClaimantInformation claimantInformation = mock(ClaimantInformation.class);
            when(pcsCase.getClaimantInformation()).thenReturn(claimantInformation);

            ClaimantContactPreferences claimantContactPreferences = ClaimantContactPreferences.builder()
                .claimantContactEmail("test@test.com")
                .claimantProvidePhoneNumber(VerticalYesNo.NO)
                .build();
            when(pcsCase.getClaimantContactPreferences()).thenReturn(claimantContactPreferences);
        }

        private void stubPlaceholderDefendant() {
            DefendantDetails defendantDetails = mock(DefendantDetails.class);
            when(pcsCase.getDefendant1()).thenReturn(defendantDetails);
        }

        @Test
        void shouldThrowExceptionForNullUnderlessee1() {
            // Given
            when(pcsCase.getHasUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);
            when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(null);

            // When
            Throwable throwable = catchThrowable(() -> underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity));

            // Then
            assertThat(throwable)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Underlessee or mortgagee 1 must be provided");
        }

        @Test
        void shouldNotCreateUnderlesseeWhenFlagIsNo() {
            // Given
            when(pcsCase.getHasUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);
            when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(UnderlesseeMortgageeDetails.builder().build());

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity, never()).addParty(any(PartyEntity.class), eq(PartyRole.UNDERLESSEE_OR_MORTGAGEE));
        }

        @ParameterizedTest(name = ARGUMENT_SET_NAME_PLACEHOLDER)
        @MethodSource("singleUnderlesseeScenarios")
        void shouldBuildListWithSingleUnderlessee(UnderlesseeMortgageeDetails underlessee1,
                                                  PartyEntity expectedPartyEntity) {
            // Given
            when(pcsCase.getHasUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);
            when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(underlessee1);
            when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);

            AddressUK correspondenceAddress = underlessee1.getAddress();
            if (correspondenceAddress != null) {
                AddressEntity mapped = mock(AddressEntity.class);
                when(modelMapper.map(correspondenceAddress, AddressEntity.class)).thenReturn(mapped);
                expectedPartyEntity.setAddress(mapped);
            }

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.UNDERLESSEE_OR_MORTGAGEE));
            PartyEntity createdUnderlessee = partyEntityCaptor.getValue();

            assertThat(createdUnderlessee)
                .usingRecursiveComparison()
                .isEqualTo(expectedPartyEntity);

            verify(pcsCaseEntity).addParty(createdUnderlessee);
        }

        @Test
        void shouldBuildListWithMultipleUnderlessees() {
            // Given
            when(pcsCase.getHasUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);

            AddressUK underlessee1Address = mock(AddressUK.class);
            AddressEntity mappedUnderlessee1Address = mock(AddressEntity.class);
            when(modelMapper.map(underlessee1Address, AddressEntity.class)).thenReturn(mappedUnderlessee1Address);

            AddressUK underlessee3Address = mock(AddressUK.class);
            AddressEntity mappedUnderlessee3Address = mock(AddressEntity.class);
            when(modelMapper.map(underlessee3Address, AddressEntity.class)).thenReturn(mappedUnderlessee3Address);

            UnderlesseeMortgageeDetails underlessee1Details = UnderlesseeMortgageeDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .name("underlessee 1 name")
                .addressKnown(VerticalYesNo.YES)
                .address(underlessee1Address)
                .build();

            UnderlesseeMortgageeDetails underlessee2Details = UnderlesseeMortgageeDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .name("underlessee 2 name")
                .addressKnown(VerticalYesNo.NO)
                .build();

            UnderlesseeMortgageeDetails underlessee3Details = UnderlesseeMortgageeDetails.builder()
                .nameKnown(VerticalYesNo.NO)
                .addressKnown(VerticalYesNo.YES)
                .address(underlessee3Address)
                .build();

            when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(underlessee1Details);
            when(pcsCase.getAdditionalUnderlesseeOrMortgagee())
                .thenReturn(wrapListItems(List.of(underlessee2Details, underlessee3Details)));
            when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);

            PartyEntity expectedUnderlessee1 = PartyEntity.builder()
                .nameKnown(VerticalYesNo.YES)
                .orgName("underlessee 1 name")
                .addressKnown(VerticalYesNo.YES)
                .address(mappedUnderlessee1Address)
                .build();

            PartyEntity expectedUnderlessee2 = PartyEntity.builder()
                .nameKnown(VerticalYesNo.YES)
                .orgName("underlessee 2 name")
                .addressKnown(VerticalYesNo.NO)
                .build();

            PartyEntity expectedUnderlessee3 = PartyEntity.builder()
                .nameKnown(VerticalYesNo.NO)
                .addressKnown(VerticalYesNo.YES)
                .address(mappedUnderlessee3Address)
                .build();

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity, times(3))
                .addParty(partyEntityCaptor.capture(), eq(PartyRole.UNDERLESSEE_OR_MORTGAGEE));

            List<PartyEntity> createdUnderlessees = partyEntityCaptor.getAllValues();

            assertThat(createdUnderlessees)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expectedUnderlessee1, expectedUnderlessee2, expectedUnderlessee3);

            verify(partyRepository, times(2)).saveAll(partyEntityListCaptor.capture());

            assertThat(partyEntityListCaptor.getAllValues().get(1))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(expectedUnderlessee1, expectedUnderlessee2, expectedUnderlessee3);
        }

        @Test
        void shouldIgnoreMultipleUnderlesseesIfAdditionalUnderlesseesNotIndicated() {
            // Given
            when(pcsCase.getHasUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);

            AddressUK underlessee1Address = mock(AddressUK.class);
            AddressEntity mappedUnderlessee1Address = mock(AddressEntity.class);
            when(modelMapper.map(underlessee1Address, AddressEntity.class)).thenReturn(mappedUnderlessee1Address);

            UnderlesseeMortgageeDetails underlessee1Details = UnderlesseeMortgageeDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .name("underlessee 1 name")
                .addressKnown(VerticalYesNo.YES)
                .address(underlessee1Address)
                .build();

            UnderlesseeMortgageeDetails underlessee2Details = UnderlesseeMortgageeDetails.builder()
                .nameKnown(VerticalYesNo.YES)
                .name("underlessee 2 name")
                .addressKnown(VerticalYesNo.NO)
                .build();

            when(pcsCase.getUnderlesseeOrMortgagee1()).thenReturn(underlessee1Details);
            when(pcsCase.getAdditionalUnderlesseeOrMortgagee()).thenReturn(wrapListItems(List.of(underlessee2Details)));
            when(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.NO);

            PartyEntity expectedUnderlessee1 = PartyEntity.builder()
                .nameKnown(VerticalYesNo.YES)
                .orgName("underlessee 1 name")
                .addressKnown(VerticalYesNo.YES)
                .address(mappedUnderlessee1Address)
                .build();

            // When
            underTest.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

            // Then
            verify(claimEntity).addParty(partyEntityCaptor.capture(), eq(PartyRole.UNDERLESSEE_OR_MORTGAGEE));

            assertThat(partyEntityCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedUnderlessee1);
        }

        private static Stream<Arguments> singleUnderlesseeScenarios() {
            AddressUK correspondenceAddress = mock(AddressUK.class);

            return Stream.of(
                argumentSet(
                    "Name and address not known",
                    UnderlesseeMortgageeDetails.builder()
                        .nameKnown(VerticalYesNo.NO)
                        .addressKnown(VerticalYesNo.NO)
                        .build(),
                    PartyEntity.builder()
                        .nameKnown(VerticalYesNo.NO)
                        .addressKnown(VerticalYesNo.NO)
                        .build()
                ),
                argumentSet(
                    "Name known, address not known",
                    UnderlesseeMortgageeDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .name("expected underlessee name")
                        .addressKnown(VerticalYesNo.NO)
                        .build(),
                    PartyEntity.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .orgName("expected underlessee name")
                        .addressKnown(VerticalYesNo.NO)
                        .build()
                ),
                argumentSet(
                    "Name known, address known",
                    UnderlesseeMortgageeDetails.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .name("expected underlessee name")
                        .addressKnown(VerticalYesNo.YES)
                        .address(correspondenceAddress)
                        .build(),
                    PartyEntity.builder()
                        .nameKnown(VerticalYesNo.YES)
                        .orgName("expected underlessee name")
                        .addressKnown(VerticalYesNo.YES)
                        .build()
                )
            );
        }
    }
}
