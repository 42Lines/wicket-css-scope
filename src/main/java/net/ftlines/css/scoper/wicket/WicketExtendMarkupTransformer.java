package net.ftlines.css.scoper.wicket;

public class WicketExtendMarkupTransformer  extends WicketPanelMarkupTransformer {

	public WicketExtendMarkupTransformer(String sourceInput) {
		super(sourceInput);
	}

	@Override
	protected String getWicketTag() {
		return "wicket:extend";
	}

}

