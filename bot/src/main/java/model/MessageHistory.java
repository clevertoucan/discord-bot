package model;

import net.dv8tion.jda.core.entities.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class MessageHistory {

    Logger logger = LoggerFactory.getLogger("MessageHistory");
    LinkedList<Message> messages;

    public MessageHistory(){
        messages = new LinkedList<>();
    }

    public void addMessage(Message m){
        messages.add(m);
    }

    public void clearMessageHistory(Context c){
        c.getChannel().purgeMessages(messages);
        logger.info("Deleting messages: " + messages);
    }

}
