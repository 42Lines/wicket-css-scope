package net.ftlines.css.scoper.scss;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ScssCompilerInterface {

	
	public static class ScssImport {
		/**
		   * The import uri for this import.
		   */
		  private final URI importUri;

		  /**
		   * The absolute uri for this import.
		   */
		  private final URI absoluteUri;

		  /**
		   * The in-memory sass code, may be <em>null</em> when importing a file.
		   */
		  private final String contents;

		  /**
		   * Create a string import.
		   *
		   * @param importUri      The file uri relative to the base uri.
		   * @param absoluteUri     The base uri for this import.
		   * @param contents The in-memory sass code.
		   */
		  public ScssImport(URI importUri, URI absoluteUri, String contents) {
			    this.importUri = importUri;
			    this.absoluteUri = absoluteUri;
			    this.contents = contents;			  
		  }


		  /**
		   * Get the import uri.
		   *
		   * @return The file uri relative to the base uri.
		   */
		  public URI getImportUri() {
		    return importUri;
		  }

		  /**
		   * Get the absolute uri.
		   *
		   * @return The base uri for this import.
		   */
		  public URI getAbsoluteUri() {
		    return absoluteUri;
		  }

		  /**
		   * Return the in-memory sass code.
		   *
		   * @return The in-memory sass code or <em>null</em> when importing a file.
		   */
		  public String getContents() {
		    return contents;
		  }

	}
	
	public interface ScssImporter {
		Collection<ScssImport> apply(String url, ScssImport previous);
		//TODO io.bit3.jsass.importer.Importer
	}
	
	public static class ScssOptions {

		private List<ScssImporter> importers = new ArrayList<ScssCompilerInterface.ScssImporter>();
		
		private boolean sourceMapContents = false;
		private boolean sourceMapEmbed = false;
		private boolean omitSourceMapUrl = false;
		
		public List<ScssImporter> getImporters() {
			return importers;
		}

		public boolean isSourceMapContents() {
			return sourceMapContents;
		}

		public void setSourceMapContents(boolean sourceMapContents) {
			this.sourceMapContents = sourceMapContents;
		}

		public boolean isSourceMapEmbed() {
			return sourceMapEmbed;
		}

		public void setSourceMapEmbed(boolean sourceMapEmbed) {
			this.sourceMapEmbed = sourceMapEmbed;
		}

		public boolean isOmitSourceMapUrl() {
			return omitSourceMapUrl;
		}

		public void setOmitSourceMapUrl(boolean omitSourceMapUrl) {
			this.omitSourceMapUrl = omitSourceMapUrl;
		}

	}
	
	public static class ScssOutput {

		private final String css;

		ScssOutput(String css) {
			this.css = css;
		}
		
		public String getCss() {
			return css;
		}
	}
	
	public static class ScssCompilationException extends Exception {

		public ScssCompilationException(String message, Exception ce) {
			super(message, ce);
		}

		private static final long serialVersionUID = 8364288313509758175L;
		
	}
	
	ScssOutput compileFile(URI uri, URI uri2, ScssOptions options) throws ScssCompilationException;

	ScssOutput compileString(String scssRaw, ScssOptions options) throws ScssCompilationException;
	
	public static ScssCompilerInterface create() {
		return new JScssCompiler();
	}
}
