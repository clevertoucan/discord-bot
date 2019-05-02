package model;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class Session {
    private Signature signature;
    private Date creationTime;
    private HashMap<String, Serializable> sessionData;

    public Session(@NotNull User user, MessageChannel channel, Guild guild) {
        signature = new Signature(user.getId(), channel.getId(), guild.getId());
        creationTime = new Date();
        sessionData = new HashMap<>();
    }

    public Session(Signature s){
        signature = s;
        creationTime = new Date();
        sessionData = new HashMap<>();
    }

    public Date getCreationTime(){
        return creationTime;
    }

    public void reset(){
        sessionData = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getSessionData(Class<T> clazz, String key){
        Serializable s = sessionData.get(key);
        if(s != null && s.getClass() == clazz){
            return (T)s;
        }
        else return null;
    }

    public void addSessionData(String key, Serializable data){
        sessionData.put(key, data);
    }

    public void removeSessionData(String key){
        sessionData.remove(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(signature, session.signature) &&
                Objects.equals(creationTime, session.creationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signature, creationTime);
    }
}
