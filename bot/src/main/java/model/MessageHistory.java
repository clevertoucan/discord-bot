package model;

import net.dv8tion.jda.core.entities.Message;

import java.util.LinkedList;

public class MessageHistory {

    LinkedList<Message> messages;

    public MessageHistory(){
        messages = new LinkedList<>();
    }

    public void addMessage(Message m){
        messages.add(m);
    }

    public void clearMessageHistory(){
        for(Message message : messages){
            message.delete().reason("Cleaning Bot Command").complete();
        }
    }

}
