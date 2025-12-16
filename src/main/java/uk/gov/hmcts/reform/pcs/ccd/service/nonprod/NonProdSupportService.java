
package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
@Profile({"local", "dev", "preview"})
public class NonProdSupportService {

    private final CaseSupportHelper caseSupportHelper;
    private final List<TestCaseGenerationStrategy> testCaseGenerationStrategies;

    public CaseSupportGenerationResponse caseGenerator(long caseReference, PCSCase fromEvent) {
        try {
            DynamicList dynamicList = fromEvent.getNonProdSupportFileList();
            if (dynamicList == null || dynamicList.getValue() == null) {
                throw new IllegalArgumentException("No non-prod case selected");
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
                        throw new SupportException(e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("No strategy found for label: " + selectedValue.getLabel()));
        } catch (Exception e) {
            throw new SupportException("Failed to generate test PCSCase", e);
        }
    }

}
