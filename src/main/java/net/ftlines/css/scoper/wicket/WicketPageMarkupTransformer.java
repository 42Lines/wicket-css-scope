package net.ftlines.css.scoper.wicket;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import net.ftlines.css.scoper.ScopedFragmentResult;

public class WicketPageMarkupTransformer  extends WicketPanelMarkupTransformer {

	public WicketPageMarkupTransformer(String sourceInput) {
		super(sourceInput);
	}

	@Override
	protected String getWicketTag() {
		return "body";
	}
	
	@Override
	public String apply(ScopedFragmentResult compiledFragments) {
		Document doc = Jsoup.parse(getSourceInput(), "", Parser.xmlParser());
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
		
		doc.getElementsByTag("wicket:scss").remove();
		doc.getElementsByTag("head").select("style:not([ignore_compile])").remove();
		
		String style = composeStyle(compiledFragments).strip();
		if(!style.isBlank()) {
			Elements headTags = doc.getElementsByTag("head");
			if(headTags.size() > 0) {
				headTags.get(0).appendElement("style").html(style);
			} else {
				doc.prependElement("head").append(style);
			}
		}
		
		Elements bodyTags = doc.getElementsByTag("body");
		if(bodyTags.size() > 0) {
			bodyTags.get(0).html(compiledFragments.getScopedMarkup().strip());
		}

		return doc.html();
	}
	
	@Override
	protected String composeBody(ScopedFragmentResult compiledFragments) {
		return "";
	}
	
	@Override
	protected String composeHead(ScopedFragmentResult compiledFragments) {	
		return "";
	}
	
	@Override
	protected String composeStyle(ScopedFragmentResult compiledFragments) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<style compiled=\"true\">").append("\n");
		buffer.append(compiledFragments.getScopedCss()).append("\n");
		buffer.append("</style>").append("\n\n");
		return buffer.toString();
	}

	@Override
	protected String getOtherNonStyleHeadElements() {
		return "";
	}

}