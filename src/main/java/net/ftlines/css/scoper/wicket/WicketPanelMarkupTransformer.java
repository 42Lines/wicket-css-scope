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
		return "<html xmlns:wicket>\n" + "<wicket:head>\n<style>\n" + compiledFragments.getScopedCss() + "\n"
			+ "</style>" + getOtherNonStyleHeadElements() + "\n</wicket:head>\n<wicket:panel>"
			+ compiledFragments.getScopedMarkup() + "\n</wicket:panel>" + "</html>";
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
