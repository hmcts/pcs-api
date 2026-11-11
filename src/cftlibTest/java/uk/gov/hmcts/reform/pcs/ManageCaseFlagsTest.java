package uk.gov.hmcts.reform.pcs;

import feign.FeignException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.pcs.service.CaseCreationService;
import uk.gov.hmcts.rse.ccd.lib.test.CftlibTest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.pcs.auth.ServiceAuthorizationGenerator.generateTestS2SToken;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManageCaseFlagsTest extends CftlibTest {

    private static final List<String> EVENT_FIELDS =
        List.of("caseFlags", "parties", "allDefendants", "flagLauncherInternal");

    @Autowired
    private CoreCaseDataApi ccdApi;

    @Autowired
    private IdamClient idamClient;

    @Autowired
    private CaseCreationService caseCreationService;

    private String adminToken;
    private String solicitorToken;
    private final String s2s = generateTestS2SToken("ccd_gw");

    @BeforeAll
    void setUp() {
        solicitorToken = idamClient.getAccessToken("pcs-solicitor1@test.com", "password");
        adminToken = idamClient.getAccessToken("caseworker@pcs.com", "password");
    }

    @Test
    void shouldAmendAnExistingCaseLevelFlag() {
        long caseReference = caseCreationService.createMinimalCase(solicitorToken);

        createCaseLevelFlag(caseReference);

        StartEventResponse amendStart = startEvent(caseReference, "amendFlags");
        Map<String, Object> amendData = eventFieldsOnly(amendStart.getCaseDetails().getData());
        dump("amendFlags START", amendStart.getCaseDetails().getData());

        List<Map<String, Object>> details = caseFlagDetails(amendData);
        assertThat(details).as("existing case-level flag visible on amendFlags start").isNotEmpty();
        value(details.getFirst()).put("flagComment", "amended by regression test");

        submit(caseReference, "amendFlags", amendStart.getToken(), amendData);

        Map<String, Object> after = startEvent(caseReference, "amendFlags").getCaseDetails().getData();
        assertThat(value(caseFlagDetails(after).getFirst()).get("flagComment"))
            .isEqualTo("amended by regression test");
    }

    @Test
    void shouldAmendAnExistingPartyLevelFlag() {
        long caseReference = caseCreationService.createMinimalCase(solicitorToken);

        String partyId = createPartyLevelFlag(caseReference);

        StartEventResponse amendStart = startEvent(caseReference, "amendFlags");
        Map<String, Object> amendData = eventFieldsOnly(amendStart.getCaseDetails().getData());

        Map<String, Object> party = partyById(amendData, partyId);
        List<Map<String, Object>> details = listOf(mapOf(party.get("value")).get("defendantFlags") == null
                                                       ? null
                                                       : partyFlags(party).get("details"));
        assertThat(details).as("existing party-level flag visible on amendFlags start").isNotEmpty();

        String flagId = (String) details.getFirst().get("id");
        assertThat(flagId).as("party flag collection id").isNotNull();

        value(details.getFirst()).put("flagComment", "party flag amended by regression test");

        submit(caseReference, "amendFlags", amendStart.getToken(), amendData);

        Map<String, Object> after = startEvent(caseReference, "amendFlags").getCaseDetails().getData();
        List<Map<String, Object>> afterDetails = listOf(partyFlags(partyById(after, partyId)).get("details"));

        assertThat(afterDetails).hasSize(1);
        assertThat(afterDetails.getFirst().get("id")).as("flag identity retained").isEqualTo(flagId);
        assertThat(value(afterDetails.getFirst()).get("flagComment"))
            .isEqualTo("party flag amended by regression test");
    }

    private String createPartyLevelFlag(long caseReference) {
        StartEventResponse start = startEvent(caseReference, "createFlags");
        Map<String, Object> data = eventFieldsOnly(start.getCaseDetails().getData());

        Map<String, Object> party = listOf(data.get("parties")).getFirst();
        String partyId = (String) party.get("id");
        assertThat(partyId).as("party projected with a collection id").isNotNull();

        Map<String, Object> flags = partyFlags(party);
        List<Object> details = new ArrayList<>();
        details.add(Map.of("value", partyLevelFlagDetail(partyId)));
        flags.put("details", details);

        submit(caseReference, "createFlags", start.getToken(), data);

        return partyId;
    }

    private Map<String, Object> partyFlags(Map<String, Object> party) {
        return mapOf(mapOf(party.get("value")).get("defendantFlags"));
    }

    private Map<String, Object> partyById(Map<String, Object> data, String partyId) {
        return listOf(data.get("parties")).stream()
            .filter(party -> partyId.equals(party.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("party " + partyId + " missing from projection"));
    }

    private Map<String, Object> partyLevelFlagDetail(String partyId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", "Language Interpreter");
        detail.put("flagCode", "PF0015");
        detail.put("status", "Active");
        detail.put("hearingRelevant", "Yes");
        detail.put("availableExternally", "No");
        detail.put("path", List.of(Map.of("id", partyId, "value", "Party")));
        return detail;
    }

    @Test
    void shouldProjectEveryPartyWithACollectionId() {
        long caseReference = caseCreationService.createMinimalCase(solicitorToken);

        Map<String, Object> data = startEvent(caseReference, "amendFlags").getCaseDetails().getData();

        List<Map<String, Object>> parties = listOf(data.get("parties"));
        assertThat(parties).isNotEmpty();
        assertThat(parties).allSatisfy(party ->
            assertThat(party.get("id")).as("parties collection item id").isNotNull());
    }

    @Test
    void shouldRejectDataContainingFieldsTheEventDoesNotDefine() {
        long caseReference = caseCreationService.createMinimalCase(solicitorToken);

        StartEventResponse start = startEvent(caseReference, "amendFlags");
        Map<String, Object> data = eventFieldsOnly(start.getCaseDetails().getData());
        data.put("caseTitleMarkdown", "not a field on this event");

        assertThatThrownBy(() -> submit(caseReference, "amendFlags", start.getToken(), data))
            .isInstanceOf(FeignException.class)
            .hasMessageContaining("No field found");
    }

    private void createCaseLevelFlag(long caseReference) {
        Map<String, Object> flags = new LinkedHashMap<>();
        flags.put("visibility", "Internal");
        List<Object> details = new ArrayList<>();
        details.add(Map.of("value", caseLevelFlagDetail(caseReference)));
        flags.put("details", details);

        StartEventResponse start = startEvent(caseReference, "createFlags");
        Map<String, Object> data = eventFieldsOnly(start.getCaseDetails().getData());
        data.put("caseFlags", flags);

        submit(caseReference, "createFlags", start.getToken(), data);
    }

    private Map<String, Object> caseLevelFlagDetail(long caseReference) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("name", "Urgent case - ODS");
        detail.put("flagCode", "CF0007");
        detail.put("status", "Active");
        detail.put("hearingRelevant", "No");
        detail.put("availableExternally", "No");
        detail.put("path", List.of(Map.of("id", Long.toString(caseReference), "value", "Case")));
        return detail;
    }

    private List<Map<String, Object>> caseFlagDetails(Map<String, Object> data) {
        Map<String, Object> caseFlags = mapOf(data.get("caseFlags"));
        return caseFlags == null ? List.of() : listOf(caseFlags.get("details"));
    }

    private Map<String, Object> value(Map<String, Object> listValue) {
        return mapOf(listValue.get("value"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOf(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(Object o) {
        return o == null ? List.of() : (List<Map<String, Object>>) o;
    }

    private Map<String, Object> eventFieldsOnly(Map<String, Object> data) {
        Map<String, Object> submitted = new LinkedHashMap<>();
        EVENT_FIELDS.stream().filter(data::containsKey).forEach(key -> submitted.put(key, data.get(key)));
        return submitted;
    }

    private StartEventResponse startEvent(long caseReference, String eventId) {
        return ccdApi.startEvent(adminToken, s2s, Long.toString(caseReference), eventId);
    }

    private void submit(long caseReference, String eventId, String token, Map<String, Object> data) {
        ccdApi.createEvent(adminToken, s2s, Long.toString(caseReference),
            CaseDataContent.builder()
                .data(data)
                .event(Event.builder().id(eventId).build())
                .eventToken(token)
                .build());
    }

    private void dump(String label, Map<String, Object> data) {
        System.out.println("### " + label + " parties=" + data.get("parties"));
        System.out.println("### " + label + " caseFlags=" + data.get("caseFlags"));
    }
}
