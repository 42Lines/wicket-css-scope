package net.ftlines.css.scoper.scss;

import java.net.URI;
import java.util.Collection;
import java.util.stream.Collectors;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.importer.Import;
import io.bit3.jsass.importer.Importer;

public class JScssCompiler implements ScssCompilerInterface {

	@Override
	public ScssOutput compileFile(URI uri, URI uri2, ScssOptions options) throws ScssCompilationException {
		try {
			return asOutput(new io.bit3.jsass.Compiler().compileFile(uri, uri2, asOptions(options)));
		} catch(CompilationException ce) {
			throw new ScssCompilationException(ce.getMessage(), ce);
		}
	}

	@Override
	public ScssOutput compileString(String scssRaw, ScssOptions options) throws ScssCompilationException {
		try {
			return asOutput(new io.bit3.jsass.Compiler().compileString(scssRaw, asOptions(options)));
		} catch(CompilationException ce) {
			throw new ScssCompilationException(ce.getMessage(), ce);
		}
	}
	
	private static Importer asImporter(ScssImporter input) {
		return new Importer() {

			@Override
			public Collection<Import> apply(String url, Import previous) {
				Collection<ScssImport> i = input.apply(url, asImport(previous));
				if(i == null) {
					return null;
				}
				
				return i.stream().map(JScssCompiler::fromImport).collect(Collectors.toList());
			}
			
		};
	}
	
	private static Import fromImport(ScssImport previous) {
		return new Import(previous.getImportUri(), previous.getAbsoluteUri(), previous.getContents());
	}
	
	private static ScssImport asImport(Import previous) {
		return new ScssImport(previous.getImportUri(), previous.getAbsoluteUri(), previous.getContents());
	}

	private static Options asOptions(ScssOptions options) {
		Options opt = new Options();
		
		opt.setImporters(options.getImporters().stream().map(JScssCompiler::asImporter).collect(Collectors.toList()));
		opt.setSourceMapContents(options.isSourceMapContents());
		opt.setSourceMapEmbed(options.isSourceMapEmbed());
		opt.setOmitSourceMapUrl(options.isOmitSourceMapUrl());
		
		return opt;
	}

	private static ScssOutput asOutput(Output c) {
		return new ScssOutput(c.getCss());
	}

}
