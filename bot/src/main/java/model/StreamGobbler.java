package model;

import net.dv8tion.jda.core.entities.Message;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class StreamGobbler implements Runnable {
    private InputStream inputStream;
    private Consumer<String> consumer;
    private Context context;
    private StringBuilder reply = new StringBuilder();

    public StreamGobbler(InputStream inputStream, Context context) {
        this.context = context;
        this.inputStream = inputStream;
        reply.append("```");
        consumer = s -> reply.append(s).append("\n");
    }

    @Override
    public void run() {
        new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        reply.append("```");
        context.getChannel().sendMessage(reply.toString()).complete();
    }
}
