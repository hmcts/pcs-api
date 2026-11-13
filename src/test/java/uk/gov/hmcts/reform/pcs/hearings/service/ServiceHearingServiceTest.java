package uk.gov.hmcts.reform.pcs.hearings.service;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseLinkReasonEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.hearings.config.ServiceHearingValuesConfiguration;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceHearingServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String MANAGE_CASE_URL = "https://manage-case.example";
    private static final String CASE_NAME = "Jane Landlord vs John Tenant";
    private static final long LINKED_CASE_REFERENCE = 6543210987654321L;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private CaseNameFormatter caseNameFormatter;

    private ServiceHearingService underTest;

    @BeforeEach
    void setUp() {
        ServiceHearingValuesConfiguration defaults = new ServiceHearingValuesConfiguration();
        defaults.setHmctsServiceId("AAA3");
        defaults.setLocationType("court");
        defaults.setRoleTypes(List.of("24", "45"));
        defaults.setPreferredHearingChannel("INTER");

        underTest = new ServiceHearingService(defaults, pcsCaseRepository, caseNameFormatter, MANAGE_CASE_URL);
    }

    @Test
    void shouldThrowWhenCaseNotFound() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

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
    void shouldLeaveSlaStartDateEmptyWhenClaimNotIssued() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getCaseSlaStartDate()).isNull();
    }

    @Test
    void shouldCountActivePartiesPlusDutySolicitor() {
        PartyEntity claimant = PartyEntity.builder().firstName("Jane").build();
        PartyEntity defendant = PartyEntity.builder().firstName("John").build();
        PartyEntity removed = PartyEntity.builder().firstName("Gone").removed(true).build();
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
    void shouldOmitListingCommentsWhenNoCaseFlags() {
        givenCase(caseBuilder().build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isNull();
    }

    @Test
    void shouldFormatFlagWithoutComment() {
        givenCase(caseBuilder().caseFlags(flags(flag("Complex case", null, 1))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isEqualTo("Case Flags: Complex case");
    }

    @Test
    void shouldFormatFlagWithComment() {
        givenCase(caseBuilder().caseFlags(flags(flag("Urgent case", "so urgent", 1))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isEqualTo("Case Flags: Urgent case (Comments: so urgent)");
    }

    @Test
    void shouldJoinMultipleFlagsInCreationOrder() {
        givenCase(caseBuilder().caseFlags(flags(
            flag("Urgent case", "so urgent", 2),
            flag("Complex case", " ", 1)
        )).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments())
            .isEqualTo("Case Flags: Complex case, Urgent case (Comments: so urgent)");
    }

    @Test
    void shouldIncludeOnlyActiveFlags() {
        givenCase(caseBuilder().caseFlags(flags(
            flag("Complex case", null, 1, "Active"),
            flag("Urgent case", "so urgent", 2, "Inactive"),
            flag("Vulnerable user", null, 3, "Requested"),
            flag("Lack of capacity", null, 4, "Not approved")
        )).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isEqualTo("Case Flags: Complex case");
    }

    @Test
    void shouldOmitListingCommentsWhenNoActiveFlags() {
        givenCase(caseBuilder().caseFlags(flags(
            flag("Urgent case", "so urgent", 1, "Inactive"),
            flag("Complex case", null, 2, "Requested")
        )).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments()).isNull();
    }

    @Test
    void shouldNotTruncateLongListingComments() {
        String longComment = "x".repeat(2000);
        givenCase(caseBuilder().caseFlags(flags(flag("Urgent case", longComment, 1))).build());

        ServiceHearingValues values = underTest.getServiceHearingValues(CASE_REFERENCE);

        assertThat(values.getListingComments())
            .isEqualTo("Case Flags: Urgent case (Comments: " + longComment + ")");
    }

    @Test
    void shouldThrowWhenCaseNotFoundForLinkedCases() {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getServiceLinkedCases(CASE_REFERENCE))
            .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    void shouldReturnNoLinkedCasesWhenCaseHasNoLinks() {
        givenCase(caseBuilder().build());

        List<ServiceLinkedCases> linkedCases = underTest.getServiceLinkedCases(CASE_REFERENCE);

        assertThat(linkedCases).isEmpty();
    }

    @Test
    void shouldMapLinkedCaseReferenceNameAndReasons() {
        PcsCaseEntity linkedCase = PcsCaseEntity.builder().caseReference(LINKED_CASE_REFERENCE).build();
        givenCase(caseBuilder()
            .caseLinks(links(link(LINKED_CASE_REFERENCE, "CLRC001", "CLRC002")))
            .build());
        when(pcsCaseRepository.findByCaseReference(LINKED_CASE_REFERENCE)).thenReturn(Optional.of(linkedCase));
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
        when(pcsCaseRepository.findByCaseReference(LINKED_CASE_REFERENCE)).thenReturn(Optional.empty());

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
        when(pcsCaseRepository.findByCaseReference(LINKED_CASE_REFERENCE)).thenReturn(Optional.empty());

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
        when(pcsCaseRepository.findByCaseReference(LINKED_CASE_REFERENCE)).thenReturn(Optional.empty());
        when(pcsCaseRepository.findByCaseReference(secondLinkedCase)).thenReturn(Optional.empty());

        List<ServiceLinkedCases> linkedCases = underTest.getServiceLinkedCases(CASE_REFERENCE);

        assertThat(linkedCases)
            .extracting(ServiceLinkedCases::getCaseReference)
            .containsExactly("6543210987654321", "1111222233334444");
    }

    private void givenCase(PcsCaseEntity pcsCase) {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
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

    private static CaseFlagEntity flag(String name, String comment, int createdOrder) {
        return flag(name, comment, createdOrder, "Active");
    }

    private static CaseFlagEntity flag(String name, String comment, int createdOrder, String status) {
        return CaseFlagEntity.builder()
            .flagRefData(FlagRefDataEntity.builder().flagName(name).build())
            .flagComment(comment)
            .defaultStatus(status)
            .dateTimeCreated(LocalDateTime.of(2026, 1, createdOrder, 9, 0))
            .build();
    }
}
