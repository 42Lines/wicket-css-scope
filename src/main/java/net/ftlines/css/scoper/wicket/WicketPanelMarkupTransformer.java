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
		// Document doc = Jsoup.parseBodyFragment(sourceInput);
		// doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
		//
		// doc.getElementsByTag("wicket:scss").remove();
		// getOrCreateStyleElement(doc).html(compiledFragments.getScopedCss());
		// doc.getElementsByTag("wicket:panel").html(compiledFragments.getScopedMarkup());

		return "<html xmlns:wicket>\n" + "<wicket:head>\n<style>\n" + compiledFragments.getScopedCss() + "\n"
			+ "</style>" + getOtherNonStyleHeadElements() + "\n</wicket:head>\n<wicket:panel>"
			+ compiledFragments.getScopedMarkup() + "\n</wicket:panel>" + "</html>";
	}

	private String getOtherNonStyleHeadElements() {
		Document doc = Jsoup.parseBodyFragment(sourceInput);
		Elements headTags = doc.getElementsByTag("wicket:head");
		headTags.select("style").remove();
		return headTags.get(0).html();
	}

	// private Element getOrCreateHeadElement(Document doc) {
	// if(doc.getElementsByTag("wicket:head").size() > 0) {
	// doc.getElementsByTag("wicket:head").get(0);
	// }
	//
	// return doc.prependElement("wicket:head");
	// }
	//
	// private Element getOrCreateStyleElement(Document doc) {
	// System.out.println( getOrCreateHeadElement(doc).select("style").size() );
	// if(getOrCreateHeadElement(doc).select("style").size() > 0) {
	// getOrCreateHeadElement(doc).select("style").get(0);
	// }
	//
	// return getOrCreateHeadElement(doc).prependElement("style");
	// }

}
