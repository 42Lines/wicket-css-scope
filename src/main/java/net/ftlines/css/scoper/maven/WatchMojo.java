package net.ftlines.css.scoper.maven;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import net.ftlines.css.scoper.Watcher;

/**
 * Watch for changes in inputPath, then compile scss files to outputPath using includePaths
 */
@Mojo(name = "watch")
public class WatchMojo extends AbstractCssScopeMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		Path outputRootPath = this.outputPath.toPath();
		Path inputRootPath = this.inputPath.toPath();
		try {
			new Watcher(inputRootPath, outputRootPath).start();
		} catch (Exception e) {
			throw new MojoFailureException(e.getMessage(), e);
		}
	}

}
