package net.ftlines.css.scoper.maven;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import net.ftlines.css.scoper.Watcher;
import net.ftlines.css.scoper.WatchingCompiler;

/**
 * Watch for changes in inputPath, then compile scss files to outputPath using includePaths
 */
@Mojo(name = "watch")
public class WatchMojo extends AbstractCssScopeMojo {

	@Parameter(required = false)
	public String onChangeWebhook;

	public void execute() throws MojoExecutionException, MojoFailureException {
		Path outputRootPath = this.outputPath.toPath();
		Path inputRootPath = this.inputPath.toPath();

		WatchingCompiler compiler = new WatchingCompiler(inputRootPath, outputRootPath,
			this::getScssImportRoots , singleFileOutputPath.toPath()) {
			@Override
			protected boolean isDebugMode() {
				return true;
			}
			
			@Override
			protected void onChange(Path file) {
				super.onChange(file);
				if(onChangeWebhook != null && onChangeWebhook.strip().length() > 1) {
					try {
						callWebhook(onChangeWebhook, file.toString());
					} catch (Exception e) {
						getLog().error(e);
					}
				}
			}
		};

		try {
			new Watcher(inputRootPath, outputRootPath,
				Watcher.isFileEndsWithFunction(".html", ".css", ".js", ".scss"),
				Watcher.allOf(Watcher.isFileEndsWithFunction(".scss"), Watcher.isFileNameStartsWithFunction("_")),
				Watcher.isFileEndsWithFunction(".css"),
				compiler::setPhase, 
				compiler::process
			).start();
		} catch (Exception e) {
			throw new MojoFailureException(e.getMessage(), e);
		}
	}
	
	private Collection<File> getScssImportRoots() {
		return Arrays.asList(scssImportRoot);
	}
	
	//TODO Add threads so all work can run
	//TODO add a onchangewebhook trigger
	//TODO add something that looks for targets and sees if a clean happened

	private static void callWebhook(String webhookUrl, String payload) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        try {
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Accept", "application/json");

            try (OutputStream os = httpURLConnection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

        } finally {
            httpURLConnection.disconnect();
        }
    }

}
