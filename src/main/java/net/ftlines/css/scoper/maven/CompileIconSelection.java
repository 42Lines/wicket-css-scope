package net.ftlines.css.scoper.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@Mojo(name = "compile-icons", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class CompileIconSelection extends AbstractMojo {

	@Parameter(required = true)
	public File outputMixinsFile;

	@Parameter(required = true)
	public File outputIconsFile;
	
	@Parameter(required = true)
	public File inputFile;
	
	@Parameter(required = true)
	public File inputMixinsTemplateFilePath;
	
	@Parameter(required = true)
	public File inputIconsTemplateFilePath;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try(FileWriter writer = new FileWriter(outputMixinsFile)) {
			IOUtils.write(renderMixinsTemplate(), writer);
			writer.flush();
		} catch (Exception e) {
			throw new MojoExecutionException("Error compiling icons", e);
		}
		
		try(FileWriter writer = new FileWriter(outputIconsFile)) {
			IOUtils.write(renderIconsTemplate(), writer);
			writer.flush();
		} catch (Exception e) {
			throw new MojoExecutionException("Error compiling icons", e);
		}
	}
	
	private List<Pair<String, Integer>> getIcons() throws Exception {
		return getIcons(inputFile);
	}
	
	protected static List<Pair<String, Integer>> getIcons(File inputFile) throws Exception {
		try (FileReader reader = new FileReader(inputFile)) {
			JsonElement elem = new Gson().fromJson(reader, JsonElement.class);
			JsonArray iconArray = elem.getAsJsonObject().get("icons").getAsJsonArray();
			List<Pair<String, Integer>> list = new ArrayList<Pair<String,Integer>>();
			
			Consumer<JsonElement> sink = (z) -> {
				convertIcon(z, list::add);
			};
			
			iconArray.asList().stream().forEach(sink);
			return list;
		}
	}
	
	private static void convertIcon(JsonElement elem, Consumer<Pair<String, Integer>> sink) {
		String name = elem.getAsJsonObject().get("properties").getAsJsonObject().get("name").getAsString();
		int code = elem.getAsJsonObject().get("properties").getAsJsonObject().get("code").getAsInt();
		for(String n: name.split(",")) {
			sink.accept(Pair.of(n.strip(), code));
		}
	}
	
	private static String renderSassMixins(List<Pair<String, Integer>> list) {
		StringBuffer buffer = new StringBuffer();
		
		list.forEach(icon -> {
			buffer.append("@mixin icon-" + icon.getLeft() + "-content {content:\"\\" + Integer.toHexString(icon.getRight()) + "\";}\n");
		});

		return buffer.toString();
	}
	
	private static String renderSassIcons(List<Pair<String, Integer>> list) {
		StringBuffer buffer = new StringBuffer();
		
		list.forEach(icon -> {
			buffer.append(".icon-" + icon.getLeft() + ":before{@include icon-"+icon.getLeft()+"-content;}\n\n");
		});

		return buffer.toString();
	}
	
	private String renderMixinsTemplate() throws Exception {
		try (FileReader reader = new FileReader(inputMixinsTemplateFilePath)) {
			return IOUtils.toString(reader).replace("{{$HASH$}}",  md5ToNumeric(getHash(inputFile))).replace("{{$ICONS$}}", renderSassMixins(getIcons()));
		}
	}
	
	private String renderIconsTemplate() throws Exception {
		try (FileReader reader = new FileReader(inputIconsTemplateFilePath)) {
			return IOUtils.toString(reader).replace("{{$HASH$}}",  md5ToNumeric(getHash(inputFile))).replace("{{$ICONS$}}", renderSassIcons(getIcons()));
		}
	}
	
	private static String md5ToNumeric(String md5Hash) {
        return new BigInteger(md5Hash, 16).toString();
    }
	
	private static String getHash(File file) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(file);
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        // Read the file data and update it to the message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        fis.close();

        // Get the hash's bytes
        byte[] bytes = digest.digest();

        // Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        // Return the complete hash
        return sb.toString();
	}
	
//	 private void replaceAllIconsInFile(File file) throws Exception {
//		 String filePath = file.getAbsolutePath();
//	        try(Stream<String> lines = Files.lines(Paths.get(filePath))){
//	            String content = String.join("\n", (Iterable<String>)lines::iterator);
//	            for(Pair<String, Integer> ico: getIcons()) {
//	    			String from = "content:\"\\" + Integer.toHexString(ico.getRight()) + "\";";
//	    			String to = "@include icon-" + ico.getLeft().strip() + "-content;";
//	    			content = content.replace(from, to);
//	    		}
//	            
//	            Files.write(Paths.get(filePath), content.getBytes(), StandardOpenOption.WRITE);
//	        } catch (IOException e) {
//	            e.printStackTrace();
//	        }
//	}

}
