package net.ftlines.css.scoper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

class CssAnalyzerTest {
//
//	@Test
//	void testSimpleSelector() {
//		
//		String [] lines = {
//		        ".simpleSelector {",
//		        "  display: block",
//		        "}",
//		    };
//		
//		CssAnalyzer a = createAnalyzer(lines);
//		assertEquals(1, a.getClassSelectors().size());
//		assertEquals(".simpleSelector", a.getClassSelectors().iterator().next());
//	}
//	
//	@Test
//	void testMultipleSelector() {
//		
//		String [] lines = {
//		        ".simpleSelector1 {",
//		        "  display: block",
//		        "} ",
//		        ".simpleSelector2 {",
//		        "  display: block",
//		        "} ",
//		        ".simpleSelector3 {",
//		        "  display: block",
//		        "} ",
//		    };
//		
//		CssAnalyzer a = createAnalyzer(lines);
//		assertEquals(3, a.getClassSelectors().size());
//		assertEquals(".simpleSelector1", new ArrayList<>(a.getClassSelectors()).get(0));
//		assertEquals(".simpleSelector2", new ArrayList<>(a.getClassSelectors()).get(1));
//		assertEquals(".simpleSelector3", new ArrayList<>(a.getClassSelectors()).get(2));
//	}
//	
//	@Test
//	void testMultipleSelectorExcludeIdSelectors() {
//		
//		String [] lines = {
//		        ".simpleSelector1 {",
//		        "  display: block",
//		        "} ",
//		        "#simpleSelector2 {",
//		        "  display: block",
//		        "} ",
//		        ".simpleSelector3 {",
//		        "  display: block",
//		        "} ",
//		    };
//		
//		CssAnalyzer a = createAnalyzer(lines);
//		assertEquals(2, a.getClassSelectors().size());
//		assertEquals(".simpleSelector1", new ArrayList<>(a.getClassSelectors()).get(0));
//		assertEquals(".simpleSelector3", new ArrayList<>(a.getClassSelectors()).get(1));
//	}
//	
//	@Test
//	void testCompoundSelector() {
//		
//		String [] lines = {
//		        "body .simpleSelector1, h1.simpleSelector2 {",
//		        "  display: block",
//		        "} ",
//		        ".simpleSelector4 .simpleSelector5, .simpleSelector6, .simpleSelector7 > .simpleSelector8 {",
//		        "  display: block",
//		        "} ",
//		        ".simpleSelector3 {",
//		        "  display: block",
//		        "} ",
//		    };
//		
//		CssAnalyzer a = createAnalyzer(lines);
//		assertEquals(8, a.getClassSelectors().size());
//		assertEquals(".simpleSelector1", new ArrayList<>(a.getClassSelectors()).get(0));
//		assertEquals(".simpleSelector2", new ArrayList<>(a.getClassSelectors()).get(1));
//		assertEquals(".simpleSelector3", new ArrayList<>(a.getClassSelectors()).get(2));
//		assertEquals(".simpleSelector4", new ArrayList<>(a.getClassSelectors()).get(3));
//		assertEquals(".simpleSelector5", new ArrayList<>(a.getClassSelectors()).get(4));
//		assertEquals(".simpleSelector6", new ArrayList<>(a.getClassSelectors()).get(5));
//		assertEquals(".simpleSelector7", new ArrayList<>(a.getClassSelectors()).get(6));
//		assertEquals(".simpleSelector8", new ArrayList<>(a.getClassSelectors()).get(7));
//	}
//	
//	@Test
//	void testFromFileSample1() {
//		CssAnalyzer a = createAnalyzerFromTestFile("testFile1.css");
//		assertEquals(3, a.getClassSelectors().size());
//		assertEquals(".div", new ArrayList<>(a.getClassSelectors()).get(0));
//		assertEquals(".notification-center", new ArrayList<>(a.getClassSelectors()).get(1));
//		assertEquals(".wicket-modal", new ArrayList<>(a.getClassSelectors()).get(2));
//	}
//	
//	@Test
//	void testFromFileSample2() {
//		CssAnalyzer a = createAnalyzerFromTestFile("testFile2.css");
//		a.getClassSelectors().forEach(System.out::println);
//		assertEquals(3, a.getClassSelectors().size());
//		assertEquals(".thumb", new ArrayList<>(a.getClassSelectors()).get(0));
//		assertEquals(".w_content_container", new ArrayList<>(a.getClassSelectors()).get(1));
//		assertEquals(".wicket-modal", new ArrayList<>(a.getClassSelectors()).get(2));
//	}
//	
//	protected CssAnalyzer createAnalyzerFromTestFile(String filename) {
//		return createAnalyzer(loadResourceAsString(filename));
//	}
//	
//	protected CssAnalyzer createAnalyzer(String...lines) {
//		StringBuilder fileLines = new StringBuilder();
//	      for (String line : lines) {
//	        fileLines.append(line);
//	      }
//	      
//	     return new CssAnalyzer(fileLines.toString());
//	}
	
	public static String loadResourceAsString(String fileName) {
	    Scanner scanner = new Scanner(CssAnalyzerTest.class.getResourceAsStream(fileName));
	    String contents = scanner.useDelimiter("\\A").next();
	    scanner.close();
//	    System.out.println(contents);
	    return contents;
	}

}
