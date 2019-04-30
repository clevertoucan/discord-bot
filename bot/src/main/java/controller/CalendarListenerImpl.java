package controller;

import model.CalendarEvent;
import model.GuildCalendar;
import model.MessageHandler;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class CalendarListenerImpl extends ListenerAdapter {
    private String cmdPrefix = "!";
    private MessageChannel out;
    private GuildCalendar calendar = new GuildCalendar();
    private String eventCreateDateFlag = null;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        StringBuilder reply = new StringBuilder();
        Message message = event.getMessage();
        String messageContent = message.getContent();
        boolean isCommand = Pattern.compile("\\Q" + cmdPrefix + "\\E.+").matcher(messageContent).matches();

        if(isCommand){
            messageContent = messageContent.substring(1);
            String[] args = messageContent.split(" ");

            switch (args[0]) {

                //Handles all create-related commands (create event)
                case "create":
                    String[] createArgs = {"event"};
                    if(args.length < 2){
                        argsMsg(createArgs, event.getChannel());
                    } else {
                        if ("event".equals(args[1])) {
                            if (args.length < 3) {
                                reply.append("Missing event name. Proper syntax is `")
                                        .append(cmdPrefix)
                                        .append("create event <eventName>");
                            } else {
                                StringBuilder x = new StringBuilder();
                                for (int i = 2; i < args.length; i++) {
                                    if (i == args.length - 1) {
                                        x.append(args[i]);
                                    } else {
                                        x.append(args[i]).append(" ");
                                    }
                                }
                                CalendarEvent e = new CalendarEvent(message.getAuthor(), x.toString());
                                calendar.add(e);
                                eventCreateDateFlag = e.getName();
                                reply.append("Please input a start time for the event in YYYY-MM-DD-hh:mm format");
                            }
                        }
                    }
                    break;
                //Handles all settings
                case "set":
                    String[] setArgs = {"commandstring"};
                    if(args.length < 2){
                        argsMsg(setArgs, event.getChannel());
                    } else {
                        switch (args[1]){
                            case "commandstring":
                                if(args.length < 3){
                                    reply.append("Missing command string. Proper syntax is `")
                                            .append(cmdPrefix)
                                            .append("set commandstring <newCmdString>");
                                }else {
                                    StringBuilder x = new StringBuilder();
                                    for (int i = 2; i < args.length; i++) {
                                        if (i == args.length - 1) {
                                            x.append(args[i]);
                                        } else {
                                            x.append(args[i]).append(" ");
                                        }
                                    }
                                    cmdPrefix = x.toString();
                                    reply.append("Command String now set to `").append(cmdPrefix).append("`");
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    break;


                default:

                    break;

            }

        } else if(eventCreateDateFlag != null){
            try{
                Date d = new SimpleDateFormat("yyyy-MM-dd-hh:mm").parse(messageContent);
                if(d != null){
                    calendar.get(eventCreateDateFlag).setStart(d);
                }
            } catch (ParseException e){
                out.sendMessage("Parsing error: null message (shouldn't be possible?)");
            }

        }
    }

    void argsMsg(String[] requiredArgs, MessageChannel c){
        StringBuilder possibleArgs = new StringBuilder();
        for(String s : requiredArgs){
            possibleArgs.append("\t").append(s).append("\n");
        }
        c.sendMessage(
                "Missing required arguments\n" +
                        "Possible arguments:\n" +
                        possibleArgs).queue();
    }
}
