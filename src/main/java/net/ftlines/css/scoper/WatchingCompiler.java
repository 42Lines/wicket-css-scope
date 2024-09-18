package net.ftlines.css.scoper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.bit3.jsass.importer.Importer;
import net.ftlines.css.scoper.AbstractScssFragmentContributor.FilePathScssImportResolver;
import net.ftlines.css.scoper.Watcher.Phase;
import net.ftlines.css.scoper.scss.ScssCompilerInterface.ScssImporter;
import net.ftlines.css.scoper.wicket.WicketSingleStyleSourceFileModifier;
import net.ftlines.css.scoper.wicket.WicketSourceFileModifier;

public class WatchingCompiler {

	public Path outputRootPath;
	public Path inputRootPath;
	public Supplier<Collection<File>> scssImportRoot;
	public Path singleFileOutputPath;
	
	public WatchingCompiler(Path inputPath, Path outputPath, Supplier<Collection<File>> scssImportRoot, Path singleFileOutputPath) {
		super();
		this.inputRootPath = inputPath;
		this.outputRootPath = outputPath;
		this.scssImportRoot = scssImportRoot;
		this.singleFileOutputPath = singleFileOutputPath;
	}

	private AtomicReference<Phase> phase = new AtomicReference<Watcher.Phase>(Phase.STARTUP);
	private StyleCollectionWriter styles = new StyleCollectionWriter() {
		@Override
		protected void onStyleSetChanged() {
			if(phase.get() == Phase.WATCHING) {
				writeSingleStyleSheet();
			}
		}
	};
	
	public void setPhase(Phase p) {
		
		if(p == Phase.REBUILDING) {
			logString(phase.get(), "Dependency changed...rebuilding all.");
		}
		
		phase.set(p);
		writeSingleStyleSheet();
	}
	
	public Phase getPhase() {
		return phase.get();
	}
	
	public void process(Path file) {
		if(StandardSassCompiler.isStandardSassFile(file)) {
			createStandardSassCompiler(file).process();
		} else {
			createModifier(file).setDebugMode(isDebugMode()).process();
		}
		
		onChange(file);
	}
	
	protected void onChange(Path file) {
		
	}
	
	private void writeSingleStyleSheet() {
		if(singleFileOutputPath != null) {
 			try {
 				styles.writeTo(singleFileOutputPath);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
 		}
	}
	
	protected boolean isDebugMode() {
		return false;
	}
	
	private StandardSassCompiler createStandardSassCompiler(Path file) {
		return new StandardSassCompiler(file, inputRootPath, outputRootPath) {
			@Override
			protected java.util.Collection<ScssImporter> getAllScssImporters() {
				return createImporterSetRelativeTo(file, super.getAllScssImporters());
			}
			
			@Override
			protected void logString(String message) {
				WatchingCompiler.this.logString(phase.get(), message);
			}
			
		};
	}
	
	protected AbstractSourceFileModifier createModifier(Path file) {

		if(singleFileOutputPath != null) {
			return new WicketSingleStyleSourceFileModifier(file, inputRootPath, outputRootPath, styles) {

				@Override
				protected java.util.Collection<ScssImporter> getAllScssImporters() {
					return createImporterSet(super.getAllScssImporters());
				}

				@Override
				protected void logString(String message) {
					WatchingCompiler.this.logString(phase.get(), message);
				}
				
			};
		}
		
		return new WicketSourceFileModifier(file, inputRootPath, outputRootPath) {

			@Override
			protected java.util.Collection<ScssImporter> getAllScssImporters() {
				return createImporterSet(super.getAllScssImporters());
			}

			@Override
			protected void logString(String message) {
				WatchingCompiler.this.logString(phase.get(), message);
			}
			
		};
	}
	
	private Collection<ScssImporter> createImporterSet(Collection<ScssImporter> list) {
		if(scssImportRoot != null) {
			for(File root: scssImportRoot.get()) {
				list.add(createImporterSet(root, list));
			}
		}
		return list;
	}
	
	private Collection<ScssImporter> createImporterSetRelativeTo(Path p, Collection<ScssImporter> list) {
		list.add(createImporterSet(inputRootPath.resolve(p).toAbsolutePath().getParent().toFile(), list));
		return list;
	}
	
	private ScssImporter createImporterSet(File root, Collection<ScssImporter> list) {
		return new FilePathScssImportResolver(root.toPath());
	}
	
	protected void logString(Phase p, String message) {
		System.out.println(message);
	}
	
}
