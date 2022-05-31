package fexcraft.tmt.slim;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import net.minecraftforge.fml.common.Loader;

public class ModelPool {

	public static ModelPoolEntry addFile(String file, Class<?> modelClass){
		ModelPoolEntry entry = null;
		if(modelMap.containsKey(file)){
			entry = modelMap.get(file);
			return entry;
		}
		try{
			entry = (ModelPoolEntry)modelClass.newInstance();
		}
		catch(Exception e){
			System.out.println("A new " + entry.getClass().getName() + " could not be initialized.");
			System.out.println(e.getMessage());
			return null;
		}
		File modelFile = entry.checkValidPath(file.substring(5));
		if(modelFile == null || !modelFile.exists()){
			System.out.println("The model with the name " + file + " does not exist.");
			return null;
		}
		entry.textures = new HashMap<String, TextureGroup>();
		entry.name = file;
		entry.setTextureGroup("0");
		entry.getModel(modelFile);
		modelMap.put(file, entry);
		return entry;
	}

	public static ModelPoolEntry addFileF(String file, Class<?> modelClass) throws IOException{
		ModelPoolEntry entry = null;
		if(modelMap.containsKey(file)){
			entry = modelMap.get(file);
			return entry;
		}
		try{
			entry = (ModelPoolEntry)modelClass.newInstance();
		}
		catch(Exception e){
			System.out.println("A new " + entry.getClass().getName() + " could not be initialized.");
			System.out.println(e.getMessage());
			return null;
		}
		File modelFile = null;
		InputStream in = entry.getClass().getResourceAsStream("/assets/" + file + ".obj");
		File tempfile = File.createTempFile(file, ".obj");
		FileOutputStream out = new FileOutputStream(tempfile);
		tempfile.deleteOnExit();
		IOUtils.copy(in, out);
		System.out.println("RENDER: " + tempfile.getPath().toString());
		modelFile = tempfile;
		if(modelFile == null || !modelFile.exists()){
			System.out.println("The model with the name " + file + " does not exist.");
			return null;
		}
		entry.textures = new HashMap<String, TextureGroup>();
		entry.name = file;
		entry.setTextureGroup("0");
		entry.getModel(modelFile);
		modelMap.put(file, entry);
		return entry;
	}

	private static Map<String, ModelPoolEntry> modelMap = new HashMap<String, ModelPoolEntry>();
	private static String[] resourceDir = new String[] {
			"minecraft/resources/models/",
			"minecraft/resources/mod/models/"
	};
	public static final Class<ModelPoolObjEntry> OBJ = ModelPoolObjEntry.class;

}
