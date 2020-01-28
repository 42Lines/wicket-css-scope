package net.ftlines.css.scoper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import net.ftlines.css.scoper.Watcher.SortedProperties;

public class WicketSourceFileModifier {

	private Path filePath;
	private Path inputRoot;
	private Path outputRoot;

	private CssScopeMetadata metaData;

	public WicketSourceFileModifier(Path filePath, Path inputRoot, Path outputRoot) {
		this.filePath = filePath;
		this.inputRoot = inputRoot;
		this.outputRoot = outputRoot;

		if (Files.exists(outputRoot.resolve(filePath.getParent()).resolve(getPropertiesFileName()))) {
			Path pth = outputRoot.resolve(filePath.getParent()).resolve(getPropertiesFileName());
			SortedProperties p = new SortedProperties();
			try {
				p.load(Files.newInputStream(pth));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			metaData = new CssScopeMetadata(p);
		} else {
			metaData = new CssScopeMetadata(new SortedProperties());
		}

	}

	public String getPropertiesFileName() {
		int idx = filePath.getFileName().toString().toLowerCase().indexOf(".html");
		;
		return filePath.getFileName().toString().substring(0, idx) + ".compiled.properties";
	}

	public String getFileName() {
		return filePath.getFileName().toString();
	}

	public String getFileContents() {
		try {
			return Files.readAllLines(inputRoot.resolve(filePath)).stream().collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CssScopeMetadata getMetaData() {
		return metaData;
	}

	public String getSourceFileHash() {
		return CssScopeMetadata.hashString(getFileContents());
	}

	public boolean isWicketPanel() {
		try {
			return Jsoup.parse(getFileContents()).getElementsByTag("wicket:panel").size() == 1;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isStyleDefiner() {
		try {
			return Jsoup.parse(getFileContents()).getElementsByTag("wicket:head").select("style").size() > 0;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getSourceMarkup() {
		Document doc = Jsoup.parseBodyFragment(getFileContents());
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
		return doc.getElementsByTag("wicket:panel").html();
	}

	public String getSourceStyle() {
		Document doc = Jsoup.parseBodyFragment(getFileContents());
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
		return doc.getElementsByTag("wicket:head").select("style").html();
	}

	public boolean isDiry() {
		return !getSourceFileHash().equalsIgnoreCase(getMetaData().getValue("source.hash", () -> {
			return "";
		}));
	}

	public String getOutput() {
		ScopedFragmentResult r = ScopedFragmentResult.transform(getSourceStyle(), getSourceMarkup(), getMetaData());

		Document doc = Jsoup.parseBodyFragment(getFileContents());
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));
		doc.getElementsByTag("wicket:head").select("style").html(r.getScopedCss());
		doc.getElementsByTag("wicket:panel").html(r.getScopedMarkup());

		return "<html xmlns:wicket>\n" + doc.body().html() + "\n</html>";
	}

	public Path getFileOutputPath() {
		return outputRoot.resolve(filePath);
	}

	public Path getPropertiesOutputPath() {
		return outputRoot.resolve(filePath.getParent()).resolve(getPropertiesFileName());
	}

	public void save() throws Exception {
		if (isDiry()) {
			System.out.println("Writing " + getFileOutputPath());
			getMetaData().setValue("source.hash", getSourceFileHash());
			Files.write(getFileOutputPath(), getOutput().getBytes(), StandardOpenOption.WRITE,
				StandardOpenOption.CREATE);
			Files.write(getPropertiesOutputPath(), CssScopeMetadata.getMetaDataAsString(getMetaData()).getBytes(),
				StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		}
	}

	public void process() {
		try {
			getFileOutputPath().getParent().toFile().mkdirs();
			if (isWicketPanel() && isStyleDefiner()) {
				save();
			} else {
				Files.copy(inputRoot.resolve(filePath), getFileOutputPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
