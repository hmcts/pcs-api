package uk.gov.hmcts.reform.pcs.hearings.service;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.hearings.config.ServiceHearingValuesConfiguration;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingWindow;
import uk.gov.hmcts.reform.pcs.hearings.model.IndividualDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.OrganisationDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.PartyDetails;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceHearingValuesMapperTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String MANAGE_CASE_URL = "https://manage-case.example";
    private static final String CASE_NAME = "Jane Landlord vs John Tenant";
    private static final long LINKED_CASE_REFERENCE = 6543210987654321L;
    private static final String STANDARD_PRIORITY = "Standard";
    private static final String URGENT_PRIORITY = "Urgent";
    private static final String URGENT_CASE_FLAG_CODE = "CF0007";

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private CaseNameFormatter caseNameFormatter;
    @Mock
    private IndividualDetailsFlagMapper individualDetailsFlagMapper;

    private ServiceHearingValuesMapper underTest;

    @BeforeEach
    void setUp() {
        ServiceHearingValuesConfiguration defaults = new ServiceHearingValuesConfiguration();
        defaults.setHmctsServiceId("AAA3");
        defaults.setLocationType("court");
        defaults.setRoleTypes(List.of("24", "45"));
        defaults.setPreferredHearingChannel("INTER");
        defaults.setHearingType("AAA3-FPH");
        defaults.setHearingChannels(List.of("INTER"));
        defaults.setHearingPriorityStandard(STANDARD_PRIORITY);
        defaults.setHearingPriorityUrgent(URGENT_PRIORITY);
        defaults.setNumberOfDaysStartOffset(27);
        defaults.setNumberOfDaysOffset(57);

        underTest = new ServiceHearingValuesMapper(
            defaults, pcsCaseService, caseNameFormatter, individualDetailsFlagMapper, MANAGE_CASE_URL);
        lenient().when(individualDetailsFlagMapper.map(any())).thenReturn(new IndividualDetails());
    }

    @Test
    void shouldThrowWhenCaseNotFound() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenThrow(new CaseNotFoundException(CASE_REFERENCE));

        assertThatThrownBy(() -> underTest.getServiceHearingValues(CASE_REFERENCE))
            .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    void shouldKeepConfiguredDefaults() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHmctsServiceID()).isEqualTo("AAA3");
        assertThat(values.getHearingLocations()).singleElement()
            .extracting("locationType").isEqualTo("court");
        assertThat(values.getPanelRequirements().getRoleType()).containsExactly("24", "45");
        assertThat(values.getHearingType()).isEqualTo("AAA3-FPH");
        assertThat(values.getHearingChannels()).containsExactly("INTER");
    }

    @Test
    void shouldSetCaseManagementLocationCodeAndLocationIdToCaseManagementLocation() {
        givenCase(caseBuilder().caseManagementLocation(20262).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getCaseManagementLocationCode()).isEqualTo("20262");
        assertThat(values.getHearingLocations()).singleElement()
            .extracting("locationId").isEqualTo("20262");
    }

    @Test
    void shouldLeaveLocationCodesEmptyWhenCaseHasNoCaseManagementLocation() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getCaseManagementLocationCode()).isNull();
        assertThat(values.getHearingLocations()).singleElement()
            .extracting("locationId").isNull();
    }

    @Test
    void shouldSetHearingWindowFromClaimIssuedDatePlusConfiguredOffsets() {
        givenCase(caseIssuedOn(LocalDateTime.of(2027, 1, 1, 10, 0)));

        HearingWindow hearingWindow = underTest.getServiceHearingValues(CASE_REFERENCE).getHearingWindow();

        assertThat(hearingWindow.getDateRangeStart()).isEqualTo(LocalDate.of(2027, 1, 28));
        assertThat(hearingWindow.getDateRangeEnd()).isEqualTo(LocalDate.of(2027, 2, 27));
        assertThat(hearingWindow.getFirstDateTimeMustBe()).isNull();
    }

    @Test
    void shouldCountHearingWindowFromUkDateOfClaimIssue() {
        // 23:30 UTC in British Summer Time is 00:30 on 2 June in the UK
        givenCase(caseIssuedOn(LocalDateTime.of(2026, 6, 1, 23, 30)));

        HearingWindow hearingWindow = underTest.getServiceHearingValues(CASE_REFERENCE).getHearingWindow();

        assertThat(hearingWindow.getDateRangeStart()).isEqualTo(LocalDate.of(2026, 6, 29));
        assertThat(hearingWindow.getDateRangeEnd()).isEqualTo(LocalDate.of(2026, 7, 29));
    }

    @Test
    void shouldLeaveHearingWindowEmptyWhenCaseHasNoClaim() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingWindow()).isNull();
    }

    @Test
    void shouldLeaveHearingWindowEmptyWhenMainClaimNotIssued() {
        givenCase(caseIssuedOn(null));

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingWindow()).isNull();
    }

    @Test
    void shouldSetStandardPriorityByDefault() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingPriorityType()).isEqualTo(STANDARD_PRIORITY);
    }

    @Test
    void shouldSetUrgentPriorityWhenCaseHasActiveUrgentFlag() {
        givenCase(caseBuilder().caseFlags(flags(flagWithCode(URGENT_CASE_FLAG_CODE, "Active"))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingPriorityType()).isEqualTo(URGENT_PRIORITY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Inactive", "Requested", "Not approved"})
    void shouldSetStandardPriorityWhenUrgentFlagIsNotActive(String status) {
        givenCase(caseBuilder().caseFlags(flags(flagWithCode(URGENT_CASE_FLAG_CODE, status))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingPriorityType()).isEqualTo(STANDARD_PRIORITY);
    }

    @Test
    void shouldSetStandardPriorityWhenActiveFlagIsNotUrgent() {
        givenCase(caseBuilder().caseFlags(flags(
            flagWithCode("CF0002", "Active"),
            flag("Urgent case", null)
        )).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingPriorityType()).isEqualTo(STANDARD_PRIORITY);
    }

    @Test
    void shouldSetUrgentPriorityWhenCaseHasOpenApplicationToSuspend() {
        givenCase(caseWithGenApp(GenAppType.SUSPEND, GenAppState.GEN_APP_ISSUED));

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingPriorityType()).isEqualTo(URGENT_PRIORITY);
    }

    @ParameterizedTest
    @EnumSource(value = GenAppState.class, names = "GEN_APP_ISSUED", mode = EnumSource.Mode.EXCLUDE)
    @NullSource
    void shouldSetStandardPriorityWhenApplicationToSuspendIsNotOpen(GenAppState state) {
        givenCase(caseWithGenApp(GenAppType.SUSPEND, state));

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingPriorityType()).isEqualTo(STANDARD_PRIORITY);
    }

    @ParameterizedTest
    @EnumSource(value = GenAppType.class, names = "SUSPEND", mode = EnumSource.Mode.EXCLUDE)
    void shouldSetStandardPriorityWhenOpenApplicationIsNotToSuspend(GenAppType type) {
        givenCase(caseWithGenApp(type, GenAppState.GEN_APP_ISSUED));

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getHearingPriorityType()).isEqualTo(STANDARD_PRIORITY);
    }

    @Test
    void shouldSetCaseIdAndCaseNamesFromPcsData() {
        PcsCaseEntity pcsCase = caseBuilder().build();
        givenCase(pcsCase);
        when(caseNameFormatter.formatCaseName(pcsCase)).thenReturn(CASE_NAME);

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getCaseId()).isEqualTo("1234567890123456");
        assertThat(values.getHmctsInternalCaseName()).isEqualTo(CASE_NAME);
        assertThat(values.getPublicCaseName()).isEqualTo(CASE_NAME);
    }

    @Test
    void shouldLinkToCaseFileViewTab() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getCaseDeepLink())
            .isEqualTo(MANAGE_CASE_URL + "/cases/case-details/PCS/PCS/1234567890123456#Case%20File%20View");
    }

    @Test
    void shouldSetSlaStartDateToUkDateOfClaimIssue() {
        // 23:30 UTC in British Summer Time is 00:30 the next day in the UK
        ClaimEntity claim = ClaimEntity.builder()
            .claimIssuedDate(LocalDateTime.of(2026, 6, 1, 23, 30))
            .build();
        givenCase(caseBuilder().claims(new ArrayList<>(List.of(claim))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getCaseSlaStartDate()).isEqualTo(LocalDate.of(2026, 6, 2));
    }

    @Test
    void shouldLeaveSlaStartDateEmptyWhenCaseHasNoClaim() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getCaseSlaStartDate()).isNull();
    }

    @Test
    void shouldLeaveSlaStartDateEmptyWhenMainClaimNotIssued() {
        ClaimEntity claim = ClaimEntity.builder().build();
        givenCase(caseBuilder().claims(new ArrayList<>(List.of(claim))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getCaseSlaStartDate()).isNull();
    }

    @Test
    void shouldCountActivePartiesPlusDutySolicitor() {
        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).firstName("Jane").build();
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).firstName("John").build();
        PartyEntity removed = PartyEntity.builder().id(UUID.randomUUID()).firstName("Gone").removed(true).build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(claimant, defendant, removed))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getNumberOfPhysicalAttendees()).isEqualTo(3);
    }

    @Test
    void shouldCountOnlyDutySolicitorWhenNoParties() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getNumberOfPhysicalAttendees()).isEqualTo(1);
    }

    @Test
    void shouldUseFlagMapperResultForPartyIndividualDetails() {
        List<CasePartyFlagEntity> partyFlags = new ArrayList<>(List.of(new CasePartyFlagEntity()));
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).defendantFlags(partyFlags).build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(defendant))).build());
        IndividualDetails mapped = new IndividualDetails();
        when(individualDetailsFlagMapper.map(partyFlags)).thenReturn(mapped);

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement()
            .satisfies(party -> assertThat(party.getIndividualDetails()).isSameAs(mapped));
    }

    @Test
    void shouldSetConfiguredHearingChannelOnPartyIndividualDetails() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(defendant))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement()
            .extracting(party -> party.getIndividualDetails().getPreferredHearingChannel())
            .isEqualTo("INTER");
    }

    @Test
    void shouldAddOnePartyPerActivePartyAndSkipRemovedParties() {
        PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).firstName("Jane").build();
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).firstName("John").build();
        PartyEntity removed = PartyEntity.builder().id(UUID.randomUUID()).firstName("Gone").removed(true).build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(claimant, defendant, removed))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).hasSize(2);
        verify(individualDetailsFlagMapper, times(2)).map(any());
    }

    @Test
    void shouldKeepDefaultPartyWhenCaseHasNoActiveParties() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement()
            .extracting(party -> party.getIndividualDetails().getPreferredHearingChannel())
            .isEqualTo("INTER");
        verifyNoInteractions(individualDetailsFlagMapper);
    }

    @Test
    void shouldSetIdentityAndContactDetailsForIndividualParty() {
        UUID partyId = UUID.randomUUID();
        PartyEntity defendant = PartyEntity.builder()
            .id(partyId)
            .firstName("John")
            .lastName("Tenant")
            .emailAddress(" john@example.com ")
            .phoneNumber("07700900123")
            .build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(defendant))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement().satisfies(party -> {
            assertThat(party.getPartyID()).isEqualTo(partyId.toString());
            assertThat(party.getPartyType()).isEqualTo("IND");
            assertThat(party.getOrganisationDetails()).isNull();
            IndividualDetails details = party.getIndividualDetails();
            assertThat(details.getFirstName()).isEqualTo("John");
            assertThat(details.getLastName()).isEqualTo("Tenant");
            assertThat(details.getHearingChannelEmail()).containsExactly("john@example.com");
            assertThat(details.getHearingChannelPhone()).containsExactly("07700900123");
            assertThat(details.getPreferredHearingChannel()).isEqualTo("INTER");
            assertThat(details.getCustodyStatus()).isNull();
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldOmitEmailAndPhoneWhenNotProvided(String contact) {
        PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .emailAddress(contact)
            .phoneNumber(contact)
            .build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(defendant))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement().satisfies(party -> {
            assertThat(party.getIndividualDetails().getHearingChannelEmail()).isNull();
            assertThat(party.getIndividualDetails().getHearingChannelPhone()).isNull();
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldTreatPartyWithoutOrganisationNameAsIndividual(String orgName) {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).orgName(orgName).build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(defendant))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement()
            .extracting(PartyDetails::getPartyType)
            .isEqualTo("IND");
    }

    @Test
    void shouldSetOrganisationDetailsInsteadOfIndividualDetailsForOrganisationParty() {
        UUID partyId = UUID.randomUUID();
        PartyEntity claimant = PartyEntity.builder()
            .id(partyId)
            .orgName("Treetops Housing")
            .organisationId("ORG1234")
            .emailAddress("housing@example.com")
            .build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(claimant))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement().satisfies(party -> {
            assertThat(party.getPartyID()).isEqualTo(partyId.toString());
            assertThat(party.getPartyType()).isEqualTo("ORG");
            assertThat(party.getIndividualDetails()).isNull();
            OrganisationDetails details = party.getOrganisationDetails();
            assertThat(details.getName()).isEqualTo("Treetops Housing");
            assertThat(details.getOrganisationType()).isEqualTo("ORG");
            assertThat(details.getCftOrganisationID()).isEqualTo("ORG1234");
        });
        verifyNoInteractions(individualDetailsFlagMapper);
    }

    @Test
    void shouldSetPartyRoleFromEntityRoleCode() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).entityRoleCodeKey("DEFENDANT-ROLE").build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(defendant))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement()
            .extracting(PartyDetails::getPartyRole)
            .isEqualTo("DEFENDANT-ROLE");
    }

    @Test
    void shouldSetPartyRoleForOrganisationParty() {
        PartyEntity claimant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .orgName("Treetops Housing")
            .entityRoleCodeKey("CLAIMANT-ROLE")
            .build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(claimant))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement()
            .extracting(PartyDetails::getPartyRole)
            .isEqualTo("CLAIMANT-ROLE");
    }

    @Test
    void shouldOmitPartyRoleWhenPartyHasNoRoleCode() {
        PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
        givenCase(caseBuilder().parties(new HashSet<>(Set.of(defendant))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getParties()).singleElement()
            .extracting(PartyDetails::getPartyRole)
            .isNull();
    }

    @Test
    void shouldOmitListingCommentsWhenNoCaseFlags() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isNull();
    }

    @Test
    void shouldFormatFlagWithoutComment() {
        givenCase(caseBuilder().caseFlags(flags(flag("Complex case", null))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isEqualTo("Case Flags: Complex case");
    }

    @Test
    void shouldFormatFlagWithComment() {
        givenCase(caseBuilder().caseFlags(flags(flag("Urgent case", "so urgent"))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isEqualTo("Case Flags: Urgent case (Comments: so urgent)");
    }

    @Test
    void shouldJoinMultipleFlagsWithComma() {
        givenCase(caseBuilder().caseFlags(flags(
            flag("Complex case", " "),
            flag("Urgent case", "so urgent")
        )).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments())
            .isEqualTo("Case Flags: Complex case, Urgent case (Comments: so urgent)");
    }

    @Test
    void shouldIncludeOnlyActiveFlags() {
        givenCase(caseBuilder().caseFlags(flags(
            flag("Complex case", null, "Active"),
            flag("Urgent case", "so urgent", "Inactive"),
            flag("Vulnerable user", null, "Requested"),
            flag("Lack of capacity", null, "Not approved")
        )).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isEqualTo("Case Flags: Complex case");
    }

    @Test
    void shouldOmitListingCommentsWhenNoActiveFlags() {
        givenCase(caseBuilder().caseFlags(flags(
            flag("Urgent case", "so urgent", "Inactive"),
            flag("Complex case", null, "Requested")
        )).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isNull();
    }

    @Test
    void shouldExcludeFlagsWithoutReferenceData() {
        CaseFlagEntity noRefData = flag("Ignored", "ignored");
        noRefData.setFlagRefData(null);
        givenCase(caseBuilder().caseFlags(flags(noRefData, flag("Complex case", null))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isEqualTo("Case Flags: Complex case");
    }

    @Test
    void shouldTrimNamesAndCommentsButKeepWhitespaceInsideComment() {
        givenCase(caseBuilder().caseFlags(flags(
            flag("  Urgent case ", "  so   urgent \n"),
            flag(" Complex case ", "")
        )).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments())
            .isEqualTo("Case Flags: Urgent case (Comments: so   urgent), Complex case");
    }

    @Test
    void shouldCapCommentAtMaxCommentLengthWhenFullTextIsTooLong() {
        String longComment = "x".repeat(ServiceHearingValuesMapper.MAX_LISTING_COMMENTS_LENGTH);
        givenCase(caseBuilder().caseFlags(flags(flag("Urgent case", longComment))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        int keptLength = ServiceHearingValuesMapper.MAX_COMMENT_LENGTH - ServiceHearingValuesMapper.ELLIPSIS.length();
        assertThat(values.getListingComments())
            .isEqualTo("Case Flags: Urgent case (Comments: " + "x".repeat(keptLength) + "...)");
    }

    @Test
    void shouldKeepFullTextWhenEightOrMoreCommentedFlagsFit() {
        List<CaseFlagEntity> caseFlags = flags();
        int flagCount = ServiceHearingValuesMapper.FULL_TEXT_MAX_COMMENTED_FLAGS + 2;
        for (int i = 1; i <= flagCount; i++) {
            caseFlags.add(flag("Flag " + i, "comment " + i));
        }
        givenCase(caseBuilder().caseFlags(caseFlags).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        String expected = "Case Flags: " + IntStream.rangeClosed(1, flagCount)
            .mapToObj(i -> "Flag " + i + " (Comments: comment " + i + ")")
            .collect(Collectors.joining(", "));
        assertThat(values.getListingComments()).isEqualTo(expected);
    }

    @Test
    void shouldShareSpaceFairlyKeepingShortCommentsWholeAndNamesInFull() {
        // 12 flags: 10 with 200-character comments and 2 with short ones. The full text would be ~2,300 characters.
        // Fixed text is 274 characters, leaving 1,726 to share: 5 + 5 for the short comments, then 171-172 each.
        List<CaseFlagEntity> caseFlags = flags();
        for (int i = 1; i <= 12; i++) {
            String comment = i == 5 || i == 12 ? "short" : "x".repeat(ServiceHearingValuesMapper.MAX_COMMENT_LENGTH);
            caseFlags.add(flag(String.format("Flag %02d", i), comment));
        }
        givenCase(caseBuilder().caseFlags(caseFlags).build());

        String listingComments = underTest.getServiceHearingValues(CASE_REFERENCE).getListingComments();

        assertThat(listingComments)
            .hasSize(ServiceHearingValuesMapper.MAX_LISTING_COMMENTS_LENGTH)
            .startsWith("Case Flags: ");
        List<String> entries = List.of(listingComments.substring("Case Flags: ".length()).split(", "));
        assertThat(entries).hasSize(12);
        for (int i = 1; i <= 12; i++) {
            String entry = entries.get(i - 1);
            String name = String.format("Flag %02d", i);
            if (i == 5 || i == 12) {
                assertThat(entry).isEqualTo(name + " (Comments: short)");
            } else {
                assertThat(entry).matches(name + " \\(Comments: x{168,169}\\.\\.\\.\\)");
            }
        }
    }

    @Test
    void shouldCutListingCommentsAtMaxLengthWhenFlagNamesAloneAreTooLong() {
        List<CaseFlagEntity> caseFlags = flags();
        for (int i = 1; i <= 100; i++) {
            caseFlags.add(flag(String.format("Flag name number %03d", i), "comment"));
        }
        givenCase(caseBuilder().caseFlags(caseFlags).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments())
            .hasSize(ServiceHearingValuesMapper.MAX_LISTING_COMMENTS_LENGTH)
            .startsWith("Case Flags: Flag name number 001, Flag name number 002");
    }

    @Test
    void shouldNotShortenCommentThatFits() {
        assertThat(ServiceHearingValuesMapper.shorten("so urgent", 9)).isEqualTo("so urgent");
    }

    @Test
    void shouldShortenAtWordBoundaryAndDropTrailingWhitespace() {
        assertThat(ServiceHearingValuesMapper.shorten("the quick brown fox", 15)).isEqualTo("the quick...");
        assertThat(ServiceHearingValuesMapper.shorten("hello   world", 10)).isEqualTo("hello...");
    }

    @Test
    void shouldShortenMidWordWhenLastWordBoundaryIsTooEarly() {
        String comment = "a " + "x".repeat(50);

        assertThat(ServiceHearingValuesMapper.shorten(comment, 20))
            .hasSize(20)
            .isEqualTo("a " + "x".repeat(15) + "...");
    }

    @Test
    void shouldShortenMidWordWhenCommentHasNoWhitespace() {
        assertThat(ServiceHearingValuesMapper.shorten("x".repeat(50), 10)).isEqualTo("xxxxxxx...");
    }

    @Test
    void shouldDropCommentWhenNoRoomForTextBeforeEllipsis() {
        assertThat(ServiceHearingValuesMapper.shorten("so urgent", 3)).isNull();
        assertThat(ServiceHearingValuesMapper.shorten("so urgent", 0)).isNull();
    }

    @Test
    void shouldNotTruncateListingCommentsAtMaxLength() {
        String prefix = "Case Flags: Urgent case (Comments: ";
        String comment = "x".repeat(ServiceHearingValuesMapper.MAX_LISTING_COMMENTS_LENGTH - prefix.length() - 1);
        givenCase(caseBuilder().caseFlags(flags(flag("Urgent case", comment))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments())
            .hasSize(ServiceHearingValuesMapper.MAX_LISTING_COMMENTS_LENGTH)
            .isEqualTo(prefix + comment + ")");
    }
    

    @Test
    void shouldThrowWhenCaseNotFoundForLinkedCases() {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenThrow(new CaseNotFoundException(CASE_REFERENCE));

        assertThatThrownBy(() -> underTest.getServiceLinkedCases(CASE_REFERENCE))
            .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    void shouldReturnNoLinkedCasesWhenCaseHasNoLinks() {
        givenCase(caseBuilder().build());

        List<ServiceLinkedCases> linkedCases = underTest.getServiceLinkedCases(CASE_REFERENCE);

        assertThat(linkedCases).isEmpty();
        verify(pcsCaseService, never()).findCasesByReferences(any());
    }

    @Test
    void shouldMapLinkedCaseReferenceNameAndReasons() {
        PcsCaseEntity linkedCase = PcsCaseEntity.builder().caseReference(LINKED_CASE_REFERENCE).build();
        givenCase(caseBuilder()
            .caseLinks(links(link(LINKED_CASE_REFERENCE, "CLRC001", "CLRC002")))
            .build());
        when(pcsCaseService.findCasesByReferences(List.of(LINKED_CASE_REFERENCE)))
            .thenReturn(List.of(linkedCase));
        when(caseNameFormatter.formatCaseName(linkedCase)).thenReturn(CASE_NAME);

        List<ServiceLinkedCases> linkedCases = underTest.getServiceLinkedCases(CASE_REFERENCE);

        assertThat(linkedCases).singleElement().satisfies(linked -> {
            assertThat(linked.getCaseReference()).isEqualTo("6543210987654321");
            assertThat(linked.getCaseName()).isEqualTo(CASE_NAME);
            assertThat(linked.getReasonsForLink()).containsExactly("CLRC001", "CLRC002");
        });
    }

    @Test
    void shouldLeaveCaseNameEmptyWhenLinkedCaseIsNotInPcs() {
        givenCase(caseBuilder()
            .caseLinks(links(link(LINKED_CASE_REFERENCE, "CLRC001")))
            .build());
        when(pcsCaseService.findCasesByReferences(List.of(LINKED_CASE_REFERENCE))).thenReturn(List.of());

        List<ServiceLinkedCases> linkedCases = underTest.getServiceLinkedCases(CASE_REFERENCE);

        assertThat(linkedCases).singleElement().satisfies(linked -> {
            assertThat(linked.getCaseReference()).isEqualTo("6543210987654321");
            assertThat(linked.getCaseName()).isNull();
            assertThat(linked.getReasonsForLink()).containsExactly("CLRC001");
        });
        verifyNoInteractions(caseNameFormatter);
    }

    @Test
    void shouldReturnEmptyReasonsWhenLinkHasNoReasons() {
        givenCase(caseBuilder()
            .caseLinks(links(link(LINKED_CASE_REFERENCE)))
            .build());
        when(pcsCaseService.findCasesByReferences(List.of(LINKED_CASE_REFERENCE))).thenReturn(List.of());

        List<ServiceLinkedCases> linkedCases = underTest.getServiceLinkedCases(CASE_REFERENCE);

        assertThat(linkedCases).singleElement()
            .extracting(ServiceLinkedCases::getReasonsForLink)
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .isEmpty();
    }

    @Test
    void shouldReturnEveryLinkedCaseInStoredOrder() {
        long secondLinkedCase = 1111222233334444L;
        givenCase(caseBuilder()
            .caseLinks(links(link(LINKED_CASE_REFERENCE, "CLRC001"), link(secondLinkedCase, "CLRC003")))
            .build());
        when(pcsCaseService.findCasesByReferences(List.of(LINKED_CASE_REFERENCE, secondLinkedCase)))
            .thenReturn(List.of());

        List<ServiceLinkedCases> linkedCases = underTest.getServiceLinkedCases(CASE_REFERENCE);

        assertThat(linkedCases)
            .extracting(ServiceLinkedCases::getCaseReference)
            .containsExactly("6543210987654321", "1111222233334444");
    }

    @Test
    void shouldLookUpEveryLinkedCaseInOneQuery() {
        long secondLinkedCase = 1111222233334444L;
        PcsCaseEntity firstCase = PcsCaseEntity.builder().caseReference(LINKED_CASE_REFERENCE).build();
        PcsCaseEntity secondCase = PcsCaseEntity.builder().caseReference(secondLinkedCase).build();
        givenCase(caseBuilder()
            .caseLinks(links(
                link(LINKED_CASE_REFERENCE, "CLRC001"),
                link(secondLinkedCase, "CLRC003"),
                link(LINKED_CASE_REFERENCE, "CLRC002")))
            .build());
        when(pcsCaseService.findCasesByReferences(List.of(LINKED_CASE_REFERENCE, secondLinkedCase)))
            .thenReturn(List.of(secondCase, firstCase));
        when(caseNameFormatter.formatCaseName(firstCase)).thenReturn(CASE_NAME);
        when(caseNameFormatter.formatCaseName(secondCase)).thenReturn("Other Landlord vs Other Tenant");

        List<ServiceLinkedCases> linkedCases = underTest.getServiceLinkedCases(CASE_REFERENCE);

        assertThat(linkedCases)
            .extracting(ServiceLinkedCases::getCaseName)
            .containsExactly(CASE_NAME, "Other Landlord vs Other Tenant", CASE_NAME);
        verify(pcsCaseService, times(1)).findCasesByReferences(any());
        verify(pcsCaseService, never()).loadCase(LINKED_CASE_REFERENCE);
        verify(pcsCaseService, never()).loadCase(secondLinkedCase);
    }

    private void givenCase(PcsCaseEntity pcsCase) {
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCase);
    }

    private static List<CaseLinkEntity> links(CaseLinkEntity... links) {
        return new ArrayList<>(List.of(links));
    }

    private static CaseLinkEntity link(long linkedCaseReference, String... reasonCodes) {
        CaseLinkEntity link = CaseLinkEntity.builder()
            .linkedCaseReference(linkedCaseReference)
            .ccdListId("PCS")
            .build();
        for (String reasonCode : reasonCodes) {
            link.getReasons().add(CaseLinkReasonEntity.builder().caseLink(link).reasonCode(reasonCode).build());
        }
        return link;
    }

    private static PcsCaseEntity.PcsCaseEntityBuilder caseBuilder() {
        return PcsCaseEntity.builder().caseReference(CASE_REFERENCE);
    }

    private static List<CaseFlagEntity> flags(CaseFlagEntity... flags) {
        return new ArrayList<>(List.of(flags));
    }

    private static CaseFlagEntity flag(String name, String comment) {
        return flag(name, comment, "Active");
    }

    private static CaseFlagEntity flag(String name, String comment, String status) {
        return CaseFlagEntity.builder()
            .flagRefData(FlagRefDataEntity.builder().flagName(name).build())
            .flagComment(comment)
            .defaultStatus(status)
            .build();
    }

    private static CaseFlagEntity flagWithCode(String flagCode, String status) {
        return CaseFlagEntity.builder()
            .flagRefData(FlagRefDataEntity.builder().flagCode(flagCode).flagName("Flag " + flagCode).build())
            .defaultStatus(status)
            .build();
    }

    private static PcsCaseEntity caseIssuedOn(LocalDateTime claimIssuedDate) {
        ClaimEntity claim = ClaimEntity.builder().claimIssuedDate(claimIssuedDate).build();
        return caseBuilder().claims(new ArrayList<>(List.of(claim))).build();
    }

    private static PcsCaseEntity caseWithGenApp(GenAppType type, GenAppState state) {
        PcsCaseEntity pcsCase = caseBuilder().build();
        pcsCase.getGenApps().add(GenAppEntity.builder().type(type).state(state).build());
        return pcsCase;
    }
}
