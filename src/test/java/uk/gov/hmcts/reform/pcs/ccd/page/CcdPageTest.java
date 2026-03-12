package uk.gov.hmcts.reform.pcs.ccd.page;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.VulnerableAdultsChildrenPage;

import static org.assertj.core.api.Assertions.assertThat;

class CcdPageTest {

    @Test
    void shouldReturnClassNameWithoutPageSuffixIfFound() {
        // Given
        Class<? extends CcdPage> clazz = VulnerableAdultsChildrenPage.class;

        // When
        String pageId = CcdPage.derivePageKey(clazz);

        // Then
        assertThat(pageId).isEqualTo("VulnerableAdultsChildren");
    }
}