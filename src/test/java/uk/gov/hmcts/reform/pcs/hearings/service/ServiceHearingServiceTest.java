package uk.gov.hmcts.reform.pcs.hearings.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.hearings.config.ServiceHearingValuesConfiguration;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceHearingServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String MANAGE_CASE_URL = "https://manage-case.example";
    private static final String CASE_NAME = "Jane Landlord vs John Tenant";

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private CaseNameFormatter caseNameFormatter;

    private ServiceHearingService underTest;

    @BeforeEach
    void setUp() {
        ServiceHearingValuesConfiguration defaults = new ServiceHearingValuesConfiguration();
        defaults.setHmctsServiceId("AAA3");
        defaults.setLocationType("Court");
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
            .extracting("locationType").isEqualTo("Court");
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

    private void givenCase(PcsCaseEntity pcsCase) {
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
    }

    private static PcsCaseEntity.PcsCaseEntityBuilder caseBuilder() {
        return PcsCaseEntity.builder().caseReference(CASE_REFERENCE);
    }

    private static List<CaseFlagEntity> flags(CaseFlagEntity... flags) {
        return new ArrayList<>(List.of(flags));
    }

    private static CaseFlagEntity flag(String name, String comment, int createdOrder) {
        return CaseFlagEntity.builder()
            .flagRefData(FlagRefDataEntity.builder().flagName(name).build())
            .flagComment(comment)
            .dateTimeCreated(LocalDateTime.of(2026, 1, createdOrder, 9, 0))
            .build();
    }
}
