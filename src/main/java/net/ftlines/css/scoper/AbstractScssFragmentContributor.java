package net.ftlines.css.scoper;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.importer.Import;
import io.bit3.jsass.importer.Importer;

public abstract class AbstractScssFragmentContributor implements CssSyleFragmentContributor {

	public abstract Optional<String> getScss();
	
	private static final String OBF_PREFIX = "__________________________";

	@Override
	public Optional<String> getCss() {

		Optional<String> scss = getScss();
		if (scss.isEmpty())
			return Optional.empty();

		Options options = new Options();

		options.setSourceMapContents(false);
		options.setSourceMapEmbed(false);
		options.setOmitSourceMapUrl(true);

		configureOptions(options);

		try {
			
			String scssRaw = scss.get();
			scssRaw = scssRaw.replace("@external", OBF_PREFIX + "external");
			scssRaw = scssRaw.replace("@container", OBF_PREFIX + "container");
			
			String resultCss = new Compiler().compileString(scssRaw, options).getCss();
			resultCss = resultCss.replace(OBF_PREFIX + "external", "@external");
			resultCss = resultCss.replace(OBF_PREFIX + "container", "@container");

			return Optional.of(resultCss);
		} catch (CompilationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void configureOptions(Options options) {

	}
	
	public static class FilePathScssImportResolver implements Importer {

		private Path root;

		public FilePathScssImportResolver(Path root) {
			this.root = root;
		}
		
		@Override
		public Collection<Import> apply(String url, Import previous) {
			if(Files.exists(root.resolve(url))) {
				try {
					URI uri = root.resolve(url).toUri();
					return Collections.singleton(new Import(uri, uri, Files.readAllLines(root.resolve(url)).stream().collect(Collectors.joining("\n"))));
				} catch(Exception e) {
					throw new RuntimeException(e);
				}
			}
			
			return Collections.emptyList();
		}
		
	}


}
