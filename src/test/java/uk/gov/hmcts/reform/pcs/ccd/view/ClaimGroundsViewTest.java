package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherNoGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.SecureOrFlexibleMandatoryGroundsAlternativeAccomm;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimGroundsViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock
    private ClaimEntity mainClaimEntity;
    @Captor
    private ArgumentCaptor<List<ListValue<ClaimGroundSummary>>> claimGroundListCaptor;

    private ClaimGroundsView underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimGroundsView();
    }

    @Test
    void shouldNotSetAnythingIfNoMainClaim() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase, never()).setClaimGroundSummaries(any());
    }

    @ParameterizedTest
    @MethodSource("claimGroundScenarios")
    void shouldSetClaimSummaryForAllCategories(ClaimGroundCategory groundCategory,
                                               String groundCode,
                                               String reason,
                                               String expectedLabel,
                                               boolean isRentArrears) {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        Set<ClaimGroundEntity> claimGrounds = Set.of(ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(groundCategory)
                                                         .code(groundCode)
                                                         .reason(reason)
                                                         .isRentArrears(isRentArrears)
                                                         .build());
        when(mainClaimEntity.getClaimGrounds()).thenReturn(claimGrounds);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setClaimGroundSummaries(claimGroundListCaptor.capture());

        List<ListValue<ClaimGroundSummary>> claimGroundList = claimGroundListCaptor.getValue();
        assertThat(claimGroundList)
            .map(ListValue::getValue)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("categoryRank", "groundRank")
            .containsExactly(
                ClaimGroundSummary.builder()
                    .category(groundCategory)
                    .code(groundCode)
                    .label(expectedLabel)
                    .reason(reason)
                    .isRentArrears(isRentArrears ? YesOrNo.YES : YesOrNo.NO)
                    .build()
            );
    }

    @Test
    void shouldSetDescriptionWhenGroundEntityHasItSet() {
        // Given
        String expectedDescription = "some ground description";

        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        Set<ClaimGroundEntity> claimGrounds = Set.of(ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                                                         .code(IntroductoryDemotedOrOtherGrounds.OTHER.name())
                                                         .description(expectedDescription)
                                                         .isRentArrears(false)
                                                         .build());
        when(mainClaimEntity.getClaimGrounds()).thenReturn(claimGrounds);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setClaimGroundSummaries(claimGroundListCaptor.capture());

        List<ListValue<ClaimGroundSummary>> claimGroundList = claimGroundListCaptor.getValue();
        assertThat(claimGroundList)
            .map(ListValue::getValue)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("categoryRank", "groundRank")
            .containsExactly(
                ClaimGroundSummary.builder()
                    .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                    .code(IntroductoryDemotedOrOtherGrounds.OTHER.name())
                    .description(expectedDescription)
                    .label("Other")
                    .isRentArrears(YesOrNo.NO)
                    .build()
            );
    }

    @Test
    void shouldOrderGroundsByCategoryRankThenGroundRank() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));

        // Grounds in different order to rank in ClaimGroundCategory and their respective ground enums
        Set<ClaimGroundEntity> claimGrounds = Set.of(ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                                                         .code(IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL.name())
                                                         .build(),
                                                     ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.ASSURED_MANDATORY)
                                                         .code(AssuredMandatoryGround.STUDENT_LET_GROUND4.name())
                                                         .build(),
                                                     ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                                                         .code(AssuredDiscretionaryGround.RENT_ARREARS_GROUND10.name())
                                                         .build(),
                                                     ClaimGroundEntity
                                                         .builder()
                                                         .id(UUID.randomUUID())
                                                         .category(ClaimGroundCategory.ASSURED_MANDATORY)
                                                         .code(AssuredMandatoryGround.HOLIDAY_LET_GROUND3.name())
                                                         .build()
                                                     );
        when(mainClaimEntity.getClaimGrounds()).thenReturn(claimGrounds);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verify(pcsCase).setClaimGroundSummaries(claimGroundListCaptor.capture());

        List<ListValue<ClaimGroundSummary>> claimGroundList = claimGroundListCaptor.getValue();
        assertThat(claimGroundList)
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getCode)
            .containsExactly(
                AssuredMandatoryGround.HOLIDAY_LET_GROUND3.name(),
                AssuredMandatoryGround.STUDENT_LET_GROUND4.name(),
                AssuredDiscretionaryGround.RENT_ARREARS_GROUND10.name(),
                IntroductoryDemotedOrOtherGrounds.ANTI_SOCIAL.name()
            );
    }

    private static Stream<Arguments> claimGroundScenarios() {
        return Stream.of(
            argumentSet(
                "Assured tenancy mandatory non rent arrears ground",
                ClaimGroundCategory.ASSURED_MANDATORY,
                AssuredMandatoryGround.REDEVELOPMENT_GROUND6.name(),
                "Reason for redevelopment ground", // Reason
                "Property required for redevelopment (ground 6)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Assured tenancy mandatory rent arrears ground",
                ClaimGroundCategory.ASSURED_MANDATORY,
                AssuredMandatoryGround.SERIOUS_RENT_ARREARS_GROUND8.name(),
                null, // Reason
                "Serious rent arrears (ground 8)", // Expected label
                true // Is rent arrears ground
            ),
            argumentSet(
                "Assured tenancy discretionary ground",
                ClaimGroundCategory.ASSURED_DISCRETIONARY,
                AssuredDiscretionaryGround.DETERIORATION_FURNITURE_GROUND15.name(),
                "Reason for deteriation of furniture ground", // Reason
                "Deterioration of furniture (ground 15)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy mandatory ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY,
                SecureOrFlexibleMandatoryGrounds.ANTI_SOCIAL.name(),
                "Reason for deteriation of antisocial ground", // Reason
                "Antisocial behaviour", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy discretionary ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY,
                SecureOrFlexibleDiscretionaryGrounds.RENT_ARREARS_OR_BREACH_OF_TENANCY.name(),
                "Reason for breach of tenancy ground", // Reason
                "Rent arrears or breach of the tenancy (ground 1)", // Expected label
                true // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy mandatory alternative ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_MANDATORY_ALT,
                SecureOrFlexibleMandatoryGroundsAlternativeAccomm.PROPERTY_SOLD.name(),
                "Reason for property sold ground", // Reason
                "Property sold for redevelopment (ground 10A)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Secure or flexible tenancy discretionary alternative ground",
                ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY_ALT,
                SecureOrFlexibleDiscretionaryGroundsAlternativeAccomm.ADAPTED_ACCOMMODATION.name(),
                "Reason for adapted accomodation ground", // Reason
                "Adapted accommodation (ground 13)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Introductory, demoted or other tenancy mandatory alternative ground",
                ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER,
                IntroductoryDemotedOrOtherGrounds.ABSOLUTE_GROUNDS.name(),
                "Reason for absolute grounds", // Reason
                "Absolute grounds", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Introductory, demoted or other tenancy discretionary alternative ground",
                ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS,
                IntroductoryDemotedOrOtherNoGrounds.NO_GROUNDS.name(),
                "Reason for no grounds", // Reason
                "No grounds", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other mandatory ground",
                ClaimGroundCategory.WALES_STANDARD_OTHER_MANDATORY,
                MandatoryGroundWales.LANDLORD_BREAK_CLAUSE_S199.name(),
                "Reason for break clause ground", // Reason
                "Notice given under a landlord’s break clause (section 199)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other mandatory ground",
                ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY,
                DiscretionaryGroundWales.RENT_ARREARS_S157.name(),
                "Reason for rent arrears ground", // Reason
                "Rent arrears (breach of contract) (section 157)", // Expected label
                true // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other estate management ground",
                ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT,
                EstateManagementGroundsWales.REDEVELOPMENT_SCHEMES.name(),
                "Reason for redevelopment schemes ground", // Reason
                "Redevelopment schemes (ground B)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other mandatory ground",
                ClaimGroundCategory.WALES_SECURE_MANDATORY,
                SecureContractMandatoryGroundsWales.LANDLORD_NOTICE_S186.name(),
                "Reason for landlord notice ground", // Reason
                "Landlord’s notice in connection with end of fixed term given (section 186)", // Expected label
                false // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other mandatory ground",
                ClaimGroundCategory.WALES_SECURE_DISCRETIONARY,
                SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR_S157.name(),
                "Reason for antisocial behaviour ground", // Reason
                "Antisocial behaviour (breach of contract) (section 157)", // Expected label
                true // Is rent arrears ground
            ),
            argumentSet(
                "Wales standard or other estate management ground",
                ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT,
                EstateManagementGroundsWales.RESERVE_SUCCESSORS.name(),
                "Reason for reserve successors ground", // Reason
                "Reserve successors (ground G)", // Expected label
                false // Is rent arrears ground
            )
        );
    }


}
