package net.ftlines.css.scoper;

import static org.antlr.v4.runtime.CharStreams.fromString;

import java.util.Properties;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import antlr.css3.css3Lexer;
import antlr.css3.css3Parser;

class CssSelectorReplaceTest {

	@Test
	void test() {
		String src = CssAnalyzerTest.loadResourceAsString("testFile2.css");
		String out = replace(src);
		System.out.println("\n\n" + src + "\n====\n" + out);
	}

	private static String replace(String cssBody) {
		CodePointCharStream cs = fromString(cssBody);

		css3Lexer lexer = new css3Lexer(cs);
		BufferedTokenStream stream = new BufferedTokenStream(lexer);
		css3Parser par = new css3Parser(stream);
		CssSelectorReplace replace = new CssSelectorReplace(stream, new CssScopeMetadata(new Properties()), false);
		ParseTreeWalker.DEFAULT.walk(replace, par.stylesheet());
		return replace.getOutput();
	}
	
}
