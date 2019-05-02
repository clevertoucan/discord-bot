package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Handles all save and load operations to allow data to persist outside of memory
 *
 * */


public class Persistence {
    private static Persistence instance;
    private HashMap<String, Serializable> saveData = new HashMap<>();
    File saveFile;
    private Logger logger;

    public static Persistence getInstance() {
        if (instance == null) {
            instance = new Persistence();
        }
        return instance;
    }

    private Persistence() {
        logger = LoggerFactory.getLogger("Persistence");

        try {
            saveFile = new File("savestate");
            if (!saveFile.exists()) {
                saveFile.createNewFile();
            }
            load();
            save();

        } catch (IOException e) {
            logger.error("CRITICAL: Exception in Persistence module", e);
            logger.error("Exiting");
            System.exit(-1);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends  Serializable> T read(Class<T> clazz, String key) {
        Serializable o1 = saveData.get(key);
        T value = null;
        if (o1 != null && o1.getClass() == clazz) {
            value = (T) o1;
        }
        return value;
    }

    public void addObject(String key, Serializable value){
        saveData.put(key, value);
        boolean saved = save();
        if(saved) {
            logger.debug("Saved Object: " + key + " with contents: " + value);
        } else {
            logger.debug("Unable to save Object: " + key + " with contents: " + value);
        }
    }

    private boolean save(){
        try{
            FileOutputStream fout = new FileOutputStream(saveFile);
            ObjectOutputStream out = new ObjectOutputStream(fout);
            out.writeObject(saveData);
            out.close();
            fout.close();
            return true;
        } catch(IOException e){
            logger.warn("Unable to write savedata");
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void load(){
        try {
            FileInputStream fin = new FileInputStream(saveFile);
            ObjectInputStream in = new ObjectInputStream(fin);
            Object o = in.readObject();
            in.close();
            fin.close();
            if(o.getClass() == saveData.getClass()){
                saveData = (HashMap<String, Serializable>) o;
                for(String s : saveData.keySet());
                for(Serializable s : saveData.values());
                logger.debug("Loaded savedata: " + saveData);
            }
        } catch (ClassNotFoundException | ClassCastException | IOException e){
            logger.warn("Unable to read savedata");
        }
    }

}

