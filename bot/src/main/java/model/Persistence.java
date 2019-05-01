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
    private FileOutputStream fout;
    private ObjectOutputStream out;
    private FileInputStream fin;
    private ObjectInputStream in;
    private Logger logger;

    public static Persistence getInstance(){
        if(instance == null){
            instance = new Persistence();
        }
        return instance;
    }

    public void updateObject(String key, Serializable value){
        saveData.put(key, value);
        save();
    }

    @SuppressWarnings("unchecked")
    public <T extends  Serializable> T read(Class<T> clazz, String key) {
        load();
        Serializable o1 = saveData.get(key);
        T value = null;
        if (o1.getClass() == clazz) {
            value = (T) o1;
        }
        return value;
    }

    public void addObject(String key, Serializable value){
        saveData.put(key, value);
        save();
    }

    private void save(){
        try{
            out.writeObject(saveData);
        } catch(IOException e){
            logger.warn("Unable to write savedata");
        }
    }

    @SuppressWarnings("unchecked")
    private void load(){
        try {
            Object o = in.readObject();
            if(o.getClass() == saveData.getClass()){
                saveData = (HashMap<String, Serializable>) in.readObject();
                for(String s : saveData.keySet());
                for(Serializable s : saveData.values());
            }
        } catch (ClassNotFoundException | ClassCastException | IOException e){
            logger.warn("Unable to read savedata");
        }
    }

    private Persistence() {
        logger = LoggerFactory.getLogger("Persistence");

        try {
            File f = new File("savestate");
            if(!f.exists()) {
                f.createNewFile();
            }

            fin = new FileInputStream(f);
            in = new ObjectInputStream(fin);

            load();

            fout = new FileOutputStream(f);
            out = new ObjectOutputStream(fout);

            save();

        } catch(IOException e){
                logger.error("CRITICAL: Exception in Persistence module");
                logger.error(e.getMessage());
                logger.error("Exiting");
                System.exit(-1);
            }
        }

    }

