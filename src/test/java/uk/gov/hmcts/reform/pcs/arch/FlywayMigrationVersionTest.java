package uk.gov.hmcts.reform.pcs.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class FlywayMigrationVersionTest {

    private static final Pattern VERSIONED_MIGRATION_PATTERN = Pattern.compile("^V(\\d+)__.*");

    private Path migrationDir = Path.of("src", "main", "resources", "db", "migration");

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void shouldNotHaveDuplicateFlywayMigrationVersionNumbers() throws IOException {
        if (!Files.exists(migrationDir) || !Files.isDirectory(migrationDir)) {
            return;
        }

        Map<String, List<String>> filesByVersion;
        try (Stream<Path> stream = Files.list(migrationDir)) {
            filesByVersion = stream
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(name -> VERSIONED_MIGRATION_PATTERN.matcher(name).matches())
                .collect(Collectors.groupingBy(
                    name -> {
                        var matcher = VERSIONED_MIGRATION_PATTERN.matcher(name);
                        matcher.matches(); // safe because already filtered
                        return matcher.group(1);
                    },
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
        }

        Map<String, List<String>> duplicates = filesByVersion.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                      (a, b) -> a, LinkedHashMap::new));

        if (!duplicates.isEmpty()) {
            String details = duplicates.entrySet().stream()
                .map(entry -> "V" + entry.getKey() + "__ -> " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
            fail("Duplicate Flyway migration version numbers found:" + System.lineSeparator() + details);
        }
    }
}
