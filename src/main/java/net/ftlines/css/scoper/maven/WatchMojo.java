package net.ftlines.css.scoper.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import net.ftlines.css.scoper.Watcher;
import net.ftlines.css.scoper.WatchingCompiler;

/**
 * Watch for changes in inputPath, then compile scss files to outputPath using includePaths
 */
@Mojo(name = "watch")
public class WatchMojo extends AbstractCssScopeMojo {

	
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		Path outputRootPath = this.outputPath.toPath();
		Path inputRootPath = this.inputPath.toPath();
		
		WatchingCompiler compiler = new WatchingCompiler(inputRootPath, outputRootPath,
			this::getScssImportRoots , singleFileOutputPath.toPath()) {
			@Override
			protected boolean isDebugMode() {
				return true;
			}
		};
		
		try {
			new Watcher(inputRootPath, 
				Watcher.isFileEndsWithFunction(".html", ".css", ".js"),
				Watcher.allOf(Watcher.isFileEndsWithFunction(".scss"), Watcher.isFileNameStartsWithFunction("_")),
				compiler::setPhase, 
				compiler::process
			).start();
		} catch (Exception e) {
			throw new MojoFailureException(e.getMessage(), e);
		}
	}
	
	private Collection<File> getScssImportRoots() {
		return Arrays.asList(scssImportRoot);
	}
	

}
