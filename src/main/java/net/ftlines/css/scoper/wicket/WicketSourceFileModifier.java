package net.ftlines.css.scoper.wicket;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.bit3.jsass.Options;
import io.bit3.jsass.importer.Importer;
import net.ftlines.css.scoper.AbstractSourceFileModifier;
import net.ftlines.css.scoper.CssSyleFragmentContributor;
import net.ftlines.css.scoper.MarkupFragmentContributor;
import net.ftlines.css.scoper.PanelizedMarkupTransformer;

public class WicketSourceFileModifier extends AbstractSourceFileModifier {

	public WicketSourceFileModifier(Path filePath, Path inputRoot, Path outputRoot) {
		super(filePath, inputRoot, outputRoot);
	}

	@Override
	protected List<CssSyleFragmentContributor> createCssSyleFragmentContributor(String input) {
		return Arrays.asList(new WicketPanelScssContributor(input) {
			@Override
			protected void configureOptions(Options options) {
				super.configureOptions(options);
				options.getImporters().addAll(getAllScssImporters());
			}
		}, new WicketPanelCssContributor(input));
	}

	@Override
	protected List<MarkupFragmentContributor> createMarkupFragmentContributor(String input) {
		if(WicketPanelMarkupContributor.isWicketPanel(input)) {
			return Arrays.asList(new WicketPanelMarkupContributor(input));
		} else if(WicketExtendsMarkupContributor.isWicketExtend(input)) {
			return Arrays.asList(new WicketExtendsMarkupContributor(input));
		} else if(WicketPageMarkupContributor.isWicketPage(input)) {
			return Arrays.asList(new WicketPageMarkupContributor(input));
		}
		
		return Collections.emptyList();
	}

	@Override
	protected PanelizedMarkupTransformer createPanelizedMarkupTransformer(String input) {
		
		if(WicketPanelMarkupContributor.isWicketPanel(input)) {
			return new WicketPanelMarkupTransformer(input);
		} else if(WicketExtendsMarkupContributor.isWicketExtend(input)) {
			return new WicketExtendMarkupTransformer(input);
		} else if(WicketPageMarkupContributor.isWicketPage(input)) {
			return new WicketPageMarkupTransformer(input);
		}
		
		throw new RuntimeException("No transformer defined for input");
	}
	
	protected Collection<Importer> getAllScssImporters() {
		return new ArrayList<>();
	}

}
