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
		return "<html xmlns:wicket>\n" + "<wicket:head>\n<style>\n" + compiledFragments.getScopedCss() + "\n"
			+ "</style>" + getOtherNonStyleHeadElements() + "\n</wicket:head>\n<wicket:extend>"
			+ compiledFragments.getScopedMarkup() + "\n</wicket:extend>" + "</html>";
	}

	private String getOtherNonStyleHeadElements() {
		Document doc = Jsoup.parseBodyFragment(sourceInput);
		Elements headTags = doc.getElementsByTag("wicket:head");
		headTags.select("style").remove();
		return headTags.get(0).html();
	}

}

