package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
@Profile({"local", "dev", "preview"})
public class CaseSupportHelper {

    public static final String LOCATION_PATTERN = "classpath*:nonprod/";
    public static final String JSON = ".json";

    private final ResourcePatternResolver resourcePatternResolver;

    public DynamicList getNonProdFilesList() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(LOCATION_PATTERN + "*");
            List<DynamicListElement> listItems = Arrays.stream(resources)
                .map(Resource::getFilename)
                .filter(Objects::nonNull)
                .distinct()
                .map(name -> DynamicListElement.builder().code(UUID.nameUUIDFromBytes(name.getBytes()))
                    .label(generateLabelFromFilename(name)).build())
                .toList();
            return DynamicList.builder()
                .listItems(listItems)
                .value(DynamicListElement.builder().label("Please select ...").build())
                .build();
        } catch (IOException e) {
            log.error("Error reading nonprod files", e);
            return DynamicList.builder().build();
        }
    }

    private String generateLabelFromFilename(String filename) {
        return filename.replace("-", " ")
            .replace(JSON, "");
    }

    public Resource getNonProdResource(String label) throws IOException {
        String name = generateNameFromLabel(label);
        return Arrays.stream(resourcePatternResolver.getResources(LOCATION_PATTERN + name + JSON))
            .findFirst().orElseThrow();
    }

    public String generateNameFromLabel(String label) {
        return label.replace(" ", "-");
    }

}
