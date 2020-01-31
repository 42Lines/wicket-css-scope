package net.ftlines.css.scoper.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import io.bit3.jsass.importer.Importer;
import net.ftlines.css.scoper.AbstractScssFragmentContributor.FilePathScssImportResolver;
import net.ftlines.css.scoper.Watcher;
import net.ftlines.css.scoper.wicket.WicketSourceFileModifier;

/**
 * Watch for changes in inputPath, then compile scss files to outputPath using includePaths
 */
@Mojo(name = "watch")
public class WatchMojo extends AbstractCssScopeMojo {

	public void execute() throws MojoExecutionException, MojoFailureException {
		Path outputRootPath = this.outputPath.toPath();
		Path inputRootPath = this.inputPath.toPath();
		try {
			new Watcher(inputRootPath, Watcher.isFileWatchableFunction(".html", ".css", ".js"), (file) -> {
				new WicketSourceFileModifier(file, inputRootPath, outputRootPath) {
					
					@Override
					protected java.util.Collection<io.bit3.jsass.importer.Importer> getAllScssImporters() {
						Collection<Importer> list = super.getAllScssImporters();
						if(scssImportRoot != null) {
							for(File root: scssImportRoot) {
								list.add(new FilePathScssImportResolver(root.toPath()));
							}
						}
						return list;
					}
					
				}.setDebugMode(true).process();
			}).start();
		} catch (Exception e) {
			throw new MojoFailureException(e.getMessage(), e);
		}
	}

}
