package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentMoveComplexAuthorisationTest {

    private List<Map<String, Object>> rows;

    @BeforeEach
    void setup() {
        rows = new DocumentMoveComplexAuthorisation().buildRows("PCS-test");
    }

    @Test
    void shouldUpdateOnlyOnDocumentPathForInternalRoles() {
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("CaseTypeID")).isEqualTo("PCS-test");
            assertThat(row.get("UserRole")).isIn("judge", "fee-paid-judge", "circuit-judge", "leadership-judge",
                     "hearing-centre-admin", "hearing-centre-team-leader", "ctsc", "ctsc-team-leader",
                     "wlu-admin", "wlu-team-leader");
            assertThat(row.get("CRUD")).isIn("R", "RU");
        });
        assertThat(rows.stream()
            .filter(row -> row.get("UserRole").equals("judge") && row.get("CRUD").equals("RU"))
            .map(row -> row.get("CaseFieldID") + "." + row.get("ListElementCode")))
            .containsExactlyInAnyOrder(
                "detailsTab_TenancyLicenceDetails.tenancyLicenceDocuments",
                "detailsTab_NoticeDetails.noticeDocuments",
                "detailsTab_RentArrearsDetails.rentStatement");
    }

    @Test
    void shouldRetainReadAccessForDocumentSiblings() {
        assertThat(rows.stream()
            .filter(row -> row.get("UserRole").equals("judge")
                && row.get("CaseFieldID").equals("detailsTab_TenancyLicenceDetails")
                && row.get("CRUD").equals("R"))
            .map(row -> row.get("ListElementCode")))
            .containsExactlyInAnyOrder("typeOfTenancyLicence", "tenancyLicenceDescription", "tenancyLicenceDate",
                "hasCopyOfTenancyLicence", "reasonsForNoTenancyLicenceDocuments");
    }

}
