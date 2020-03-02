package net.ftlines.css.scoper.wicket;

import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import net.ftlines.css.scoper.MarkupFragmentContributor;

public class WicketExtendsMarkupContributor implements MarkupFragmentContributor {

	private String input;

	public WicketExtendsMarkupContributor(String input) {
		this.input = input;
	}

	@Override
	public Optional<String> getMarkup() {
		if (!isWicketExtend(input))
			return Optional.empty();

		Document doc = Jsoup.parseBodyFragment(input);
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

		return Optional.of(doc.getElementsByTag("wicket:extend").html());
	}
	
	public static boolean isWicketExtend(String input) {
		return input.toLowerCase().contains("wicket:extend");
	}

}