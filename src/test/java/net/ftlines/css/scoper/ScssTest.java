package net.ftlines.css.scoper;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.importer.Import;
import net.ftlines.css.scoper.wicket.WicketPanelScssContributor;

public class ScssTest {

	@Test
	void test() throws CompilationException {
		
		Options options = new Options();
		options.setImporters(Collections.singleton(this::doImport));
		
		String src = CssAnalyzerTest.loadResourceAsString("testPanel1.html");
//		System.out.println(WicketPanelScssContributor.extractScss(src));
//		System.out.println(" ------------ ");
		final Compiler compiler = new Compiler();
		final Output output = compiler.compileString(
			WicketPanelScssContributor.extractScss(src), options
		);

		
		System.out.println(output.getCss());
	}
	
	
	private Collection<Import> doImport(String url, Import previous) {
//		System.out.println("import: " + url  + "  " + previous.getAbsoluteUri());
		URI uri = Path.of(".").resolve(url).toUri();
//		System.out.println(" -- " + Path.of("src/test/java/net/ftlines/css/scoper").resolve(url).toAbsolutePath());
		return Collections.singleton(new Import(uri, uri, CssAnalyzerTest.loadResourceAsString(url)));
	}
	
}
