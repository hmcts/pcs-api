package uk.gov.hmcts.reform.pcs.ccd.page;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A CCD label is a case field, so its ID is unique across the whole case type - the text lives once
 * in CaseField.json regardless of how many pages place it. Two pages declaring the same ID with
 * different content silently collapse to one, and the losing page renders the other's text and
 * placeholders. Nothing in CCD or the SDK warns about it, so it is asserted here instead.
 */
class LabelIdUniquenessTest {

    private static final Pattern LABEL_WITH_BODY =
        Pattern.compile("\\.label\\(\\s*\"([^\"]+)\"\\s*,\\s*\"{3}(.*?)\"{3}", Pattern.DOTALL);

    @Test
    void shouldNotDeclareTheSameLabelIdWithDifferentContent() throws IOException {
        Map<String, Set<String>> bodiesById = new LinkedHashMap<>();

        try (Stream<Path> sources = Files.walk(Paths.get("src/main/java"))) {
            sources.filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> collectLabels(path, bodiesById));
        }

        Map<String, Set<String>> conflicting = new LinkedHashMap<>();
        bodiesById.forEach((id, bodies) -> {
            if (bodies.size() > 1) {
                conflicting.put(id, bodies);
            }
        });

        assertThat(conflicting.keySet())
            .as("label IDs declared more than once with different content - "
                    + "one definition silently wins and the other page renders the wrong text")
            .isEmpty();
    }

    private void collectLabels(Path path, Map<String, Set<String>> bodiesById) {
        String source;
        try {
            source = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + path, e);
        }

        Matcher matcher = LABEL_WITH_BODY.matcher(source);
        while (matcher.find()) {
            String body = matcher.group(2).replaceAll("\\s+", " ").trim();
            bodiesById.computeIfAbsent(matcher.group(1), key -> new LinkedHashSet<>()).add(body);
        }
    }
}
