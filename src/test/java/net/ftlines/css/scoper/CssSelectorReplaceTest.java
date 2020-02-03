package net.ftlines.css.scoper;

import static org.antlr.v4.runtime.CharStreams.fromString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
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
		p.setProperty(CssSelectorReplace.SCOPE_PROPERTY, "abcde");
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#simpleStyle"), "fghij");
		
		assertEquals(".fghij {}", replace("#simpleStyle {}", p));
	}
	
	@Test
	void testClassReplaceNotHexColor() {
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.CLASS_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, ".simpleStyle"), "fghij");
		
		assertEquals(".fghij {color: #FFF}", replace(".simpleStyle {color: #FFF}", p));
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

	
	@Test
	void testIdReplaceHexOverlap() { //TODO FIX THIS
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#FFF"), "fghij");
		
		assertEquals(".fghij {}", replace("#FFF {}", p));
	}
	
	@Test
	void testIdReplaceHexOverlapComplexRules() { //TODO FIX THIS
		Properties p = new Properties();
		p.setProperty(CssSelectorReplace.SCOPE_PROPERTY, "abcde");
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#EEE"), "fghij");
		p.setProperty(CssSelectorReplace.getPropertyKey(CssSelectorReplace.HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, "#FFF"), "lmnop");
		

		assertEquals(".fghij { background-color: #FFF;}", replace("#EEE { background-color: #FFF;}", p));
		assertEquals(".fghij { background-color: #214984;}", replace("#EEE { background-color: #214984;}", p));
		assertEquals(".lmnop { background-color: #FFF;}", replace("#FFF { background-color: #FFF;}", p));
		
		
		assertEquals(".abcde #class-grades .form-list label, .abcde #class-grades .form-list .label {	width:120px; }", 
			replace("@container #class-grades .form-list label, #class-grades .form-list .label {	width:120px; }", p));
		
		
		assertEquals(".lmnop { background-color: #FFF;}", replace(".wicket-upload .progressbar .progress {height:10px;	background: #214984;	}", p));
		
		
	}
	
	
	
	
	public static String replace(String cssBody, Properties p) {
		return CssSelectorReplace.parse(cssBody, new CssScopeMetadata(p), false).getOutput();
	}
	
	private static void viz(String source) {
		
	    CodePointCharStream cs = fromString(source);	
		css3Lexer lexer = new css3Lexer(cs);
		BufferedTokenStream stream = new BufferedTokenStream(lexer);
		css3Parser par = new css3Parser(stream);

		CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
		
		for (Token t : tokens.getTokens()) {
            String symbolicName = lexer.VOCABULARY.getSymbolicName(t.getType());
            String literalName = lexer.VOCABULARY.getLiteralName(t.getType());
            System.out.printf("  %-20s '%s'\n",
                    symbolicName == null ? literalName : symbolicName,
                    t.getText().replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t"));
        }
		
		JFrame frame = new JFrame("Antlr AST");
        JPanel panel = new JPanel();
        TreeViewer viewer = new TreeViewer(Arrays.asList(
        	par.getRuleNames()),par.stylesheet());
        viewer.setScale(1.5); // Scale a little
        panel.add(viewer);
        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
	}
	
	public static void main(String[] args) throws IOException {
		//viz("#eee { background-color: #FFF;}");
		
		viz(".class-grades .form-list .label {	width:120px; }");
		
//		viz(new WicketPanelCssContributor(AbstractSourceFileModifier.pathAsString(Paths.get(
//			"/Users/peter/git/harmonize/application/lms/target/classes/net/ftlines/lms/discussion/activity/classic/GradeByTypePanel.html"))).getCss().get());
	}
	
}
