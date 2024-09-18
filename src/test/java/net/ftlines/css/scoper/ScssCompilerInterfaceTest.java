package net.ftlines.css.scoper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.ftlines.css.scoper.scss.ScssCompilerInterface;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssImport;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssOptions;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssOutput;

class ScssCompilerInterfaceTest {

    private ScssCompilerInterface compiler;

    @BeforeEach
    void setUp() {
        // Initialize the compiler instance
        compiler = ScssCompilerInterface.create();
    }

    @AfterEach
    void tearDown() {
        // Clean up if necessary
    }
    
    /**
     * Custom assertion method that compares two strings while ignoring any whitespace differences.
     */
    private void assertEqualsIgnoringWhitespace(String expected, String actual, String message) {
        String expectedNormalized = expected.replaceAll("\\s+", "");
        String actualNormalized = actual.replaceAll("\\s+", "");
        assertEquals(expectedNormalized, actualNormalized, message);
    }


    @Test
    void testCompileSimpleString() throws ScssCompilerInterface.ScssCompilationException {
        String scss = "$color: red; .test { color: $color; }";
        ScssOptions options = new ScssOptions();
        options.setOmitSourceMapUrl(true);

        ScssOutput output = compiler.compileString(scss, options);
        String expectedCss = ".test {\n  color: red;\n}";

        assertEqualsIgnoringWhitespace(expectedCss.strip(), output.getCss().strip(), "The compiled CSS should match the expected output.");
    }

    @Test
    void testCompileStringWithOptions() throws ScssCompilerInterface.ScssCompilationException {
        String scss = "@mixin border-radius($radius) { -webkit-border-radius: $radius; border-radius: $radius; } .box { @include border-radius(10px); }";
        ScssOptions options = new ScssOptions();
        // Set options if needed
        options.setSourceMapContents(true);
        options.setSourceMapEmbed(true);
        options.setOmitSourceMapUrl(true);

        ScssOutput output = compiler.compileString(scss, options);
        String expectedCss = ".box {\n  -webkit-border-radius: 10px;\n  border-radius: 10px;\n}";

        assertTrue(output.getCss().contains("-webkit-border-radius"), "The compiled CSS should include the mixin output.");
        assertEqualsIgnoringWhitespace(expectedCss.strip(), output.getCss().strip(), "The output should match");
    }

    @Test
    void testCompileStringWithImporter() throws ScssCompilerInterface.ScssCompilationException {
        String scss = "@import 'custom';";
        ScssOptions options = new ScssOptions();

        options.getImporters().add((url, previous) -> {
            if ("custom".equals(url)) {
                ScssImport scssImport = new ScssImport(null, null, "$bg-color: blue; .imported { background-color: $bg-color; }");
                return Collections.singletonList(scssImport);
            }
            return null;
        });

        ScssOutput output = compiler.compileString(scss, options);

        assertTrue(output.getCss().contains(".imported"), "The compiled CSS should include content from the custom importer.");
        assertTrue(output.getCss().contains("background-color: blue;"), "The background color should be blue as defined in the imported content.");
    }

    @Test
    void testCompileInvalidString() {
        String invalidScss = "$color: red; .test { color: $col; }"; // $col is undefined
        ScssOptions options = new ScssOptions();

        Exception exception = assertThrows(ScssCompilerInterface.ScssCompilationException.class, () -> {
            compiler.compileString(invalidScss, options);
        });

        String expectedMessagePart = "Undefined variable";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessagePart), "The exception message should indicate an undefined variable.");
    }

    @Test
    void testCompileFile() throws IOException, URISyntaxException, ScssCompilerInterface.ScssCompilationException {
        // Prepare a temporary SCSS file
        Path tempFile = Files.createTempFile("test", ".scss");
        Files.write(tempFile, Arrays.asList("$color: green;", ".file-test { color: $color; }"));

        URI inputFileUri = tempFile.toUri();
        URI outputFileUri = Files.createTempFile("output", ".css").toUri();

        ScssOptions options = new ScssOptions();

        ScssOutput output = compiler.compileFile(inputFileUri, outputFileUri, options);

        assertTrue(output.getCss().contains(".file-test"), "The compiled CSS should include the .file-test class.");
        assertTrue(output.getCss().contains("color: green;"), "The color should be green as defined in the SCSS file.");

        // Clean up temporary files
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(Paths.get(outputFileUri));
    }

    @Test
    void testCompileFileWithImporter() throws IOException, URISyntaxException, ScssCompilerInterface.ScssCompilationException {
        // Prepare a temporary SCSS file that imports 'variables'
        Path tempFile = Files.createTempFile("testImporter", ".scss");
        Files.write(tempFile, Arrays.asList("@import 'variables';", ".importer-test { color: $imported-color; }"));

        URI inputFileUri = tempFile.toUri();
        URI outputFileUri = Files.createTempFile("outputImporter", ".css").toUri();

        ScssOptions options = new ScssOptions();

        options.getImporters().add((url, previous) -> {
            if ("variables".equals(url)) {
                ScssImport scssImport = new ScssImport(null, null, "$imported-color: purple;");
                return Collections.singletonList(scssImport);
            }
            return null;
        });

        ScssOutput output = compiler.compileFile(inputFileUri, outputFileUri, options);

        assertTrue(output.getCss().contains(".importer-test"), "The compiled CSS should include the .importer-test class.");
        assertTrue(output.getCss().contains("color: purple;"), "The color should be purple as defined by the importer.");

        // Clean up temporary files
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(Paths.get(outputFileUri));
    }

    @Test
    void testCustomImporterErrorHandling() {
        String scss = "@import 'nonexistent';";
        ScssOptions options = new ScssOptions();

        options.getImporters().add((url, previous) -> {
            // Simulate an error in the importer
            throw new RuntimeException("Importer error");
        });

        Exception exception = assertThrows(ScssCompilerInterface.ScssCompilationException.class, () -> {
            compiler.compileString(scss, options);
        });

        String expectedMessagePart = "Importer error";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessagePart), "The exception message should include the importer error.");
    }

    @Test
    void testCompilerWithNullOptions() throws ScssCompilerInterface.ScssCompilationException {
        String scss = ".null-options { margin: 0; }";

        ScssOutput output = compiler.compileString(scss, null);

        assertTrue(output.getCss().contains(".null-options"), "The compiled CSS should include the .null-options class.");
    }

    @Test
    void testCompileEmptyString() throws ScssCompilerInterface.ScssCompilationException {
        String scss = "";
        ScssOptions options = new ScssOptions();

        ScssOutput output = compiler.compileString(scss, options);

        assertTrue(output.getCss().isEmpty(), "The compiled CSS should be empty for an empty SCSS input.");
    }

    @Test
    void testCompileNullString() {
        ScssOptions options = new ScssOptions();

        assertThrows(NullPointerException.class, () -> {
            compiler.compileString(null, options);
        }, "Compiling a null SCSS string should throw a NullPointerException.");
    }

}