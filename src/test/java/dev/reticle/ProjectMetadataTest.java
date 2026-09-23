package dev.reticle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ProjectMetadataTest {
    private static JsonObject resourceJson(String name) throws IOException {
        InputStream in = ProjectMetadataTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(in, name + " must be on the classpath");
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static boolean has(String name) {
        return ProjectMetadataTest.class.getClassLoader().getResource(name) != null;
    }

    @Test
    void modJson() throws IOException {
        assumeTrue(has("fabric.mod.json"));
        JsonObject mod = resourceJson("fabric.mod.json");
        assertEquals("reticle", mod.get("id").getAsString());
        assertEquals("Reticle", mod.get("name").getAsString());
        assertEquals("client", mod.get("environment").getAsString());
        assertFalse(mod.get("version").getAsString().contains("$"), "version placeholder must be expanded");

        JsonObject entrypoints = mod.getAsJsonObject("entrypoints");
        assertEquals("dev.reticle.fabric.ReticleFabric", entrypoints.getAsJsonArray("client").get(0).getAsString());

        JsonObject depends = mod.getAsJsonObject("depends");
        assertFalse(depends.has("modmenu"), "Mod Menu must stay optional");
        assertTrue(mod.getAsJsonObject("suggests").has("modmenu"));
    }

    @Test
    void entrypointsExist() throws IOException {
        assumeTrue(has("fabric.mod.json"));
        JsonObject mod = resourceJson("fabric.mod.json");
        JsonObject entrypoints = mod.getAsJsonObject("entrypoints");
        for (String key : entrypoints.keySet()) {
            for (JsonElement entry : entrypoints.getAsJsonArray(key)) {
                assertClassExists(entry.getAsString());
            }
        }
    }

    private static void assertClassExists(String className) {
        String path = className.replace('.', '/') + ".class";
        assertNotNull(ProjectMetadataTest.class.getClassLoader().getResource(path), path + " must be compiled");
    }

    @Test
    void noOtherProjectReferences() throws IOException {
        String[] forbidden = {"rot" + "client", "rot-" + "client", "rot " + "client", "fi." + "rot", "loupe", "smoothzoom", "limn"};
        try (Stream<Path> files = Stream.of(Path.of("src/main"), Path.of("build.gradle"), Path.of("gradle.properties"),
                        Path.of("settings.gradle"), Path.of("README.md"), Path.of("LICENSE"), Path.of("docs"))
                .filter(Files::exists)
                .flatMap(ProjectMetadataTest::walk)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> !file.toString().endsWith(".png") && !file.toString().endsWith(".jar")
                            && !file.toString().endsWith(".ogg"))
                    .forEach(file -> {
                String text;
                try {
                    text = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                } catch (IOException e) {
                    throw new IllegalStateException(file.toString(), e);
                }
                for (String token : forbidden) {
                    assertFalse(text.contains(token), file + " mentions \"" + token + "\"");
                }
            });
        }
    }

    private static Stream<Path> walk(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException e) {
            throw new IllegalStateException(root.toString(), e);
        }
    }
}
