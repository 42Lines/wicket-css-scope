package net.ftlines.css.scoper.wicket;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import net.ftlines.css.scoper.PanelizedMarkupTransformer;
import net.ftlines.css.scoper.ScopedFragmentResult;

public class WicketExtendMarkupTransformer  implements PanelizedMarkupTransformer {

	private String sourceInput;

	public WicketExtendMarkupTransformer(String sourceInput) {
		this.sourceInput = sourceInput;
	}

	@Override
	public String apply(ScopedFragmentResult compiledFragments) {
		return "<html xmlns:wicket>\n" // 
			+ composeHead(compiledFragments) // 
			+ composeBody(compiledFragments)  //
			+ "</html>"; //
	}
	
	
	protected String composeBody(ScopedFragmentResult compiledFragments) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<wicket:extend>").append("\n");
		buffer.append(compiledFragments.getScopedMarkup()).append("\n");
		buffer.append("</wicket:extend>").append("\n");
		return buffer.toString();
	}
	
	protected String composeHead(ScopedFragmentResult compiledFragments) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<wicket:head>").append("\n");
		buffer.append("<style compiled=\"true\">").append("\n");
		buffer.append(compiledFragments.getScopedCss()).append("\n");
		buffer.append("</style>").append("\n\n");
		
		buffer.append(getOtherNonStyleHeadElements()).append("\n");
		
		buffer.append("</wicket:head>").append("\n");
		return buffer.toString();
	}

	private String getOtherNonStyleHeadElements() {
		
		if(!sourceInput.toLowerCase().contains("wicket:head"))
			return "";
		
		Document doc = Jsoup.parseBodyFragment(sourceInput);
		Elements headTags = doc.getElementsByTag("wicket:head");
		headTags.select("style").remove();
		return headTags.get(0).html();
	}

}

