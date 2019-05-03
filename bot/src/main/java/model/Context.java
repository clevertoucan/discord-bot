package model;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class Context implements Serializable {
    private Signature signature;
    private StringBuilder reply;
    private JDA jda;
    private HashMap<String, Serializable> data;


    public String commandString, dateFormatString, eventName, pingMessageContent, description, location;
    public CalendarEvent event;
    public CalendarEvent[] events;
    public Boolean rsvpGoing;
    public Date date;

    public Context(Signature signature, StringBuilder reply, JDA jda) {
        this.signature = signature;
        this.reply = reply;
        this.jda = jda;
        data = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getData(Class<T> clazz, String key){
        Serializable s = data.get(key);
        if(s != null && s.getClass() == clazz){
            return (T)s;
        }
        else return null;
    }

    public void addData(String key, Serializable value){
        data.put(key, value);
    }

    public JDA getJDA() {
        return jda;
    }

    public User getUser(){
        return getJDA().getUserById(getUserID());
    }

    public Guild getGuild(){
        return  getJDA().getGuildById(getGuildID());
    }

    public TextChannel getChannel(){
        return getJDA().getTextChannelById(getChannelID());
    }

    public String getUserID() {
        return signature.userID;
    }

    public String getGuildID() {
        return signature.guildID;
    }

    public  String getChannelID(){
        return signature.channelID;
    }

    public StringBuilder getReply() {
        return reply;
    }

    public void setReply(StringBuilder reply) {
        this.reply = reply;
    }

    public Signature getSignature(){
        return  signature;
    }
}
