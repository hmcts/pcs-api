package uk.gov.hmcts.reform.pcs.data.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class FlywayMigrationVersionTest {

    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");
    private static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("^V(\\d+)__.+\\.sql$");

    @Test
    void shouldEarlyCatchDuplicateMigrationVersions() throws IOException {
        Map<Integer, List<String>> migrationFilesByVersion;
        try (Stream<Path> migrationFiles = Files.list(MIGRATION_DIRECTORY)) {
            migrationFilesByVersion = migrationFiles.map(Path::getFileName)
                .map(Path::toString)
                .map(MIGRATION_FILE_PATTERN::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.groupingBy(
                    matcher -> Integer.parseInt(matcher.group(1)),
                    TreeMap::new,
                    Collectors.mapping(Matcher::group, Collectors.toList())
                ));
        }

        Map<Integer, List<String>> duplicateVersions = migrationFilesByVersion.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (first, second) -> first, TreeMap::new
            ));
        assertThat(duplicateVersions).as("Flyway migration versions must be unique").isEmpty();
    }
}
