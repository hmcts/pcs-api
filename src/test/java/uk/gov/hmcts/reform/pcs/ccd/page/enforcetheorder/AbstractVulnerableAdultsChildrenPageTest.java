package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AbstractVulnerableAdultsChildrenPageTest {

    @Mock
    private TextAreaValidationService service;

    private ConcreteVulnerableAdultsChildrenPage pageUnderTest;

    private final List<String> errorList = List.of("error - too long");
    private final List<String> emptyErrorList = List.of();
    private PCSCase pcsCase;

    @BeforeEach
    void setup() {
        pcsCase = PCSCase.builder().build();
        EnforcementOrder enforcementOrder = EnforcementOrder.builder().build();
        pcsCase.setEnforcementOrder(enforcementOrder);
        pageUnderTest = new ConcreteVulnerableAdultsChildrenPage(service);
    }

    @Test
    void shouldReturnEmptyErrorsWhenNoText() {
        pageUnderTest.setValidationResult(emptyErrorList);
        pageUnderTest.setReturnedText(null);
        List<String> result = pageUnderTest.performValidation(pcsCase);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnErrorsWhenValidationFails() {
        pageUnderTest.setValidationResult(errorList);
        pageUnderTest.setReturnedText("some text");
        List<String> result = pageUnderTest.performValidation(pcsCase);

        assertThat(result).containsExactly("error - too long");
    }

    @Test
    void shouldReturnCorrectShowCondition() {
        String text = pageUnderTest.getVulnerablePeoplePresentShowCondition();

        assertThat(text).contains("vulnerablePeoplePresentWarrantRest=\"YES\"")
                .contains("vulnerableAdultsChildrenWarrantRest.vulnerableCategory=\"VULNERABLE_ADULTS\"")
                .contains("vulnerableAdultsChildrenWarrantRest.vulnerableCategory=\"VULNERABLE_CHILDREN\"")
                .contains("vulnerableAdultsChildrenWarrantRest.vulnerableCategory=\"VULNERABLE_ADULTS_AND_CHILDREN\"");

    }

    @Getter
    @Setter
    static class ConcreteVulnerableAdultsChildrenPage extends AbstractVulnerableAdultsChildrenPage {

        private String returnedText;
        private List<String> validationResult;

        public ConcreteVulnerableAdultsChildrenPage(TextAreaValidationService textAreaValidationService) {
            super(textAreaValidationService);
        }

        @Override
        public String getVulnerableReasonTextToValidate(PCSCase data) {
            return returnedText;
        }

        @Override
        public String getFieldSuffix() {
            return "WarrantRest";
        }

        @Override
        public List<String> getValidationErrors(String txt, String message, int maxLength) {
            if (returnedText == null) {
                assertThat(txt).isNull();
            } else {
                assertThat(txt).isEqualTo(returnedText);
            }
            return validationResult;
        }

        @Override
        public String getFieldPrefix() {
            return "vulnerableAdultsChildren";
        }
    }
}
