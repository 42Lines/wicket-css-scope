package net.ftlines.css.scoper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

import net.ftlines.css.scoper.Watcher.SortedProperties;

public abstract class AbstractSourceFileModifier {

	private Path filePath;
	private Path inputRoot;
	private Path outputRoot;

	private CssScopeMetadata metaData;

	private boolean debugMode = false;

	public AbstractSourceFileModifier(Path filePath, Path inputRoot, Path outputRoot) {
		this.filePath = filePath;
		this.inputRoot = inputRoot;
		this.outputRoot = outputRoot;
	}
	
	public final Path getFilePath() {
		return filePath;
	}
	
	public final Path getInputRoot() {
		return inputRoot;
	}
	
	public final Path getOutputRoot() {
		return outputRoot;
	}

	public AbstractSourceFileModifier setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
		return this;
	}

	public String getPropertiesFileName() {
		int idx = filePath.getFileName().toString().toLowerCase().indexOf(".html");
		return filePath.getFileName().toString().substring(0, idx) + ".compiled.properties";
	}

	public String getFileName() {
		return filePath.getFileName().toString();
	}

	private String getFileContents() {
		try {
			return pathAsString(inputRoot.resolve(filePath));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public CssScopeMetadata getMetaData() {

		if (metaData == null) {
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

		return metaData;
	}

	public String getSourceFileHash() {
		return CssScopeMetadata.hashString(getFileContents());
	}

	private boolean isPanelDefiner(List<MarkupFragmentContributor> markupContributors) {
		return markupContributors.stream().filter(c -> c.getMarkup().isPresent()).count() > 0;
	}

	private boolean isStyleDefiner(List<CssSyleFragmentContributor> cssContributors) {
		return cssContributors.stream().filter(c -> c.getCss().isPresent()).count() > 0;
	}

	public boolean isDiry() {
		return !getSourceFileHash().equalsIgnoreCase(getMetaData().getValue("source.hash", () -> {
			return "";
		}));
	}

	private String getOutput(List<CssSyleFragmentContributor> cssContributors,
		List<MarkupFragmentContributor> markupContributors, PanelizedMarkupTransformer transformer) {
		return transformer.apply(ScopedFragmentResult.transform(CssSyleFragmentContributor.combine(cssContributors),
			MarkupFragmentContributor.combine(markupContributors), getMetaData(), debugMode));
	}

	public Path getFileOutputPath() {
		return outputRoot.resolve(filePath);
	}

	public Path getPropertiesOutputPath() {
		return outputRoot.resolve(filePath.getParent()).resolve(getPropertiesFileName());
	}

	private void save(List<CssSyleFragmentContributor> cssContributors,
		List<MarkupFragmentContributor> markupContributors, PanelizedMarkupTransformer transformer) throws Exception {
		if (isDiry()) {
			logString("Compiling " + getFileOutputPath());
			getMetaData().setValue("source.hash", getSourceFileHash());
			getFileOutputPath().getParent().toFile().mkdirs();
			Files.write(getFileOutputPath(), getOutput(cssContributors, markupContributors, transformer).getBytes(),
				StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			Files.write(getPropertiesOutputPath(), CssScopeMetadata.getMetaDataAsString(getMetaData()).getBytes(),
				StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		}
	}

	protected abstract List<CssSyleFragmentContributor> createCssSyleFragmentContributor(String input);

	protected abstract List<MarkupFragmentContributor> createMarkupFragmentContributor(String input);

	protected abstract PanelizedMarkupTransformer createPanelizedMarkupTransformer(String input);

	public void process() {

		String input = getFileContents();
		try {

			List<CssSyleFragmentContributor> cssContributors = createCssSyleFragmentContributor(input).stream()
				.map(CssSyleFragmentContributor::cache).collect(Collectors.toList());
			List<MarkupFragmentContributor> markupContributors = createMarkupFragmentContributor(input).stream()
				.map(MarkupFragmentContributor::cache).collect(Collectors.toList());

			if (isPanelDefiner(markupContributors) && isStyleDefiner(cssContributors)) {
				save(cssContributors, markupContributors, createPanelizedMarkupTransformer(input));
			} else {
				if (copy(filePath, inputRoot, outputRoot)) {
					logString("Syncing " + filePath);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error processing " + inputRoot.resolve(filePath), e);
		}
	}

	public static boolean copy(Path filePath, Path inputPath, Path outputPath) throws IOException {
		if (Files.exists(inputPath.resolve(filePath))) {
			outputPath.resolve(filePath).getParent().toFile().mkdirs();
			if (isChanged(inputPath.resolve(filePath), outputPath.resolve(filePath))) {
				Files.copy(inputPath.resolve(filePath), outputPath.resolve(filePath),
					StandardCopyOption.REPLACE_EXISTING);
				return true;
			}
		}

		return false;
	}

	private static boolean isChanged(Path sourceFile, Path targetFile) throws IOException {
		final long size = Files.size(sourceFile);

		if (!Files.exists(targetFile))
			return true;

		if (size != Files.size(targetFile))
			return true;

		return !CssScopeMetadata.hashString(pathAsString(sourceFile))
			.equalsIgnoreCase(CssScopeMetadata.hashString(pathAsString(targetFile)));
	}

	private static String pathAsString(Path file) throws IOException {
		return Files.readAllLines(file).stream().collect(Collectors.joining("\n"));
	}

	protected void logString(String message) {
		System.out.println(message);
	}

}
