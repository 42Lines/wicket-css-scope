package net.ftlines.css.scoper.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

public abstract class AbstractCssScopeMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	public File outputPath;

	@Parameter(defaultValue = "${project.build.sourceDirectory}", required = true)
	public File inputPath;

	@Parameter(required = true, defaultValue = "")
	public FileSet fileset;
	
	@Parameter(required = false)
	public File[] scssImportRoot;

	protected FileSetManager fileSetManager = new FileSetManager();

}
