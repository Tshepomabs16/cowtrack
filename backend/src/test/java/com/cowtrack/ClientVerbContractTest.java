package com.cowtrack;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the verbs the web client actually sends and checks the backend accepts
 * each one.
 *
 * <p>Two endpoints had drifted: the client resolved an alert and completed a
 * reminder with PUT while both controllers accepted only POST. Each returned
 * 405 and neither had ever worked from the UI. Path coverage was already
 * asserted by {@code ApiContractTest}, which is why the mismatch survived - the
 * path existed, so it looked reconciled.
 *
 * <p>Parsing the client source is deliberate. Restating the call list here by
 * hand would make this a test of the restatement rather than of the client, and
 * it would drift the moment someone edits {@code api.js} without touching this
 * file.
 */
@SpringBootTest
@ActiveProfiles("test")
class ClientVerbContractTest {

    /** e.g. {@code api.put(`/alerts/${id}/resolve`)} */
    private static final Pattern CLIENT_CALL = Pattern.compile(
            "api\\.(get|post|put|delete)\\(\\s*[`'\"]([^`'\"]+)[`'\"]");

    /** Template placeholders and path variables both collapse to a wildcard. */
    private static final Pattern TEMPLATE_ARG = Pattern.compile("\\$\\{[^}]+}");
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^}]+}");

    // Actuator contributes a second RequestMappingHandlerMapping, so the MVC one
    // has to be selected by name.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyVerbTheClientSendsIsAcceptedByTheBackend() throws IOException {
        Map<String, Set<String>> accepted = mappedVerbsByPath();
        List<String> mismatches = new ArrayList<>();

        Matcher matcher = CLIENT_CALL.matcher(readClientSource());
        while (matcher.find()) {
            String verb = matcher.group(1).toUpperCase(Locale.ROOT);
            String path = normalise("/api" + matcher.group(2));

            Set<String> verbs = accepted.get(path);
            if (verbs == null) {
                // Unmapped paths are ApiContractTest's concern, not this one.
                continue;
            }
            if (!verbs.contains(verb)) {
                mismatches.add("%s %s -> backend accepts %s"
                        .formatted(verb, path, String.join(",", new TreeSet<>(verbs))));
            }
        }

        assertThat(mismatches)
                .withFailMessage("The client sends verbs the backend rejects, which surfaces as 405:%n  %s",
                        String.join("%n  ".formatted(), mismatches))
                .isEmpty();
    }

    private Map<String, Set<String>> mappedVerbsByPath() {
        Map<String, Set<String>> byPath = new HashMap<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry
                : handlerMapping.getHandlerMethods().entrySet()) {

            RequestMappingInfo info = entry.getKey();
            Set<String> patterns = info.getPathPatternsCondition() == null
                    ? Set.of()
                    : info.getPathPatternsCondition().getPatternValues();

            Set<String> verbs = info.getMethodsCondition().getMethods().stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());

            for (String pattern : patterns) {
                byPath.computeIfAbsent(normalise(pattern), key -> new HashSet<>()).addAll(verbs);
            }
        }
        return byPath;
    }

    /** Collapses both {@code ${id}} and {@code {cowId}} so the two forms compare. */
    private String normalise(String path) {
        String collapsed = TEMPLATE_ARG.matcher(path).replaceAll("{}");
        collapsed = PATH_VARIABLE.matcher(collapsed).replaceAll("{}");
        int query = collapsed.indexOf('?');
        return query >= 0 ? collapsed.substring(0, query) : collapsed;
    }

    private String readClientSource() throws IOException {
        Path source = Path.of("../frontend/src/services/api.js");
        assertThat(Files.exists(source))
                .withFailMessage("Expected the client API module at %s", source.toAbsolutePath())
                .isTrue();
        return Files.readString(source);
    }
}
