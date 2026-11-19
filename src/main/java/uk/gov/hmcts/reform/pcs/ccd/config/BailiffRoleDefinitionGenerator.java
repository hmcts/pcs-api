package uk.gov.hmcts.reform.pcs.ccd.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.generator.ConfigGenerator;
import uk.gov.hmcts.ccd.sdk.generator.JSONConfigGenerator;
import uk.gov.hmcts.ccd.sdk.generator.JsonUtils;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The SDK emits every access-profile enum constant, even when no AM role maps to it.
 * Remove only the three bailiff case-type permissions when disabled. AM mappings are gated separately.
 * Changing these permissions requires regenerating and importing the CCD definition.
 */
@Component
@Primary
public class BailiffRoleDefinitionGenerator<T, S, R extends HasRole> extends JSONConfigGenerator<T, S, R> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> BAILIFF_PROFILES = Set.of(
        AccessProfile.BAILIFF.getRole(), AccessProfile.BAILIFF_ADMIN.getRole(), AccessProfile.BAILIFF_MANAGER.getRole()
    );

    private final FeatureToggleService featureToggleService;

    public BailiffRoleDefinitionGenerator(Collection<ConfigGenerator<T, S, R>> writers,
                                         FeatureToggleService featureToggleService) {
        super(writers);
        this.featureToggleService = featureToggleService;
    }

    @Override
    public void writeConfig(File outputFolder, ResolvedCCDConfig<T, S, R> config) {
        super.writeConfig(outputFolder, config);
        if (featureToggleService.isEnabled(FeatureFlag.BAILIFF_MANAGEMENT)) {
            return;
        }
        Path permissionsFile = outputFolder.toPath().resolve("AuthorisationCaseType.json");
        try {
            List<Map<String, Object>> permissions = MAPPER.readValue(
                permissionsFile.toFile(), new TypeReference<>() { });
            List<Map<String, Object>> retained = permissions.stream()
                .filter(row -> !BAILIFF_PROFILES.contains(String.valueOf(row.get("UserRole"))))
                .toList();
            if (retained.size() != permissions.size()) {
                Files.writeString(permissionsFile, JsonUtils.serialise(retained, false));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not remove disabled bailiff role configuration", e);
        }
    }
}
