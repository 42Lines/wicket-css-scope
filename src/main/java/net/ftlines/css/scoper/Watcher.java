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
import java.nio.file.Watchable;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.sun.nio.file.SensitivityWatchEventModifier;;

public class Watcher implements Runnable {

	private Path inputRootPath;
	private Path outputRootPath;

	private Function<Path, Boolean> isWatchableFunction;
	private Consumer<Path> processingFunction;
	private Consumer<Phase> phaseChangeFunction;

	public enum Phase {
		STARTUP, WATCHING, REBUILDING, SHUTDOWN
	}

	private Phase phase = Phase.STARTUP;
	private Function<Path, Boolean> isFullRecompileTriggerFunction;
	private Function<Path, Boolean> isRebuildFunction;

	public Watcher(Path inputRoot, Path outputRootPath, 
			Function<Path, Boolean> isWatchableFunction,
			Function<Path, Boolean> isFullRecompileTriggerFunction, 
			Function<Path, Boolean> isRebuildFunction,
			Consumer<Phase> phaseChangeFunction,
			Consumer<Path> processingFunction) throws Exception {
		this.inputRootPath = inputRoot;
		this.outputRootPath = outputRootPath;
		this.isWatchableFunction = isWatchableFunction;
		this.isFullRecompileTriggerFunction = isFullRecompileTriggerFunction;
		this.isRebuildFunction = isRebuildFunction;
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
		while (phase != Phase.SHUTDOWN) {
			phase = Phase.STARTUP;
			phaseChangeFunction.accept(phase);
			try {
				reProcessAllFiles();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(500);

			try(WatchService watchService = FileSystems.getDefault().newWatchService()) {
				List<WatchKey> registeredKeys = new ArrayList<>();
				registeredKeys.addAll(registerRecursiveSourceWatchables(watchService, inputRootPath));
				registeredKeys.addAll(registerRecursiveTargetWatchables(watchService, outputRootPath));

				registeredKeys.add(outputRootPath.register(watchService,
						new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
								StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY },
						SensitivityWatchEventModifier.HIGH));

				phase = Phase.WATCHING;
				phaseChangeFunction.accept(phase);

				WatchKey key;
				while ((key = watchService.take()) != null && phase == Phase.WATCHING) {

					if(!key.isValid()) {
						System.out.println("Key '" + key.watchable() + "' is invalid .. rebuilding.");
						break;
					}

					processEvents(key.watchable(), key.pollEvents());
					key.reset();
				}

				//Cleanup registered keys
				registeredKeys.forEach(z -> z.cancel());
				registeredKeys.clear();
			}
			
			Thread.sleep(2000);
		}
	}

	private void processEvents(Watchable watchable, List<WatchEvent<?>> events) {
		for (WatchEvent<?> event : events) {
			Path dir = (Path) watchable;
			Path workingDirPath = dir.resolve((Path) event.context());
			if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE 
					&& isChildPath(outputRootPath, workingDirPath)
					&& isRebuildFunction.apply(workingDirPath) ) {
					Path p = outputRootPath.relativize(workingDirPath).normalize();
                    System.out.println(p + " was deleted! triggering full rebuild");
                    phase = Phase.REBUILDING;
                    phaseChangeFunction.accept(phase);
                return;
		    } else if(isChildPath(inputRootPath, workingDirPath)) {
		    	Path p = inputRootPath.relativize(workingDirPath).normalize();
				try {
					onChange(p);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Exception .. triggering full rebuild");
                    phase = Phase.REBUILDING;
                    phaseChangeFunction.accept(phase);
				}
			} 
		}
	}

	private void onChange(Path p) throws IOException {
		if (isWatchableFunction.apply(p)) {
			processingFunction.accept(p);
		}

		if (isFullRecompileTriggerFunction.apply(p)) {
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

	private List<WatchKey> registerRecursive(WatchService watchService, final Path root, Function<Path, Boolean> isWatchable) throws IOException {
		List<WatchKey> registeredKeys = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (isWatchable.apply(dir)) {
					registeredKeys.add(dir.register(watchService,
							new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
									StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY },
							SensitivityWatchEventModifier.HIGH));
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
		return registeredKeys;
	}
	
	private List<WatchKey> registerRecursiveSourceWatchables(WatchService watchService, final Path root) throws IOException {
		return registerRecursive(watchService, root, this::pathContainsSourceWatchable);
	}
	
	private List<WatchKey> registerRecursiveTargetWatchables(WatchService watchService, final Path root) throws IOException {
		return registerRecursive(watchService, root, this::pathContainsTargetWatchable);
	}

	private boolean pathContainsSourceWatchable(Path dir) {
		return pathContainsWatchable(dir, p -> isWatchableFunction.apply(p) || isFullRecompileTriggerFunction.apply(p));
	}
	
	private boolean pathContainsTargetWatchable(Path dir) {
		return pathContainsWatchable(dir, p -> isRebuildFunction.apply(p));
	}
	
	private boolean pathContainsWatchable(Path dir, Predicate<Path> filter) {
		try (Stream<Path> stream = Files.walk(dir)) {
			return stream.filter(file -> !Files.isDirectory(file))
					.filter(filter).findAny()
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
						public int compare(java.util.Map.Entry<Object, Object> o1,
								java.util.Map.Entry<Object, Object> o2) {
							return o1.getKey().toString().compareTo(o2.getKey().toString());
						}
					});
			set.addAll(super.entrySet());
			return set;
		}

	}

	@SafeVarargs
	public static Function<Path, Boolean> allOf(Function<Path, Boolean>... funcs) {
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
	
	private static boolean isChildPath(Path parent, Path child) {
	    return child.toAbsolutePath().startsWith(parent.toAbsolutePath());
	}


	public static void startAsDaemon(Path inputRoot, Path outputRootPath, Function<Path, Boolean> isWatchableFunction,
		Function<Path, Boolean> isFullRebuildTrigger, Function<Path, Boolean> isRebuildFunction, Consumer<Phase> phaseChangeFunction, Consumer<Path> processingFunction) throws Exception {
		Thread watcher = new Thread(new Watcher(inputRoot, outputRootPath, isWatchableFunction, isFullRebuildTrigger, 
				isRebuildFunction, phaseChangeFunction, processingFunction));
		watcher.setDaemon(true);
		watcher.start();
	}
	
}
