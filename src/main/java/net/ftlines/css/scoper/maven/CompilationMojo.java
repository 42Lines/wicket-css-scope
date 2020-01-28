package net.ftlines.css.scoper.maven;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import net.ftlines.css.scoper.WicketSourceFileModifier;

/**
 * Compilation of all html files from inputpath to outputpath using includePaths
 */
@Mojo(name = "compile-markup", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class CompilationMojo extends AbstractCssScopeMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Path outputRootPath = this.outputPath.toPath();
		Path inputRootPath = this.inputPath.toPath();

		try {
			for (String f : fileSetManager.getIncludedFiles(fileset)) {
				new WicketSourceFileModifier(Path.of(f), inputRootPath, outputRootPath).process();
			}

		} catch (Exception e) {
			throw new MojoFailureException(e.getMessage(), e);
		}
	}

}
