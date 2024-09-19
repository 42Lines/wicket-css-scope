package net.ftlines.css.scoper.scss;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import com.sass_lang.embedded_protocol.InboundMessage.ImportResponse.ImportSuccess;
import com.sass_lang.embedded_protocol.Syntax;

import de.larsgrefer.sass.embedded.CompileSuccess;
import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.SassCompiler;
import de.larsgrefer.sass.embedded.SassCompilerFactory;
import de.larsgrefer.sass.embedded.importer.CustomImporter;

public class DartScssCompiler implements ScssCompilerInterface {

    @Override
    public ScssOutput compileFile(URI inputFileUri, URI outputFileUri, ScssOptions options) throws ScssCompilationException {
    	
    	if(options == null) 
    		options = new ScssOptions();
    	
        try (SassCompiler sassCompiler = SassCompilerFactory.bundled()) {
            // Configure options
            setOptions(sassCompiler, options);

            // Convert input URI to File
            File inputFile = new File(inputFileUri);
            // Compile the file
            CompileSuccess compileSuccess = sassCompiler.compileFile(inputFile);

            // Get the compiled CSS
            String css = compileSuccess.getCss();

            return new ScssOutput(css);
        } catch (SassCompilationFailedException e) {
            throw new ScssCompilationException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ScssCompilationException("IO error during compilation", e);
        } catch (Exception e) {
            throw new ScssCompilationException("Unexpected error during compilation", e);
        }
    }

    @Override
    public ScssOutput compileString(String scssRaw, ScssOptions options) throws ScssCompilationException {
    	
    	if(scssRaw == null)
    		throw new NullPointerException();
    	
    	if(scssRaw.isEmpty())
			return new ScssOutput("");
    	
    	if(options == null) 
    		options = new ScssOptions();

        try (SassCompiler sassCompiler = SassCompilerFactory.bundled()) {
            // Configure options
            setOptions(sassCompiler, options);

            // Compile the string
            CompileSuccess compileSuccess = sassCompiler.compileScssString(scssRaw);

            // Get the compiled CSS
            String css = compileSuccess.getCss();

            return new ScssOutput(css);
        } catch (SassCompilationFailedException e) {
            throw new ScssCompilationException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ScssCompilationException("IO error during compilation", e);
        } catch (Exception e) {
            throw new ScssCompilationException("Unexpected error during compilation", e);
        }
    }

    private void setOptions(SassCompiler sassCompiler, ScssOptions options) {
        // Map source map options
        sassCompiler.setGenerateSourceMaps(!options.isOmitSourceMapUrl());
        sassCompiler.setSourceMapIncludeSources(options.isSourceMapContents());

     // Map importers
        options.getImporters().stream().map(this::asCustomImporter).forEach(sassCompiler::registerImporter);
        
        // Additional options can be set here if needed
    }

    private CustomImporter asCustomImporter(ScssImporter scssImporter) {
        return new CustomImporter() {
            @Override
            public String canonicalize(String url, boolean fromImport) throws Exception {
            	
                // We assume the 'url' provided corresponds to the URL the user is importing
                Collection<ScssImport> scssImports = scssImporter.apply(url, null);
                if (scssImports == null || scssImports.isEmpty()) {
                    return null; // If no imports are found, return null
                }

                // Return the absolute URI for the import if available
                ScssImport scssImport = scssImports.iterator().next();
                if (scssImport.getAbsoluteUri() != null) {
                    return scssImport.getAbsoluteUri().toString(); // This URI will be used by the compiler
                } else {
                    // If no absolute URI is available, fallback to the import URL (like inline content)
                    return "memory:" + url; // Marking it as in-memory content
                }
            }

            @Override
            public ImportSuccess handleImport(String url) throws Exception {
            	
            	if(url.startsWith("memory:")) {
            		url = url.replace("memory:", "");
            	}
            	
            	if(url.startsWith("file:")) {
            		url = url.replace("file:", "");
            	}
            	
                // The method to actually resolve the import's content
                Collection<ScssImport> scssImports = scssImporter.apply(url, null);
                if (scssImports == null || scssImports.isEmpty()) {
                    return null; // If the import couldn't be found, return null
                }

                // We assume we're handling a single import at a time
                ScssImport scssImport = scssImports.iterator().next();

                ImportSuccess.Builder result = ImportSuccess.newBuilder();
                
                // If the import contains SCSS content, add it to the result
                if (scssImport.getContents() != null) {
                    result.setContents(scssImport.getContents()); // Set the in-memory content to be compiled
                    result.setSyntax(Syntax.SCSS); // Assuming SCSS syntax
                }

                // Return the built ImportSuccess object
                return result.build();
            }
        };
    }


 
}
