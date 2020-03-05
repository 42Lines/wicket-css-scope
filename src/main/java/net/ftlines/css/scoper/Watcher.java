package net.ftlines.css.scoper;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.sun.nio.file.SensitivityWatchEventModifier;;

public class Watcher implements Runnable {

	private Path inputRootPath;
	private WatchService watchService;

	private Function<Path, Boolean> isWatchableFunction;
	private Consumer<Path> processingFunction;
	private Consumer<Phase> phaseChangeFunction;
	
	public enum Phase {
		STARTUP, WATCHING, REBUILDING
	}
	
	private Phase phase = Phase.STARTUP;
	private Function<Path, Boolean> isFullRecompileTriggerFunction;

	public Watcher(Path inputRoot, Function<Path, Boolean> isWatchableFunction, Consumer<Path> processingFunction)
		throws Exception {
		this(inputRoot, isWatchableFunction, (p) -> false, (c) ->{}, processingFunction);
	}
	
	public Watcher(Path inputRoot, 
		Function<Path, Boolean> isWatchableFunction, 
		Function<Path, Boolean> isFullRecompileTriggerFunction, 
		Consumer<Phase> phaseChangeFunction, 
		Consumer<Path> processingFunction)
		throws Exception {
		inputRootPath = inputRoot;
		this.isWatchableFunction = isWatchableFunction;
		this.isFullRecompileTriggerFunction = isFullRecompileTriggerFunction;
		this.phaseChangeFunction = phaseChangeFunction;
		this.processingFunction = processingFunction;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(1000);
			start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void start() throws Exception {

		phase = Phase.STARTUP;
		phaseChangeFunction.accept(phase);
		try {
			reProcessAllFiles();
		} catch(Exception e) {
			e.printStackTrace();
		}
		Thread.sleep(500);

		watchService = FileSystems.getDefault().newWatchService();
		registerRecursive(watchService, inputRootPath);

		phase = Phase.WATCHING;
		phaseChangeFunction.accept(phase);
		
		WatchKey key;
		while ((key = watchService.take()) != null) {
			for (WatchEvent<?> event : key.pollEvents()) {

				Path dir = (Path) key.watchable();
				Path workingDirPath = dir.resolve((Path) event.context());

				Path p = inputRootPath.relativize(workingDirPath).normalize();
				try {
					onChange(p);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			key.reset();
		}
	}
	
	private void onChange(Path p) throws IOException {
		if (isWatchableFunction.apply(p)) {
			processingFunction.accept(p);
		}
		
		if(isFullRecompileTriggerFunction.apply(p)) {
			phase = Phase.REBUILDING;
			phaseChangeFunction.accept(phase);
			
			reProcessAllFiles();
			
			phase = Phase.WATCHING;
			phaseChangeFunction.accept(phase);
		}
	}

	private void reProcessAllFiles() throws IOException {
		Files.walk(inputRootPath, FileVisitOption.FOLLOW_LINKS).filter(p -> isWatchableFunction.apply(p))
			.map(inputRootPath::relativize).forEach(processingFunction);
	}

	private void registerRecursive(WatchService watchService, final Path root) throws IOException {
		// register all subfolders
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (pathContainsWatchable(dir)) {
					dir.register(watchService,
						new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
							StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY },
						SensitivityWatchEventModifier.HIGH);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private boolean pathContainsWatchable(Path dir) {
		try (Stream<Path> stream = Files.walk(dir)) {
			return stream.filter(file -> !Files.isDirectory(file)).filter(p -> isWatchableFunction.apply(p) || isFullRecompileTriggerFunction.apply(p)).findAny()
				.isPresent();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class SortedProperties extends Properties {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3977772714334230810L;

		@Override
		public Set<java.util.Map.Entry<Object, Object>> entrySet() {
			Set<java.util.Map.Entry<Object, Object>> set = new TreeSet<java.util.Map.Entry<Object, Object>>(
				new Comparator<java.util.Map.Entry<Object, Object>>() {

					@Override
					public int compare(java.util.Map.Entry<Object, Object> o1, java.util.Map.Entry<Object, Object> o2) {
						return o1.getKey().toString().compareTo(o2.getKey().toString());
					}
				});
			set.addAll(super.entrySet());
			return set;
		}

	}

	public static void startAsDaemon(Path inputRoot, Function<Path, Boolean> isWatchableFunction,
		Function<Path, Boolean> isFullRebuildTrigger, Consumer<Phase> phaseChangeFunction, Consumer<Path> processingFunction) throws Exception {
		Thread watcher = new Thread(new Watcher(inputRoot, isWatchableFunction, isFullRebuildTrigger, phaseChangeFunction, processingFunction));
		watcher.setDaemon(true);
		watcher.start();
	}
	
	@SafeVarargs
	public static Function<Path, Boolean> allOf(Function<Path, Boolean> ... funcs) {
		return (p) -> {
			boolean flag = true;
			for (Function<Path, Boolean> ext : funcs) {
				flag &= ext.apply(p);
			}
			return flag;
		};
	}

	public static Function<Path, Boolean> isFileEndsWithFunction(String... extensions) {
		return (p) -> {
			for (String ext : extensions) {
				if (p.toString().toLowerCase().endsWith(ext.toLowerCase())) {
					return true;
				}
			}
			return false;
		};
	}
	
	public static Function<Path, Boolean> isFileNameStartsWithFunction(String... prefix) {
		return (p) -> {
			for (String ext : prefix) {
				if (p.getFileName().toString().toLowerCase().startsWith(ext.toLowerCase())) {
					return true;
				}
			}
			return false;
		};
	}

}
