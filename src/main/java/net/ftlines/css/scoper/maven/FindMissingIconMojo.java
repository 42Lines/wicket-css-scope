package net.ftlines.css.scoper.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;


@Mojo(name = "find-icon-violations", defaultPhase = LifecyclePhase.VERIFY)
public class FindMissingIconMojo extends AbstractMojo {

	@Parameter(required = true)
	public File inputFile;
	
	@Parameter(required = true, defaultValue = "")
	public FileSet fileset;
	
	@Parameter(required = true, defaultValue = "false")
	public boolean failOnError = false;
	
	private FileSetManager fileSetManager = new FileSetManager();
	
	private List<Pair<String, Integer>> getIcons() throws Exception {
		return CompileIconSelection.getIcons(inputFile);
	} 
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
		
		List<String> iconClasses = getIcons().stream().map(z-> "icon-" + z.getLeft()).collect(Collectors.toList());
		findIconViolations(iconClasses, fileset);
		
		List<String> iconCharacterCodes = getIcons().stream().map(z-> ("\\" + Integer.toHexString(z.getRight()))).collect(Collectors.toList());
		findIconContentViolations(iconCharacterCodes, fileset);
		
		} catch(Exception ex) {
			if(ex instanceof MojoExecutionException) {
				throw (MojoExecutionException)ex;
			}
			
			if(ex instanceof MojoFailureException) {
				throw (MojoFailureException)ex;
			}
			
			getLog().error(ex);
		}
	}
	
	private void findIconContentViolations(List<String> iconCharacterCodes, FileSet fileSet) throws MojoExecutionException {
		for (String f : fileSetManager.getIncludedFiles(fileset)) {
	        try {
	            List<String> lines = Files.readAllLines(Path.of(fileset.getDirectory(), f));

	            for (String line : lines) {
	                if (line.contains("content")) {
	                    String contentCode = extractContentCode(line);
	                    if (contentCode != null && !iconCharacterCodes.contains(contentCode)) {
	                        getLog().error("Found violation in " + f + ": " + line.strip());
	                        if (failOnError) {
	                            throw new MojoExecutionException("Icon violation found in " + f);
	                        }
	                    }
	                }
	            }
	        } catch (IOException e) {
	            getLog().error("Error reading file: " + f, e);
	        }
	    }
	}

	private String extractContentCode(String line) {
	    Pattern pattern = Pattern.compile("content:\\s*\"\\\\([0-9a-fA-F]+)\";");
	    Matcher matcher = pattern.matcher(line);
	    if (matcher.find()) {
	        return "\\" + matcher.group(1);
	    }
	    return null;
	}

	
	private void findIconViolations(List<String> iconClasses, FileSet fileSet) throws IOException, MojoFailureException {
		for (String f : fileSetManager.getIncludedFiles(fileset)) {
			
	        org.jsoup.nodes.Document doc = Jsoup.parse(Files.readString(Path.of(fileset.getDirectory(), f)), "UTF-8");
	        Elements elementsWithIconClass = doc.select("[class^=icon-]");

	        for (org.jsoup.nodes.Element element : elementsWithIconClass) {
	            for(String className: element.className().split(" ")) {
	            	if (className.startsWith("icon-") && !iconClasses.contains(className)) {
	            		getLog().error("Icon violation found in " + f + ": Class " + className + " is not in the allowed list.");
	            		if (failOnError) {
	            			throw new MojoFailureException("Icon violation found: Class " + className + " is not in the allowed list.");
	            		}
	            	}
	            }
	        }
	    }
	}

}
