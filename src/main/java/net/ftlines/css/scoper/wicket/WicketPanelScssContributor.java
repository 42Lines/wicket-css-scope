package net.ftlines.css.scoper.wicket;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import net.ftlines.css.scoper.AbstractScssFragmentContributor;

public class WicketPanelScssContributor extends AbstractScssFragmentContributor {

	private String input;

	public WicketPanelScssContributor(String input) {
		this.input = input;
	}

	@Override
	public Optional<String> getScss() {

		if (!input.toLowerCase().contains("scss"))
			return Optional.empty();

		return Optional.ofNullable(extractScss(input));
	}

	public static String extractScss(String input) {
		return StringUtils.substringBetween(input.replace("</wicket:scss>", "<wicket:scss>"), "<wicket:scss>");
	}

}
