package net.ftlines.css.scoper.wicket;

import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import net.ftlines.css.scoper.MarkupFragmentContributor;

public class WicketPanelMarkupContributor implements MarkupFragmentContributor {

	private String input;

	public WicketPanelMarkupContributor(String input) {
		this.input = input;
	}

	@Override
	public Optional<String> getMarkup() {
		if (!isWicketPanel(input))
			return Optional.empty();

		Document doc = Jsoup.parseBodyFragment(input);
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

		return Optional.of(doc.getElementsByTag("wicket:panel").html());
	}
	
	public static boolean isWicketPanel(String input) {
		return input.toLowerCase().contains("wicket:panel");
	}

}
