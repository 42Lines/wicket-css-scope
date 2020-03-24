package net.ftlines.css.scoper.wicket;

import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import net.ftlines.css.scoper.MarkupFragmentContributor;

public class WicketPageMarkupContributor implements MarkupFragmentContributor {

	private String input;

	public WicketPageMarkupContributor(String input) {
		this.input = input;
	}

	@Override
	public Optional<String> getMarkup() {
		if (!isWicketPage(input))
			return Optional.empty();

		Document doc = Jsoup.parse(input, "", Parser.xmlParser());
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

		return Optional.of(doc.getElementsByTag("body").html());
	}
	
	public static boolean isWicketPage(String input) {
		return input.toLowerCase().contains("<body");
	}

}