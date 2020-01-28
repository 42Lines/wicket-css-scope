package net.ftlines.css.scoper;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.stream.Stream;

import com.sun.nio.file.SensitivityWatchEventModifier;;

public class Watcher implements Runnable {

	private Path inputRootPath;
	private Path outputRootPath;
	private WatchService watchService;

	public Watcher(Path inputRoot, Path outputRoot) throws Exception {
		inputRootPath = inputRoot;
		outputRootPath = outputRoot;
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

		Files.walk(inputRootPath, FileVisitOption.FOLLOW_LINKS).filter(this::parseableResource)
			.map(inputRootPath::relativize).forEach(this::processPath);
		Thread.sleep(500);

		watchService = FileSystems.getDefault().newWatchService();
		registerRecursive(watchService, inputRootPath);

		WatchKey key;
		while ((key = watchService.take()) != null) {
			for (WatchEvent<?> event : key.pollEvents()) {

				Path dir = (Path) key.watchable();
				Path workingDirPath = dir.resolve((Path) event.context());

				Path p = inputRootPath.relativize(workingDirPath).normalize();

				if (parseableResource(p)) {
					processPath(p);
				}
			}
			key.reset();
		}
	}

	protected boolean parseableResource(Path path) {
		return path.toString().toLowerCase().endsWith(".html");
	}

	private void processPath(Path file) {
		new WicketSourceFileModifier(file, inputRootPath, outputRootPath).process();
	}

	private static void registerRecursive(WatchService watchService, final Path root) throws IOException {
		// register all subfolders
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				// Only if contains .html files
				if (pathContainsHtml(dir)) {
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

	private static boolean pathContainsHtml(Path dir) {
		try (Stream<Path> stream = Files.walk(dir)) {
			return stream.filter(file -> !Files.isDirectory(file))
				.filter(file -> file.toString().toLowerCase().endsWith(".html")).findAny().isPresent();
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

	public static void startAsDaemon(Path inputRoot, Path outputRoot) throws Exception {
		Thread watcher = new Thread(new Watcher(inputRoot, outputRoot));
		watcher.setDaemon(true);
		watcher.start();
	}

	public static void main(String[] args) throws Exception {
		new Watcher(Paths.get(args[0]), Paths.get(args[1])).start();
	}

}
