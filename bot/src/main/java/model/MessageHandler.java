package model;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Created by scary on 4/27/2017.
 */
public class MessageHandler extends Handler {
    private Formatter formatter = new SimpleFormatter();
    private MessageChannel channel;

    public MessageHandler(MessageChannel channel){
        this.channel = channel;
    }

    @Override
    public void publish(LogRecord record) {
        channel.sendMessage(formatter.format(record)).queue();
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }

    public void setFormatter(Formatter formatter){
        this.formatter = formatter;
    }
}
