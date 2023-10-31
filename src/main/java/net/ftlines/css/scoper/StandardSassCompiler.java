package net.ftlines.css.scoper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.importer.Importer;

public abstract class StandardSassCompiler {

	private Path filePath;
	private Path inputRootPath;
	private Path outputRootPath;

	public StandardSassCompiler(Path filePath, Path inputRootPath, Path outputRootPath) {
		this.filePath = filePath;
		this.inputRootPath = inputRootPath;
		this.outputRootPath = outputRootPath;
	}

	public static boolean isStandardSassFile(Path path) {
		return path.toString().toLowerCase().endsWith(".scss");
	}
	
	public static boolean isStandardSassInclude(Path path) {
		return path.getFileName().toString().toLowerCase().startsWith("_");
	}

	protected Collection<Importer> getAllScssImporters() {
		return new ArrayList<Importer>();
	}
	
	public void process() {
		if(!isStandardSassInclude(filePath)) {
			
			Path rel;
			try {
			  rel = inputRootPath.relativize(filePath);
			} catch (Exception e) {
				rel = filePath;
			}

			Path in = inputRootPath.resolve(rel);
			Path out = changeExtension(outputRootPath.resolve(rel), ".scss", ".css");

			logString("Compiling " + out.toAbsolutePath());
			Options options = new Options();

			options.setSourceMapContents(false);
			options.setSourceMapEmbed(false);
			options.setOmitSourceMapUrl(true);
			options.getImporters().addAll(getAllScssImporters());
	
			try {
				Output z = new Compiler().compileFile(in.toUri(), out.toUri(), options);
				Files.write(out,z.getCss().getBytes(),
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			} catch (CompilationException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
		}
	}

	private static Path changeExtension(Path originalPath, String oldExtension, String newExtension) {
        Path path = originalPath;
        String filename = path.getFileName().toString();

        if (filename.endsWith(oldExtension)) {
            filename = filename.substring(0, filename.lastIndexOf(oldExtension)) + newExtension;
            return path.resolveSibling(filename);
        }
        return originalPath;  // returns the original path if the old extension doesn't match
    }

	protected void logString(String message) {
		System.out.println(message);
	}
	
}
