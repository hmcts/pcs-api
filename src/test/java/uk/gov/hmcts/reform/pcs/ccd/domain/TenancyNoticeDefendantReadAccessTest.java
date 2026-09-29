package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantReadAccess;

import static org.assertj.core.api.Assertions.assertThat;

class TenancyNoticeDefendantReadAccessTest {

    @Test
    void tenancyLicenceDetailsFieldsGrantDefendantSolicitorReadAccess() throws Exception {
        var typeField = TenancyLicenceDetails.class.getDeclaredField("typeOfTenancyLicence");
        var dateField = TenancyLicenceDetails.class.getDeclaredField("tenancyLicenceDate");

        assertThat(typeField.getAnnotation(CCD.class).access()).contains(DefendantReadAccess.class);
        assertThat(dateField.getAnnotation(CCD.class).access()).contains(DefendantReadAccess.class);
    }
}
