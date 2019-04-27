package controller;

import model.CalendarEvent;
import model.MessageHandler;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.regex.Pattern;

public class CalendarListenerImpl extends ListenerAdapter {
    String cmdPrefix = "!";
    MessageChannel out;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        boolean eventCreateDateFlag, eventCreateTimeFlag;
        Message message = event.getMessage();
        String messageContent = message.getContent();
        boolean isCommand = Pattern.compile("\\Q" + cmdPrefix + "\\E.+").matcher(messageContent).matches();

        if(isCommand){
            messageContent = messageContent.substring(1);
            String[] args = messageContent.split(" ");

            switch (args[0]) {

                case "create":
                    String[] createArgs = {"event"};
                    if(args.length < 2){
                        StringBuilder possibleArgs = new StringBuilder();
                        for(String s : createArgs){
                            possibleArgs.append("\t" + s + "\n");
                        }
                        out.sendMessage(
                                "Missing required arguments\n" +
                                        "Possible arguments:\n" +
                                        possibleArgs);
                    } else {
                        switch (args[1]){
                            case "event":
                                if(args.length < 3){
                                    out.sendMessage("Missing event name. Proper syntax is `"+cmdPrefix+"create event <eventName>");
                                } else {
                                    StringBuilder x = new StringBuilder();
                                    for (int i = 2; i < args.length; i++){
                                        if(i == args.length - 1){
                                            x.append(args[i]);
                                        } else {
                                            x.append(args[i]).append(" ");
                                        }
                                    }
                                    CalendarEvent e = new CalendarEvent(message.getAuthor(), x.toString());
                                    eventCreateDateFlag = true;
                                    out.sendMessage("Please input a day for the event in YYYY-MM-DD format");
                                }
                        }
                    }
                    break;

                default:

                    break;

            }

        } else if(eventCreateDateFlag){

        } else if(event)
    }
}
