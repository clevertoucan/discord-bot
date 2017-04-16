package model;

import com.sun.xml.internal.ws.developer.Serialization;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by scary on 4/16/2017.
 */

public class Data implements Serializable{
    Guild targetGuild;
    HashMap<Guild, Role> roleMap;
    HashMap<Role, Set<Channel>> associations;

    public Data(Guild targetGuild, HashMap<Guild, Role> roleMap) {
        this.targetGuild = targetGuild;
        this.roleMap = roleMap;
    }

    public boolean saveData(){
        return false;
    }

    public boolean loadData(){
        return false;
    }

}
