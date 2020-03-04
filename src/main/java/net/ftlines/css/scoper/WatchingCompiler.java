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
		phase.set(p);
		writeSingleStyleSheet();
	}
	
	public Phase getPhase() {
		return phase.get();
	}
	
	public void process(Path file) {
		createModifier(file).setDebugMode(isDebugMode()).process();
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
	
	protected AbstractSourceFileModifier createModifier(Path file) {

		if(singleFileOutputPath != null) {
			return new WicketSingleStyleSourceFileModifier(file, inputRootPath, outputRootPath, styles) {

				@Override
				protected java.util.Collection<io.bit3.jsass.importer.Importer> getAllScssImporters() {
					Collection<Importer> list = super.getAllScssImporters();
					if(scssImportRoot != null) {
						for(File root: scssImportRoot.get()) {
							list.add(new FilePathScssImportResolver(root.toPath()));
						}
					}
					return list;
				}

				@Override
				protected void logString(String message) {
					WatchingCompiler.this.logString(message);
				}
				
			};
		}
		
		return new WicketSourceFileModifier(file, inputRootPath, outputRootPath) {

			@Override
			protected java.util.Collection<io.bit3.jsass.importer.Importer> getAllScssImporters() {
				Collection<Importer> list = super.getAllScssImporters();
				if(scssImportRoot != null) {
					for(File root: scssImportRoot.get()) {
						list.add(new FilePathScssImportResolver(root.toPath()));
					}
				}
				return list;
			}

			@Override
			protected void logString(String message) {
				WatchingCompiler.this.logString(message);
			}
			
		};
	}
	
	protected void logString(String message) {
		System.out.println(message);
	}
	
}
