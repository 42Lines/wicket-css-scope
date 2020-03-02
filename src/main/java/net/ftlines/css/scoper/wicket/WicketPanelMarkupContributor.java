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
		if (input.toLowerCase().contains("wicket:panel"))
			return getPanelMarkup();
		
		if (input.toLowerCase().contains("wicket:extend"))
			return getExtendsMarkup();
		
		return Optional.empty();
	}
	
	private Optional<String> getPanelMarkup() {
		if (!input.toLowerCase().contains("wicket:panel"))
			return getExtendsMarkup();

		Document doc = Jsoup.parseBodyFragment(input);
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

		return Optional.of(doc.getElementsByTag("wicket:panel").html());
	}
	
	private Optional<String> getExtendsMarkup() {
		if (!input.toLowerCase().contains("wicket:extend"))
			return Optional.empty();

		Document doc = Jsoup.parseBodyFragment(input);
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

		return Optional.of(doc.getElementsByTag("wicket:extend").html());
	}

}
