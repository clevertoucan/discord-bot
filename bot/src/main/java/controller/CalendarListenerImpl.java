package controller;

import model.CalendarEvent;
import model.GuildCalendar;
import model.MessageHandler;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarListenerImpl extends ListenerAdapter {
    private String cmdPrefix = ";";
    private MessageChannel out;
    private GuildCalendar calendar = new GuildCalendar();
    private CalendarEvent eventCreateDateFlag = null;
    private CalendarEvent[] eventRemoveFlag = null;
    private CalendarEvent[] rsvpGoingFlag= null, rsvpStayingFlag = null, viewEventFlag = null, pingEventFlag = null;
    private Logger logger = LoggerFactory.getLogger("CalendarEventListener");

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor() != event.getJDA().getSelfUser()) {
            StringBuilder reply = new StringBuilder();
            Message message = event.getMessage();
            String messageContent = message.getContentDisplay();
            boolean isCommand = Pattern.compile("\\Q" + cmdPrefix + "\\E.+").matcher(messageContent).matches();

            if (isCommand) {
                messageContent = messageContent.substring(cmdPrefix.length());
                String[] args = messageContent.split(" ");

                switch (args[0]) {

                    //Handles all create-related commands (create event)
                    case "create":
                        String[] createArgs = {"event"};
                        if (args.length < 2) {
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
                                    eventCreateDateFlag = new CalendarEvent(message.getAuthor(), x.toString());
                                    reply.append("Please input a start time for the event in YYYY-MM-DD-hh:mm format");
                                }
                            } else {
                                argsMsg(createArgs, event.getChannel());
                            }
                        }
                        break;
                    //Handles all settings
                    case "set":
                        String[] setArgs = {"commandstring"};
                        if (args.length < 2) {
                            argsMsg(setArgs, event.getChannel());
                        } else {
                            if ("commandstring".equals(args[1])) {
                                if (args.length < 3) {
                                    reply.append("Missing command string. Proper syntax is `")
                                            .append(cmdPrefix)
                                            .append("set commandstring <newCmdString>");
                                } else {
                                    cmdPrefix = cat(args, 2);
                                    reply.append("Command String now set to `").append(cmdPrefix).append("`");
                                }
                            } else {
                                argsMsg(setArgs, event.getChannel());
                            }
                        }
                        break;

                    case "remove":
                        String[] removeArgs = {"event"};
                        if (args.length < 2) {
                            argsMsg(removeArgs, event.getChannel());
                        } else {
                            if ("event".equals(args[1])) {
                                if (args.length < 3) {
                                    reply.append("Which event would you like to remove?\n");
                                    eventRemoveFlag = listEvents(reply);
                                } else {
                                    String eventName = cat(args, 2);
                                    CalendarEvent e = calendar.get(eventName);
                                    if (e == null) {
                                        reply.append("No event found by name: ").append(eventName).append("\n");
                                        reply.append("Which event would you like to remove?\n");
                                        eventRemoveFlag = listEvents(reply);
                                    } else {
                                        removeEvent(reply, eventName);
                                    }
                                }
                            } else {
                                argsMsg(removeArgs, event.getChannel());
                            }
                        }
                        break;

                    case "list":
                        String[] listArgs = {"events"};
                        if (args.length < 2) {
                            argsMsg(listArgs, event.getChannel());
                        } else {
                            if (args[1].equals("events")) {
                                listEvents(reply);
                            }
                        }
                        break;

                    case "rsvp":
                        if(calendar.size() > 0) {
                            String[] rsvpArgs = {"going", "notgoing"};
                            if (args.length < 2) {
                                argsMsg(rsvpArgs, event.getChannel());
                            } else {
                                if (args[1].equals("going") || args[1].equals("notgoing")) {
                                    boolean going = args[1].equals("going");
                                    if (args.length < 3) {
                                        reply.append("Which event would you like to rsvp for?\n");
                                        if (going) {
                                            rsvpGoingFlag = listEvents(reply);
                                        } else {
                                            rsvpStayingFlag = listEvents(reply);
                                        }
                                    } else {
                                        String eventName = cat(args, 2);
                                        rsvpForEvent(going, event.getAuthor(), eventName, reply);
                                    }
                                }
                            }
                        } else {
                            reply.append("No events to rsvp for");
                        }
                        break;

                    case "view":
                        String[] viewArgs = {"event"};
                        if(args.length < 2){
                            argsMsg(viewArgs, event.getChannel());
                        } else {
                            if(args[1].equals("event")){
                                if(args.length < 3){
                                    reply.append("Which event would you like to view?\n");
                                    viewEventFlag = listEvents(reply);
                                } else {
                                    String eventName = cat(args, 2);
                                    CalendarEvent e = calendar.get(eventName);
                                    if (e == null) {
                                        reply.append("No event found by name: ").append(eventName).append("\n");
                                        reply.append("Which event would you like to view?\n");
                                        viewEventFlag = listEvents(reply);
                                    } else {
                                        viewEvent(e, event.getGuild(), reply);
                                    }
                                }
                            }
                        }
                        break;
                    // ping "event name" "message"
                    case "ping":
                        if(args.length < 2){
                            reply.append("Which event would you like to ping?");
                            pingEventFlag = listEvents(reply);
                        } else {
                            Matcher m = Pattern.compile(".*\"(.*)\".*\"(.*)\"").matcher(messageContent);
                            if(m.matches()){
                                String eventName = m.group(1);
                                String outMessage = m.group(2);
                                CalendarEvent e = calendar.get(eventName);
                                if(e != null){
                                    
                                }
                            }
                        }
                    default:

                        break;

                }

            } else if (eventCreateDateFlag != null) {
                try {
                    Date d = new SimpleDateFormat("yyyy-MM-dd-hh:mm").parse(messageContent);
                    if (d != null) {
                        try {
                            eventCreateDateFlag.setStart(d);
                            calendar.add(eventCreateDateFlag);
                            reply.append("Event `").append(eventCreateDateFlag.getName()).append("` successfully created.");
                            eventCreateDateFlag = null;
                        } catch (Exception e){
                            reply.append("Unable to create event, see log for details");
                            logger.error(e.getMessage());
                        }
                    }
                } catch (ParseException e) {
                    logger.error("Parsing error: null message (shouldn't be possible?)");
                    logger.error(e.getMessage());
                }

            } else if (eventRemoveFlag != null) {
                CalendarEvent e = eventLookup(messageContent, rsvpGoingFlag, reply);
                if(e != null){
                    calendar.remove(e.getName());
                }
            } else if(rsvpGoingFlag != null){
                CalendarEvent e = eventLookup(messageContent, rsvpGoingFlag, reply);
                if(e != null){
                    rsvpForEvent(true, event.getAuthor(), e.getName(), reply);
                }
            } else if(rsvpStayingFlag != null){
                CalendarEvent e = eventLookup(messageContent, rsvpStayingFlag, reply);
                if(e != null){
                    rsvpForEvent(false, event.getAuthor(), e.getName(), reply);
                }
            } else if(viewEventFlag != null){
                CalendarEvent e = eventLookup(messageContent, viewEventFlag, reply);
                viewEvent(e, event.getGuild(), reply);
            }

            if (reply.length() > 0) {
                event.getChannel().sendMessage(reply.toString()).queue();
            }
        }
    }

    private void viewEvent(CalendarEvent e, Guild guild, StringBuilder reply){
        if(e != null){
            reply.append("```Name: ").append(e.getName()).append("\n");
            if(e.getDescription() != null){
                reply.append("Description: ").append(e.getDescription()).append("\n");
            }
            reply.append("Start Date: ").append(e.getStart()).append("\n");
            if(e.getEnd() != null){
                reply.append("End Date: ").append(e.getEnd()).append("\n");
            }
            reply.append("Attendees: ");
            User[] attendees = e.getAttendees();
            reply.append(getFormattedUserList(attendees, guild));
            reply.append("\n");
            reply.append("Absentees: ");
            User[] absentees = e.getAbsentees();
            reply.append(getFormattedUserList(absentees, guild));
            reply.append("\n");
            reply.append("```");
        }
    }

    private String getFormattedUserList(User[] list, Guild g){
        StringBuilder reply = new StringBuilder();
        for(int i = 0; i < list.length; i++){
            User u = list[i];
            String name = u.getName();
            Member m = g.getMember(u);
            if(m != null){
                if(m.getNickname() != null) {
                    name = m.getNickname();
                    int x = name.indexOf('|');
                    if (x >= 0) {
                        name = name.substring(x + 2);
                    }
                }
            }
            reply.append(name);
            if(i < list.length - 1){
                reply.append(",");
            }
        }
        return reply.toString();
    }

    private String cat(String[] args, int start){
        StringBuilder x = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i == args.length - 1) {
                x.append(args[i]);
            } else {
                x.append(args[i]).append(" ");
            }
        }
        return x.toString();
    }

    private CalendarEvent eventLookup(String s, CalendarEvent[] e, StringBuilder reply){
        int x;
        try {
            x = Integer.parseInt(s);
            if(x >= e.length){
                reply.append("Index out of range");
                return null;
            }
            return e[x];
        } catch(NumberFormatException ex){
            logger.info("Bad integer parse");
            logger.info(ex.getMessage());

            return null;
        }
    }

    private CalendarEvent[] listEvents(StringBuilder reply){
        CalendarEvent[] events = calendar.toArr();
        if(events.length > 0) {
            int maxwidth = 0;
            for(CalendarEvent e : events){
                if(e.getName().length() > maxwidth){
                    maxwidth = e.getName().length();
                }
            }
            reply.append("```");
            Formatter formatter = new Formatter(reply, Locale.US);
            String formatString = "[%s]\t%-" + maxwidth + "." + maxwidth + "s\t%s\n";
            for (int i = 0; i < events.length; i++) {
                formatter.format(formatString, i, events[i].getName(), events[i].getStart());
            }
            reply.append("```");
        } else {
            reply.append("No events to display");
        }
        return events;
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

    void removeEvent(StringBuilder reply, String eventName){
        try{
            calendar.remove(eventName);
            reply.append("Removed event `").append(eventName).append("`");
        } catch (Exception e){
            reply.append("Unable to remove event, see log");
            logger.error(e.getMessage());
        }
    }

    void rsvpForEvent(boolean going, User user, String eventName, StringBuilder reply){
        CalendarEvent e = calendar.get(eventName);
        if(going) {
            e.rsvpGoing(user);
            reply.append("You have successfully rsvp'd for `").append(e).append("`");
        } else {
            e.rsvpNotGoing(user);
            reply.append("You have successfully anti-rsvp'd for `").append(e).append("`");
        }
    }
}
