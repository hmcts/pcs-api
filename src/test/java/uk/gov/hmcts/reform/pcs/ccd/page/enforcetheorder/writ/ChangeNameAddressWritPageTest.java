package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChangeNameAddressPage tests")
class ChangeNameAddressWritPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ChangeNameAddressWritPage());
    }

    @Test
    @DisplayName("Should allow progression without a blocking callback")
    void shouldNotRegisterBlockingCallback() {
        assertThat(event.getFields().getPagesToMidEvent())
            .doesNotContainKey("changeNameAddressWrit");
    }

}

