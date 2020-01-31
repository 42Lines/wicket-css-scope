package net.ftlines.css.scoper;

import org.junit.jupiter.api.Test;

import net.ftlines.css.scoper.wicket.WicketPanelMarkupTransformer;

class WicketPanelMarkupTransformerTest {

	@Test
	void test() {
		WicketPanelMarkupTransformer xform = new WicketPanelMarkupTransformer(CssAnalyzerTest.loadResourceAsString("testPanel1.html"));
		System.out.println(xform.apply(new ScopedFragmentResult("", "", ".new{}", "<div class=\"new\"></div>", null) ));
	}

}
