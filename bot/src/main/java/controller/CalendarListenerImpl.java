package controller;

import model.CalendarEvent;
import model.GuildCalendar;
import model.MessageHandler;
import model.Persistence;
import net.dv8tion.jda.client.requests.restaction.pagination.MentionPaginationAction;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarListenerImpl extends ListenerAdapter {
    private Persistence persistence;
    private String cmdPrefix;
    private MessageChannel out;
    private GuildCalendar calendar;
    private boolean eventCreateNameFlag = false;
    private CalendarEvent eventCreateDateFlag = null;
    private CalendarEvent[] eventRemoveFlag = null;
    private CalendarEvent[] rsvpGoingFlag= null, rsvpStayingFlag = null, viewEventFlag = null, pingEventFlag = null;
    private CalendarEvent pingWaitingForMessageFlag = null;
    private Logger logger = LoggerFactory.getLogger("CalendarEventListener");
    private String ownerID;

    private String dateFormatString;

    public CalendarListenerImpl(){
        persistence = Persistence.getInstance();
        cmdPrefix = persistence.read(String.class, "commandprefix");
        if(cmdPrefix == null){
            cmdPrefix = ";";
            persistence.addObject("commandprefix", cmdPrefix);
        }

        dateFormatString = persistence.read(String.class, "dateformatstring");
        if(dateFormatString == null){
            dateFormatString = "yyyy-MM-dd-hh:mm";
            persistence.addObject("dateformatstring", dateFormatString);
        }

        calendar = persistence.read(GuildCalendar.class, "calendar");
        if(calendar == null){
            calendar = new GuildCalendar();
            persistence.addObject("calendar", calendar);
        }

        ownerID = persistence.read(String.class, "owner");

    }

    @Override
    public void onReady(ReadyEvent event) {
        updatePlayingMessage(event.getJDA().getPresence());
    }

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
                        resetFlags();
                        String[] createArgs = {"event"};
                        if (args.length < 2) {
                            argsMsg(createArgs, event.getChannel());
                        } else {
                            if ("event".equals(args[1])) {
                                if (args.length < 3) {
                                    reply.append("Please input a name for the event");
                                    eventCreateNameFlag = true;
                                } else {
                                    StringBuilder x = new StringBuilder();
                                    for (int i = 2; i < args.length; i++) {
                                        if (i == args.length - 1) {
                                            x.append(args[i]);
                                        } else {
                                            x.append(args[i]).append(" ");
                                        }
                                    }
                                    if(calendar.contains(x.toString())){
                                        reply.append("An active event already exists with that name. Please input a new name.");
                                        eventCreateNameFlag = true;
                                    } else {
                                        eventCreateDateFlag = new CalendarEvent(message.getAuthor().getId(), x.toString());
                                        reply.append("Please input a start time for the event in `").append(dateFormatString).append("` format");
                                    }
                                }
                            } else {
                                argsMsg(createArgs, event.getChannel());
                            }
                        }
                        break;
                    //Handles all settings
                    case "set":
                        resetFlags();
                        String[] setArgs = {"commandstring", "event", "dateformatstring"};
                        if (args.length < 2) {
                            argsMsg(setArgs, event.getChannel());
                        } else {
                            if ("commandstring".equals(args[1])) {
                                if (args.length < 3) {
                                    reply.append("Missing command string. Proper syntax is `")
                                            .append(cmdPrefix)
                                            .append("set commandstring <newCmdString>`");
                                } else {
                                    String c = cat(args, 2);
                                    setCommandString(c, event.getJDA().getPresence(), reply);
                                }
                            } else if (args[1].equals("event")) {
                                String[] setEventArgs = {"description", "start", "end", "location"};
                                if (args.length < 3) {
                                    argsMsg(setEventArgs, event.getChannel());
                                } else {
                                    Matcher m = Pattern.compile(".*\"(.*)\".*\"(.*)\"").matcher(messageContent);
                                    String eventName, arg;
                                    if (m.matches()) {
                                        eventName = m.group(1);
                                        CalendarEvent e = calendar.get(eventName);
                                        if (e == null) {
                                            reply.append("invalid event name");
                                        } else {
                                            arg = m.group(2);
                                            if (args[2].equals("description")) {
                                                e.setDescription(arg);
                                                reply.append("Description Updated:\n");
                                                viewEvent(e, event.getGuild(), reply);
                                            } else if (args[2].equals("location")) {
                                                e.setLocation((arg));
                                                reply.append("Location Updated:\n");
                                                viewEvent(e, event.getGuild(), reply);
                                            } else if (args[2].equals("start")) {
                                                Date d = parseDate(arg, reply);
                                                if (d != null) {
                                                    e.setStart(d);
                                                    reply.append("Start Date Updated:\n");
                                                    viewEvent(e, event.getGuild(), reply);
                                                }
                                            } else if (args[2].equals("end")) {
                                                Date d = parseDate(arg, reply);
                                                if (d != null) {
                                                    e.setEnd(d);
                                                    reply.append("End Date Updated:\n");
                                                    viewEvent(e, event.getGuild(), reply);
                                                }
                                            }
                                        }

                                    }
                                }
                        } else if (args[1].equals("dateformatstring")){
                            if(args.length < 3){
                                reply.append("Format string missing");
                            } else {
                                String s = cat(args, 2);
                                setDateFormatString(s, reply);
                            }
                        } else {
                                argsMsg(setArgs, event.getChannel());
                            }
                        }
                        break;

                    case "remove":
                        resetFlags();
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
                        resetFlags();
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
                        resetFlags();
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
                        resetFlags();
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
                        resetFlags();
                        if(args.length < 2){
                            reply.append("Which event would you like to ping?\n");
                            pingEventFlag = listEvents(reply);
                        } else {
                            Matcher m = Pattern.compile(".*\"(.*)\".*\"(.*)\"").matcher(messageContent);
                            if(m.matches()){
                                String eventName = m.group(1);
                                String outMessage = m.group(2);
                                CalendarEvent e = calendar.get(eventName);
                                if(e != null){
                                    if(e.getAttendees().length > 0) {
                                        if (outMessage != null) {
                                            pingEvent(e, event.getAuthor(), reply, outMessage);
                                        } else {
                                            reply.append("Unable to parse message. Syntax for `ping` is:\n")
                                                    .append("`").append(cmdPrefix)
                                                    .append("ping <\"event name\"> <\"message\">`");
                                        }
                                    } else {
                                        reply.append("Event has no attendees - ping cancelled");
                                    }
                                } else {
                                     reply.append("Unable to find event with that name; which event would you like to ping?\n");
                                     pingEventFlag = listEvents(reply);
                                }
                            } else {
                                reply.append("Unable to parse message. Syntax for `ping` is:\n")
                                        .append("`").append(cmdPrefix)
                                        .append("ping <\"event name\"> <\"message\">`");
                            }
                        }
                        break;
                    case "help":
                        help(reply);
                        break;
                    //Temp debug functions

                    case "owner":
                        ownerID = event.getAuthor().getId();
                        persistence.addObject("owner", ownerID);
                    case "shutdown":
                        if(event.getAuthor().getId().equals(ownerID)){
                            event.getChannel().sendMessage("Shutting down").complete();
                            System.exit(1);
                        } else {
                            reply.append("Only Josh can do that, dingus");
                        }
                    default:

                        break;

                }

            } else if(eventCreateNameFlag) {
                if(calendar.contains(messageContent)){
                    reply.append("An active event already exists with that name. Please input a new name.");
                }else {
                    CalendarEvent e = new CalendarEvent(event.getAuthor().getId(), messageContent);
                    eventCreateNameFlag = false;
                    reply.append("Please input a start time for the event in `").append(dateFormatString).append("` format");
                    eventCreateDateFlag = e;
                }
            } else if (eventCreateDateFlag != null) {
                Date d = parseDate(messageContent, reply);
                if(d != null) {
                    eventCreateDateFlag.setStart(d);
                    registerEvent(eventCreateDateFlag, reply);
                    eventCreateDateFlag = null;
                }
            } else if (eventRemoveFlag != null) {
                CalendarEvent e = eventLookup(messageContent, rsvpGoingFlag, reply);
                if(e != null){
                    removeEvent(reply, e.getName());
                    eventRemoveFlag = null;
                }
            } else if(rsvpGoingFlag != null){
                CalendarEvent e = eventLookup(messageContent, rsvpGoingFlag, reply);
                if(e != null){
                    rsvpForEvent(true, event.getAuthor(), e.getName(), reply);
                    rsvpGoingFlag = null;
                }
            } else if(rsvpStayingFlag != null){
                CalendarEvent e = eventLookup(messageContent, rsvpStayingFlag, reply);
                if(e != null){
                    rsvpForEvent(false, event.getAuthor(), e.getName(), reply);
                    rsvpStayingFlag = null;
                }
            } else if(viewEventFlag != null){
                CalendarEvent e = eventLookup(messageContent, viewEventFlag, reply);
                if(e != null) {
                    viewEvent(e, event.getGuild(), reply);
                    viewEventFlag = null;
                }
            } else if(pingEventFlag != null){
                CalendarEvent e = eventLookup(messageContent, pingEventFlag, reply);
                if(e != null) {
                    reply.append("What would you like your message to say?");
                    pingWaitingForMessageFlag = e;
                    pingEventFlag = null;
                }
            } else if(pingWaitingForMessageFlag != null){
                pingEvent(pingWaitingForMessageFlag, event.getAuthor(),reply, messageContent);
                pingWaitingForMessageFlag = null;
            }

            if (reply.length() > 0) {
                event.getChannel().sendMessage(reply.toString()).queue();
            }
        }
    }

    private void setCommandString(String s, Presence p, StringBuilder reply){
        cmdPrefix = s;
        persistence.addObject("commandprefix", cmdPrefix);
        updatePlayingMessage(p);
        reply.append("Command String now set to `").append(cmdPrefix).append("`");
        logger.info("Command Prefix set to " + s);
    }

    private void setDateFormatString(String s, StringBuilder reply){
        dateFormatString = s;
        persistence.addObject("dateformatstring", dateFormatString);
        reply.append("Date Format String now set to `").append(s).append("`");
        logger.info("Date Format String set to " + s);
    }

    private void registerEvent(CalendarEvent e, StringBuilder reply){
        calendar.add(e);
        persistence.addObject("calendar", calendar);
        reply.append("Event `").append(eventCreateDateFlag.getName()).append("` successfully created.");
        logger.info("Created event: " + eventCreateDateFlag);
    }

    private void pingEvent(CalendarEvent calendarEvent, User author, StringBuilder reply, String msg){
        for(User u:calendarEvent.getAttendees()){
            reply.append(u.getAsMention()).append(" ");
        }
        reply.append("\n").append(msg);
        logger.info("User " + author + " has pinged event " + calendarEvent);
    }

    private void viewEvent(CalendarEvent e, Guild guild, StringBuilder reply){
        if(e != null){
            reply.append("```Name: ").append(e.getName()).append("\n");
            if(e.getDescription() != null){
                reply.append("Description: ").append(e.getDescription()).append("\n");
            }
            if(e.getLocation() != null){
                reply.append("Location: ").append(e.getLocation()).append("\n");
            }
            reply.append("Start Time: ").append(e.getStart()).append("\n");
            if(e.getEnd() != null){
                reply.append("End Time: ").append(e.getEnd()).append("\n");
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

    private void help(StringBuilder reply){
        reply.append("```").append(cmdPrefix).append("create event <name> - creates an event\n\n")
                .append(cmdPrefix).append("set commandstring <new cmdstring> - sets the command prefix\n\n")
                .append(cmdPrefix).append("set dateformatstring <new dateFormatString> - sets the date formatting pattern. See Java Date documentation\n\n")
                .append(cmdPrefix).append("set event description/location/start/end <\"event name\"> <\"arg\"> - modify various attributes of an event\n\n")
                .append(cmdPrefix).append("remove event <event name> - removes a specified event\n\n")
                .append(cmdPrefix).append("list events - lists all currently active events")
                .append(cmdPrefix).append("view event <event name> - view all event information\n\n")
                .append(cmdPrefix).append("ping <\"event name\"> <\"message\"> - pings all users going to an event with the specified message")
                .append("```");
    }

    private Date parseDate(String s, StringBuilder reply){
        try {
            Date d = new SimpleDateFormat(dateFormatString).parse(s);
            if(d == null){
                reply.append("Unable to parse date. Format must be `").append(dateFormatString)
                        .append("`. To change format string, use `").append(cmdPrefix).append("set dateformatstring`.");
            }
            return d;
        } catch (ParseException ex) {
            logger.error("Parsing error: null message (shouldn't be possible?)");
            logger.error(ex.getMessage());
            reply.append("Parsing error (tell josh)");
        }
        return null;
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
                reply.append(", ");
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
            Arrays.sort(events);
            int maxwidth = 0;
            for(CalendarEvent e : events){
                if(e.getName().length() > maxwidth){
                    maxwidth = e.getName().length();
                }
            }
            reply.append("```");
            Formatter formatter = new Formatter(reply, Locale.US);
            String formatString = "[%s]  %-" + maxwidth + "." + maxwidth + "s  %s\n";
            for (int i = 0; i < events.length; i++) {
                formatter.format(formatString, i, events[i].getName(), events[i].getStart());
            }
            reply.append("```");
        } else {
            reply.append("No events to display");
        }
        return events;
    }

    private void argsMsg(String[] requiredArgs, MessageChannel c){
        StringBuilder possibleArgs = new StringBuilder();
        for(int i = 0; i < requiredArgs.length; i++){
            possibleArgs.append(requiredArgs[i]);
            if(i < requiredArgs.length - 1){
                possibleArgs.append(", ");
            }
        }
        c.sendMessage("```Missing required arguments\n" +
                "Possible arguments: " + possibleArgs + "```").queue();
    }

    private void removeEvent(StringBuilder reply, String eventName){
        try{
            CalendarEvent e = calendar.get(eventName);
            calendar.remove(eventName);
            persistence.addObject("calendar", calendar);
            reply.append("Removed event `").append(eventName).append("`");
            logger.info("Removed event: " + e);
        } catch (Exception e){
            reply.append("Unable to remove event, see log");
            logger.error(e.getMessage());
        }
    }

    private void rsvpForEvent(boolean going, User user, String eventName, StringBuilder reply){
        CalendarEvent e = calendar.get(eventName);
        if(going) {
            e.rsvpGoing(user);
            persistence.addObject("calendar", calendar);
            reply.append("You have successfully rsvp'd for `").append(e.getName()).append("`");
            logger.info("User " + user + " has rsvp'd 'going' for event " + e);
        } else {
            e.rsvpNotGoing(user);
            persistence.addObject("calendar", calendar);
            reply.append("You have successfully anti-rsvp'd for `").append(e.getName()).append("`");
            logger.info("User " + user + " has rsvp'd 'not going' for event " + e);
        }
    }

    private void updatePlayingMessage(Presence p){
        p.setGame(Game.playing(cmdPrefix + "help for cmd list"));
    }

    private void resetFlags(){
        eventCreateNameFlag = false;
        eventCreateDateFlag = null;
        eventRemoveFlag = null;
        rsvpGoingFlag= null;
        rsvpStayingFlag = null;
        viewEventFlag = null;
        pingEventFlag = null;
        pingWaitingForMessageFlag = null;
    }
}
