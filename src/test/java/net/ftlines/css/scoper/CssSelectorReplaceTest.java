package net.ftlines.css.scoper;

import static org.antlr.v4.runtime.CharStreams.fromString;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

import antlr.css3.css3Lexer;
import antlr.css3.css3Parser;

class CssSelectorReplaceTest {

	@Test
	void testClassReplace() {
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.CLASS_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, ".simpleStyle"), "fghij");
		
		assertEquals(".fghij {}", replace(".simpleStyle {}", p));
	}
	
	@Test
	void testIdReplace() {
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#simpleStyle"), "fghij");
		
		assertEquals(".fghij {}", replace("#simpleStyle {}", p));
	}

	@Test
	void testExternalAnnotation() {
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#simpleStyle"), "fghij");
		
		assertEquals("#simpleStyle {}", replace("@external #simpleStyle {}", p));
	}
	
	@Test
	void testContainerAnnotation() {
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.SCOPE_PROPERTY, "abcde");
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#simpleStyle"), "fghij");
		
		assertEquals(".abcde #simpleStyle {}", replace("@container #simpleStyle {}", p));
	}
	
	@Test
	void testMultiClassReplace() {
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.CLASS_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, ".simpleStyle"), "fghij");
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.CLASS_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, ".otherStyle"), "klmno");
		
		assertEquals(".fghij .klmno {}", replace(".simpleStyle .otherStyle {}", p));
		assertEquals(".fghij.klmno {}", replace(".simpleStyle.otherStyle {}", p));
		assertEquals(".klmno .fghij{}", replace(".otherStyle .simpleStyle{}", p));
	}
	
	@Test
	void testMixedReplace() {
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.CLASS_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, ".simpleStyle"), "fghij");
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#otherStyle"), "klmno");
		
		assertEquals(".fghij .klmno {}", replace(".simpleStyle #otherStyle {}", p));
		assertEquals(".fghij.klmno {}", replace(".simpleStyle#otherStyle {}", p));
		assertEquals(".klmno .fghij{}", replace("#otherStyle .simpleStyle{}", p));
	}
	
//	@Test
//	void testIdReplaceHexOverlap() { //TODO FIX THIS
//		Properties p = new Properties();
//		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#FFF"), "fghij");
//		
//		assertEquals(".fghij {}", replace("#FFF {}", p));
//	}
	
	public static String replace(String cssBody, Properties p) {
		CodePointCharStream cs = fromString(cssBody);
		css3Lexer lexer = new css3Lexer(cs);
		BufferedTokenStream stream = new BufferedTokenStream(lexer);
		css3Parser par = new css3Parser(stream);
		CssSelectorReplace replace = new CssSelectorReplace(stream, new CssScopeMetadata(p), false);
		ParseTreeWalker.DEFAULT.walk(replace, par.stylesheet());
		return replace.getOutput();
	}
	
}
