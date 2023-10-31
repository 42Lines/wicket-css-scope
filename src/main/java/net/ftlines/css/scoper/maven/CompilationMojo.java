package net.ftlines.css.scoper.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import io.bit3.jsass.importer.Importer;
import net.ftlines.css.scoper.AbstractScssFragmentContributor.FilePathScssImportResolver;
import net.ftlines.css.scoper.StandardSassCompiler;
import net.ftlines.css.scoper.StyleCollectionWriter;
import net.ftlines.css.scoper.wicket.WicketSingleStyleSourceFileModifier;
import net.ftlines.css.scoper.wicket.WicketSourceFileModifier;

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
			if(singleFileOutputPath == null) {
				executePanalizedCompile(outputRootPath, inputRootPath);
			} else {
				StyleCollectionWriter styles = new StyleCollectionWriter();
				executeUnifiedCompile(outputRootPath, inputRootPath, styles);
				styles.writeTo(singleFileOutputPath.toPath());
				
				executeStandAloneCompile(outputRootPath, inputRootPath);
			}

		} catch (Exception e) {
			throw new MojoFailureException(e.getMessage(), e);
		}
	}
	

	private void executeUnifiedCompile(Path outputRootPath, Path inputRootPath, StyleCollectionWriter styles) {
		for (String f : fileSetManager.getIncludedFiles(fileset)) {
			new WicketSingleStyleSourceFileModifier(Path.of(f), inputRootPath, outputRootPath, styles) {			
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
				
			}.process();
		}
	}

	private void executePanalizedCompile(Path outputRootPath, Path inputRootPath) {
		for (String f : fileSetManager.getIncludedFiles(fileset)) {
			executeSingleCompile(Path.of(f), outputRootPath, inputRootPath);
		}
	}
	
	private void executeStandAloneCompile(Path outputRootPath, Path inputRootPath) {
		for (String f : fileSetManager.getIncludedFiles(fileset)) {
			Path p = Path.of(f);
			StandardSassCompiler.isStandardSassFile(p);
			executeSingleCompile(p, outputRootPath, inputRootPath);
		}
	}
	
	private void executeSingleCompile(Path filePath, Path outputRootPath, Path inputRootPath) {
		
		if(StandardSassCompiler.isStandardSassFile(filePath)) {
			new StandardSassCompiler(filePath, inputRootPath, outputRootPath) {
				@Override
				protected java.util.Collection<io.bit3.jsass.importer.Importer> getAllScssImporters() {
					Collection<Importer> list = super.getAllScssImporters();
					list.add(createImporterSet(inputRootPath.resolve(filePath).toAbsolutePath().getParent().toFile(), list));
					return list;
				}
			}.process();
		} else {
			new WicketSourceFileModifier(filePath, inputRootPath, outputRootPath) {

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

			}.process();
		}
	}
	
	private Importer createImporterSet(File root, Collection<Importer> list) {
		return new FilePathScssImportResolver(root.toPath());
	}

}
