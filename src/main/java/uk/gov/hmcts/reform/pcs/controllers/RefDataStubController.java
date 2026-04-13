package uk.gov.hmcts.reform.pcs.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@RestController
public class RefDataStubController {

    private static final String SERVICE = "PCS";
    private static final String SERVICE_NAME = "Possessions";
    private static final String LOCATION_ID = "336559";
    private static final String LOCATION_NAME = "Central London County Court";
    private static final String REGION_ID = "1";

    @GetMapping(value = "/refdata/internal/staff/usersByServiceName", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> usersByServiceName(@RequestParam("ccd_service_names") String ccdServiceNames) {
        return List.of(
            staffUser("74e702fa-e20f-3a40-bc1d-d915f0874d00", "caseworker@pcs.com", "Case", "Worker"),
            staffUser("749ce9f7-535a-3cf5-ba07-f66e6d55c5fa", "judge@pcs.com", "Judge", "PCS")
        );
    }

    @GetMapping(value = "/refdata/location/court-venues/services", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> courtVenuesByService(@RequestParam("service_code") String serviceCode) {
        return Map.of("court_venues", buildCourtVenueList());
    }

    @GetMapping(value = "/refdata/location/court-venues", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> courtVenuesById(@RequestParam("epimms_id") String epimmsId) {
        return buildCourtVenueList();
    }

    @GetMapping(value = "/refdata/location/orgServices", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> orgServices() {
        return Map.of(
            "service_area", List.of(Map.ofEntries(
                entry("service_code", SERVICE),
                entry("ccd_service_name", SERVICE),
                entry("service_name", SERVICE_NAME),
                entry("service_short_description", SERVICE_NAME),
                entry("service_description", SERVICE_NAME),
                entry("jurisdiction", SERVICE),
                entry("order", 1)
            ))
        );
    }

    private Map<String, Object> staffUser(String userId, String email, String firstName, String lastName) {
        Map<String, Object> staffProfile = Map.ofEntries(
            entry("id", userId),
            entry("first_name", firstName),
            entry("last_name", lastName),
            entry("region_id", REGION_ID),
            entry("user_type", "Caseworker"),
            entry("idam_roles", "caseworker-pcs"),
            entry("suspended", "N"),
            entry("case_allocator", "N"),
            entry("task_supervisor", "N"),
            entry("staff_admin", "N"),
            entry("created_time", "2024-01-01T00:00:00Z"),
            entry("last_updated_time", "2024-01-01T00:00:00Z"),
            entry("email_id", email),
            entry("region", "London"),
            entry("base_location", List.of(Map.of(
                "location_id", LOCATION_ID,
                "location", LOCATION_NAME,
                "is_primary", true,
                "services", List.of(
                    Map.of(
                        "service_code", SERVICE,
                        "ccd_service_name", SERVICE,
                        "service_name", SERVICE_NAME
                    )
                )
            ))),
            entry("user_type_id", "1"),
            entry("role", List.of()),
            entry("skills", List.of()),
            entry("work_area", List.of())
        );

        return Map.of(
            "ccd_service_name", SERVICE,
            "staff_profile", staffProfile
        );
    }

    private List<Map<String, Object>> buildCourtVenueList() {
        return List.of(Map.of(
            "epimms_id", LOCATION_ID,
            "site_name", LOCATION_NAME,
            "is_case_management_location", "Y",
            "region_id", REGION_ID,
            "court_type", SERVICE_NAME,
            "service_code", SERVICE
        ));
    }
}
