package net.ftlines.css.scoper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.base.Objects;

public class StyleCollectionWriter {

	private TreeMap<String, String> styleMap = new TreeMap<String, String>();
	
	
	public void addOrReplace(ScopedFragmentResult compiledFragments) {
		String scope = compiledFragments.getMetadata().getValue(CssSelectorReplace.SCOPE_PROPERTY);
		if(scope == null)
			throw new RuntimeException("Invalid output scope");
		
		String css = compiledFragments.getScopedCss();
		String existing = styleMap.get(scope);
		if(!Objects.equal(existing, css)) {
			styleMap.put(scope, css);
			onStyleSetChanged();
		}
	}
	
	protected void onStyleSetChanged() {
		
	}
	
	protected String composeStyle() {
		return styleMap.values().stream().collect(Collectors.joining("\n\n"));
	}

	public void writeTo(Path outputStylePath) throws IOException {
		outputStylePath.getParent().toFile().mkdirs();
		Files.write(outputStylePath, composeStyle().getBytes(),
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}
}
