package uk.gov.hmcts.reform.pcs.config;

import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.integrations.FileData;
import com.launchdarkly.sdk.server.subsystems.ComponentConfigurer;
import com.launchdarkly.sdk.server.subsystems.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
@Slf4j
public class LaunchDarklyConfiguration {

    @Bean(destroyMethod = "close")
    public LDClient ldClient(@Value("${launchdarkly.sdk-key:}") String sdkKey,
                             @Value("${launchdarkly.offline-mode:false}") Boolean offlineMode,
                             @Value("${launchdarkly.files:}") String[] flagFiles) {
        LDConfig.Builder builder = new LDConfig.Builder();

        if (BooleanUtils.isTrue(offlineMode)) {
            // Use local flag files as the flag datasource if specified, (instead of the remote service),
            // otherwise set the whole LDClient to offline mode.
            getExistingFiles(flagFiles).ifPresentOrElse(
                localFlagFiles -> builder.dataSource(this.getDataSource(localFlagFiles)),
                () -> builder.offline(true));
        }

        LDClient client = new LDClient(sdkKey, builder.build());
        if (!client.isInitialized()) {
            log.warn("LaunchDarkly not initialised at startup - serving code defaults until connected");
        }
        return client;
    }

    private ComponentConfigurer<DataSource> getDataSource(Path[] flagFilePaths) {
        return FileData.dataSource()
            .filePaths(flagFilePaths)
            .autoUpdate(true)
            .duplicateKeysHandling(FileData.DuplicateKeysHandling.IGNORE);
    }

    private Optional<Path[]> getExistingFiles(String[] files) {
        if (files == null || files.length < 1) {
            return Optional.empty();
        }
        Path[] existing = Stream.of(files)
            .map(this::getPathIfExists)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toArray(Path[]::new);
        return existing.length > 0 ? Optional.of(existing) : Optional.empty();
    }

    private Optional<Path> getPathIfExists(String file) {
        if (StringUtils.isBlank(file)) {
            return Optional.empty();
        }
        Path flagFile = file.startsWith("/") ? Paths.get(file) : Paths.get("").resolve(file);
        if (Files.exists(flagFile)) {
            return Optional.of(flagFile);
        }
        log.warn("Could not find LaunchDarkly flag file defined by {}", file);
        return Optional.empty();
    }
}
