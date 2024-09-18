package net.ftlines.css.scoper;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import net.ftlines.css.scoper.scss.ScssCompilerInterface;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssCompilationException;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssImport;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssImporter;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssOptions;

public abstract class AbstractScssFragmentContributor implements CssSyleFragmentContributor {

	public abstract Optional<String> getScss();
	
	private static final String OBF_PREFIX = "__________________________";

	@Override
	public Optional<String> getCss() {

		Optional<String> scss = getScss();
		if (scss.isEmpty())
			return Optional.empty();

		ScssOptions options = new ScssOptions();

		options.setSourceMapContents(false);
		options.setSourceMapEmbed(false);
		options.setOmitSourceMapUrl(true);

		configureOptions(options);

		try {
			
			String scssRaw = scss.get();
			scssRaw = scssRaw.replace("@external", OBF_PREFIX + "external");
			scssRaw = scssRaw.replace("@container", OBF_PREFIX + "container");
			
			String resultCss = ScssCompilerInterface.create().compileString(scssRaw, options).getCss();
			resultCss = resultCss.replace(OBF_PREFIX + "external", "@external");
			resultCss = resultCss.replace(OBF_PREFIX + "container", "@container");

			return Optional.of(resultCss);
		} catch (ScssCompilationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void configureOptions(ScssOptions options) {

	}
	
	public static class FilePathScssImportResolver implements ScssImporter {

		private Path root;

		public FilePathScssImportResolver(Path root) {
			this.root = root;
		}
		
		@Override
		public Collection<ScssImport> apply(String url, ScssImport previous) {
			Path resolved = root.resolve(url);
			if(Files.exists(resolved)) {
				try {
					URI uri = resolved.toUri();
					String content = Files.readAllLines(resolved).stream().collect(Collectors.joining("\n"));
					return Collections.singleton(new ScssImport(uri, uri, content));
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			}
			
			return Collections.emptyList();
		}
		
	}


}
