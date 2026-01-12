package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class NonProdSupportService {

    static final String NO_NON_PROD_CASE_AVAILABLE = "No non-prod case json available.";
    static final String TEST_CASE_CREATION_NOT_SUPPORTED = "Strategy not supported : ";
    static final String FAILED_TO_GENERATE_TEST_CASE = "Failed to generate Test Case";

    private final CaseSupportHelper caseSupportHelper;
    private final List<TestCaseGenerationStrategy> testCaseGenerationStrategies;

    public CaseSupportGenerationResponse caseGenerator(long caseReference, PCSCase fromEvent) {
        try {
            DynamicList dynamicList = fromEvent.getNonProdSupportFileList();
            if (dynamicList == null) {
                throw new IllegalArgumentException(NO_NON_PROD_CASE_AVAILABLE);
            }
            DynamicListElement selectedValue = dynamicList.getValue();
            return testCaseGenerationStrategies.stream()
                .filter(strategy -> strategy.supports(selectedValue.getLabel()))
                .findFirst()
                .map(strategy -> {
                    try {
                        return strategy.generate(caseReference, fromEvent,
                                                 caseSupportHelper.getNonProdResource(selectedValue.getLabel()));
                    } catch (IOException e) {
                        throw new NonProdSupportException(e);
                    }
                })
                .orElseThrow(() -> new RuntimeException(TEST_CASE_CREATION_NOT_SUPPORTED
                                                            + selectedValue.getLabel()));
        } catch (Exception e) {
            throw new NonProdSupportException(FAILED_TO_GENERATE_TEST_CASE, e);
        }
    }

}
