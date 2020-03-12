package net.ftlines.css.scoper.wicket;

import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import net.ftlines.css.scoper.CssSyleFragmentContributor;

public class WicketPanelCssContributor implements CssSyleFragmentContributor {

	private String input;

	public WicketPanelCssContributor(String input) {
		this.input = input;
	}

	@Override
	public Optional<String> getCss() {
		if (!input.toLowerCase().contains("wicket:head"))
			return Optional.empty();

		Document doc = Jsoup.parseBodyFragment(input);
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

		if (doc.getElementsByTag("wicket:head").size() == 0)
			return Optional.empty();

		if (doc.getElementsByTag("wicket:head").select("style:not([ignore_compile])").size() == 0)
			return Optional.empty();

		return Optional.of(doc.getElementsByTag("wicket:head").select("style:not([ignore_compile])").html());
	}

}
