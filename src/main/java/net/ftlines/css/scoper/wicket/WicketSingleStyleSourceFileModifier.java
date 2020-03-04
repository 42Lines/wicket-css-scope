package net.ftlines.css.scoper.wicket;

import java.nio.file.Path;

import net.ftlines.css.scoper.PanelizedMarkupTransformer;
import net.ftlines.css.scoper.ScopedFragmentResult;
import net.ftlines.css.scoper.StyleCollectionWriter;

public class WicketSingleStyleSourceFileModifier extends WicketSourceFileModifier {
	
	private StyleCollectionWriter styleCollection;

	public WicketSingleStyleSourceFileModifier(Path filePath, Path inputRoot, Path outputRoot, StyleCollectionWriter styleCollection) {
		super(filePath, inputRoot, outputRoot);
		this.styleCollection = styleCollection;
	}
	
	@Override
	protected PanelizedMarkupTransformer createPanelizedMarkupTransformer(String input) {
		
		if(WicketPanelMarkupContributor.isWicketPanel(input)) {
			return new WicketPanelMarkupTransformer(input) {
				@Override
				protected String composeStyle(ScopedFragmentResult compiledFragments) {
					styleCollection.addOrReplace(compiledFragments);
					return "";
				}
			};
		} else if(WicketExtendsMarkupContributor.isWicketExtend(input)) {
			return new WicketExtendMarkupTransformer(input) {
				@Override
				protected String composeStyle(ScopedFragmentResult compiledFragments) {
					styleCollection.addOrReplace(compiledFragments);
					return "";
				}
			};
		}
		
		throw new RuntimeException("No transformer defined for input");
	}
	
	
	@Override
	public boolean isDiry() {
		return true;
	}


}
