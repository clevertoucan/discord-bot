package model;

import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Joshua Owens on 1/30/2017.
 */
public class ListenerImpl extends ListenerAdapter {
    private Logger logger = Logger.getLogger("bot");

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        if (event.isFromType(ChannelType.PRIVATE)){
            PrivateChannel channel = event.getPrivateChannel();
            User author = event.getAuthor();
            Message message = event.getMessage();
            System.out.println(String.format("[PM] %s: %s\n", author.getName(),
                    message.getContent()));

            RestAction<Message> msg = channel.sendMessage(String.format("Hello, %s!",author.getName()));
        }
        else
        {
            logger.log(Level.INFO,String.format("[%s][%s] %s: %s\n", event.getGuild().getName(),
                    event.getTextChannel().getName(), event.getMember().getEffectiveName(),
                    event.getMessage().getContent()));
        }
    }
}
