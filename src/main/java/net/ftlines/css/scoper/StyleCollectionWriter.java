package net.ftlines.css.scoper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.common.base.Objects;
import com.google.debugging.sourcemap.FilePosition;
import com.google.debugging.sourcemap.SourceMapFormat;
import com.google.debugging.sourcemap.SourceMapGenerator;
import com.google.debugging.sourcemap.SourceMapGeneratorFactory;

public class StyleCollectionWriter {

	
	private TreeMap<String, String> nameMap = new TreeMap<String, String>();
	private TreeMap<String, String> styleMap = new TreeMap<String, String>();
	private TreeMap<String, String> sourceMap = new TreeMap<String, String>();
	
	
	public void addOrReplace(ScopedFragmentResult compiledFragments) {
		String scope = compiledFragments.getMetadata().getValue(CssSelectorReplace.SCOPE_PROPERTY);
		if(scope == null)
			throw new RuntimeException("Invalid output scope");
		
		String css = compiledFragments.getScopedCss();
		String existing = styleMap.get(scope);
		if(!Objects.equal(existing, css)) {
			styleMap.put(scope, css);
			sourceMap.put(scope, compiledFragments.getOldCss());
			nameMap.put(scope, compiledFragments.getMetadata().getValue(CssSelectorReplace.SCOPE_SOURCE_NAME));
			onStyleSetChanged();
		}
	}
	
	public Map<String, String> getStyleMap() {
		return Collections.unmodifiableMap(styleMap);
	}
	
	public Map<String, String> getSourceMap() {
		return Collections.unmodifiableMap(sourceMap);
	}
	
	protected void onStyleSetChanged() {
		
	}

	public void writeTo(Path outputStylePath) throws IOException {
		SourceMapGenerator g = SourceMapGeneratorFactory.getInstance(SourceMapFormat.V3);
		outputStylePath.getParent().toFile().mkdirs();
		String mapName = outputStylePath.getFileName() + ".map";
		
		StringBuffer buffer = new StringBuffer();
		int mapLine = 1;
		for(Entry<String, String> compStyle: styleMap.entrySet()) {
			StringBuffer inner = new StringBuffer();
			String[] lines = compStyle.getValue().split(System.getProperty("line.separator"));		
			Arrays.asList(lines).forEach(l -> {
				inner.append(" ").append(l.strip()).append(" ");
			});
			String line =  inner.toString().strip();
			if(line.length() > 0) {
				String srcName = nameMap.get(compStyle.getKey());
				g.addSourcesContent(srcName, sourceMap.get(compStyle.getKey()));
				g.addMapping(srcName, "", new FilePosition(0, 0), new FilePosition(mapLine, 0), new FilePosition(mapLine+2, 0));

				buffer.append("/* " + compStyle.getKey() + " */ ")
					.append(System.getProperty("line.separator"))
					.append(line)
					.append(System.getProperty("line.separator"))
					.append(System.getProperty("line.separator"));
				mapLine += 3;
			}
		}
		
		buffer.append("\n\n/*# sourceMappingURL="+mapName+" */");
	
		Files.write(outputStylePath,buffer.toString().getBytes(),
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Writer bWriter = new OutputStreamWriter(baos);
		g.appendTo(bWriter, outputStylePath.getFileName().toString());
		bWriter.flush();
		byte[] contents = baos.toByteArray();
		bWriter.close();
		
		Files.write(outputStylePath.resolveSibling(mapName), 
			contents,
			StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}
}
