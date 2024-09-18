package net.ftlines.css.scoper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;

import net.ftlines.css.scoper.scss.ScssCompilerInterface;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssCompilationException;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssImporter;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssOptions;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssOutput;

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

	protected Collection<ScssImporter> getAllScssImporters() {
		return new ArrayList<ScssImporter>();
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
			ScssOptions options = new ScssOptions();

			options.setSourceMapContents(false);
			options.setSourceMapEmbed(false);
			options.setOmitSourceMapUrl(true);
			options.getImporters().addAll(getAllScssImporters());
	
			try {
				ScssOutput z = ScssCompilerInterface.create().compileFile(in.toUri(), out.toUri(), options);
				out.getParent().toFile().mkdirs();
				Files.write(out,z.getCss().getBytes(),
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			} catch (ScssCompilationException e) {
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
