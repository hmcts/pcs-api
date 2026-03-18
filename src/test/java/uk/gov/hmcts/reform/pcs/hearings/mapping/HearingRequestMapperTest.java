package uk.gov.hmcts.reform.pcs.hearings.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class HearingRequestMapperTest {

    private HearingRequestMapper mapper;

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String SERVICE_ID = "BBA3";
    private static final String HEARING_TYPE = "BBA3-SUB";
    private static final int DEFAULT_DURATION = 60;
    private static final String EXUI_URL = "https://manage-case.aat.platform.hmcts.net";

    @BeforeEach
    void setUp() {
        mapper = new HearingRequestMapper();
        setField(mapper, "hmctsServiceCode", SERVICE_ID);
        setField(mapper, "hearingType", HEARING_TYPE);
        setField(mapper, "defaultDuration", DEFAULT_DURATION);
        setField(mapper, "exuiUrl", EXUI_URL);
    }

    @Test
    void shouldBuildHearingRequestWithCaseDetails() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        HearingRequest result = mapper.buildHearingRequest(CASE_REFERENCE, pcsCase);

        assertThat(result).isNotNull();
        assertThat(result.getCaseDetails()).isNotNull();
        assertThat(result.getCaseDetails().getCaseRef()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(result.getCaseDetails().getHmctsServiceCode()).isEqualTo(SERVICE_ID);
        assertThat(result.getCaseDetails().getCaseDeepLink())
            .isEqualTo(EXUI_URL + "/cases/case-details/" + CASE_REFERENCE);
    }

    @Test
    void shouldBuildHearingDetails() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        HearingRequest result = mapper.buildHearingRequest(CASE_REFERENCE, pcsCase);

        assertThat(result.getHearingDetails()).isNotNull();
        assertThat(result.getHearingDetails().getHearingType()).isEqualTo(HEARING_TYPE);
        assertThat(result.getHearingDetails().getDuration()).isEqualTo(DEFAULT_DURATION);
        assertThat(result.getHearingDetails().getHearingInWelshFlag()).isFalse();
    }

    @Test
    void shouldSetWelshFlagForWalesCase() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .build();

        HearingRequest result = mapper.buildHearingRequest(CASE_REFERENCE, pcsCase);

        assertThat(result.getHearingDetails().getHearingInWelshFlag()).isTrue();
    }

    @Test
    void shouldMapIndividualParties() {
        Party defendant = new Party();
        defendant.setFirstName("Danny");
        defendant.setLastName("Defendant");

        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .allDefendants(List.of(ListValue.<Party>builder().id("1").value(defendant).build()))
            .build();

        HearingRequest result = mapper.buildHearingRequest(CASE_REFERENCE, pcsCase);

        assertThat(result.getPartyDetails()).hasSize(1);
        assertThat(result.getPartyDetails().getFirst().getIndividualDetails().getFirstName()).isEqualTo("Danny");
        assertThat(result.getPartyDetails().getFirst().getIndividualDetails().getLastName()).isEqualTo("Defendant");
        assertThat(result.getPartyDetails().getFirst().getPartyRole()).isEqualTo("RESP");
    }

    @Test
    void shouldMapOrganisationParty() {
        Party claimant = new Party();
        claimant.setOrgName("Test Org Ltd");

        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .allClaimants(List.of(ListValue.<Party>builder().id("1").value(claimant).build()))
            .build();

        HearingRequest result = mapper.buildHearingRequest(CASE_REFERENCE, pcsCase);

        assertThat(result.getPartyDetails()).hasSize(1);
        assertThat(result.getPartyDetails().getFirst().getOrganisationDetails().getName()).isEqualTo("Test Org Ltd");
        assertThat(result.getPartyDetails().getFirst().getPartyRole()).isEqualTo("APEL");
    }

    @Test
    void shouldHandleNullPartyLists() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        HearingRequest result = mapper.buildHearingRequest(CASE_REFERENCE, pcsCase);

        assertThat(result.getPartyDetails()).isEmpty();
    }

    @Test
    void shouldUseCaseManagementLocationWhenSet() {
        PCSCase pcsCase = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .caseManagementLocation(12345)
            .build();

        HearingRequest result = mapper.buildHearingRequest(CASE_REFERENCE, pcsCase);

        assertThat(result.getCaseDetails().getCaseManagementLocationCode()).isEqualTo("12345");
        assertThat(result.getHearingDetails().getHearingLocations().getFirst().getLocationId()).isEqualTo("12345");
    }
}
