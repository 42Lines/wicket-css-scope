package net.ftlines.css.scoper.wicket;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import net.ftlines.css.scoper.PanelizedMarkupTransformer;
import net.ftlines.css.scoper.ScopedFragmentResult;

public class WicketPanelMarkupTransformer implements PanelizedMarkupTransformer {

	private String sourceInput;

	public WicketPanelMarkupTransformer(String sourceInput) {
		this.sourceInput = sourceInput;
	}

	@Override
	public String apply(ScopedFragmentResult compiledFragments) {
		return "<html xmlns:wicket>\n" // 
			+ composeHead(compiledFragments) // 
			+ composeBody(compiledFragments)  //
			+ "</html>"; //
	}
	
	protected String getWicketTag() {
		return "wicket:panel";
	}
	
	protected String composeBody(ScopedFragmentResult compiledFragments) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<"+getWicketTag()+">").append("\n");
		buffer.append(compiledFragments.getScopedMarkup()).append("\n");
		buffer.append("</"+getWicketTag()+">").append("\n");
		return buffer.toString();
	}
	
	protected String composeHead(ScopedFragmentResult compiledFragments) {
		String head = composeStyle(compiledFragments).strip() + getOtherNonStyleHeadElements().strip();
		if(head.length() > 0) {
			return "<wicket:head>" + head + "</wicket:head>";
		}
		
		return "";
	}
	
	protected String composeStyle(ScopedFragmentResult compiledFragments) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<style compiled=\"true\">").append("\n");
		buffer.append(compiledFragments.getScopedCss()).append("\n");
		buffer.append("</style>").append("\n\n");
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
