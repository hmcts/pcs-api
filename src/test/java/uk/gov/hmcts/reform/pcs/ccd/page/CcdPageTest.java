package uk.gov.hmcts.reform.pcs.ccd.page;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.VulnerableAdultsChildrenPage;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("rawtypes")
class CcdPageTest {

    @Test
    void shouldReturnClassNameWithoutPageSuffixIfFound() {
        // Given
        Class clazz = VulnerableAdultsChildrenPage.class;

        // When
        String pageId = CcdPage.getPageId(clazz);

        // Then
        assertThat(pageId).isEqualTo("VulnerableAdultsChildren");
    }

    @Test
    void shouldReturnClassNameIfNoSuffixFound() {
        // Given
        Class clazz = EnforcementPageConfigurer.class;

        // When
        String pageId = CcdPage.getPageId(clazz);

        // Then
        assertThat(pageId).isEqualTo("EnforcementPageConfigurer");
    }
}