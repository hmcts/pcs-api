package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

@DisplayName("LegalCostsPlaceholder tests")
class LegalCostsPlaceholderTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new LegalCostsPlaceholder());
    }
}

