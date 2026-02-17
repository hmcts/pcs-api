package uk.gov.hmcts.reform.pcs.ccd.testcasesupport;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class TestCaseSupportHelper {

    public static final String LOCATION_PATTERN = "classpath*:test-case-support/";
    public static final String JSON = ".json";

    private final ResourcePatternResolver resourcePatternResolver;

    public DynamicList getFileList() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(LOCATION_PATTERN + "*");
            List<DynamicListElement> listItems = Arrays.stream(resources)
                .filter(Resource::isFile)
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .filter(name -> name.endsWith(".json"))
                .distinct()
                .map(name -> DynamicListElement.builder().code(UUID.nameUUIDFromBytes(name.getBytes()))
                    .label(generateLabelFromFilename(name)).build())
                .toList();
            return DynamicList.builder()
                .listItems(listItems)
                .value(DynamicListElement.builder().label("Please select ...").code(UUID.randomUUID()).build())
                .build();
        } catch (IOException e) {
            log.error("Error reading Test Case Support files", e);
            return DynamicList.builder().build();
        }
    }

    private String generateLabelFromFilename(String filename) {
        return filename.replace("-", " ").replace(JSON, "");
    }

    public Resource getTestResource(String label) throws IOException {
        String name = label.replace(" ", "-");
        Resource[] resources = resourcePatternResolver.getResources(LOCATION_PATTERN + name + JSON);
        if (resources == null || resources.length == 0) {
            throw new IOException("No resource found for label: " + label);
        }
        return resources[0];
    }

}
