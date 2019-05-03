package controller;

import model.*;
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

import java.io.Serializable;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarListenerImpl extends ListenerAdapter {
    private Persistence persistence;
    private String cmdPrefix;
    private GuildCalendar calendar;
    private Logger logger = LoggerFactory.getLogger("CalendarEventListener");
    private HashMap<Signature, Session> sessions;
    private String ownerID;

    private String dateFormatString;


    //TODO: Remove message history
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

        sessions = new HashMap<>();

    }

    @Override
    public void onReady(ReadyEvent event) {
        updatePlayingMessage(event.getJDA().getPresence());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor() != event.getJDA().getSelfUser()) {
            Session session;
            Signature sig = new Signature(event.getAuthor(), event.getChannel(), event.getGuild());
            session = sessions.get(sig);
            StringBuilder reply = new StringBuilder();

            Message message = event.getMessage();
            String messageContent = message.getContentDisplay();
            boolean isCommand = Pattern.compile("\\Q" + cmdPrefix + "\\E.+").matcher(messageContent).matches();

            if (isCommand) {
                messageContent = messageContent.substring(cmdPrefix.length());
                String[] args = messageContent.split(" ");
                session = null;

                switch (args[0]) {

                    //Handles all create-related commands (create event)
                    case "create":
                        String[] createArgs = {"event"};
                        if (args.length < 2) {
                            argsMsg(createArgs, event.getChannel());
                        } else {
                            if ("event".equals(args[1])) {
                                if (args.length < 3) {
                                    reply.append("Please input a name for the event");
                                    addSession(sig, "createEventName", true);
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
                                        addSession(sig, "createEventName", true);
                                    } else {
                                        addSession(sig, "createEventDate", new CalendarEvent(event.getAuthor(), x.toString()));
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
                                            switch (args[2]) {
                                                case "description":
                                                    e.setDescription(arg);
                                                    reply.append("Description Updated:\n");
                                                    viewEvent(e, event.getGuild(), reply);
                                                    break;
                                                case "location":
                                                    e.setLocation((arg));
                                                    reply.append("Location Updated:\n");
                                                    viewEvent(e, event.getGuild(), reply);
                                                    break;
                                                case "start": {
                                                    Date d = parseDate(arg, reply);
                                                    if (d != null) {
                                                        e.setStart(d);
                                                        reply.append("Start Date Updated:\n");
                                                        viewEvent(e, event.getGuild(), reply);
                                                    }
                                                    break;
                                                }
                                                case "end": {
                                                    Date d = parseDate(arg, reply);
                                                    if (d != null) {
                                                        e.setEnd(d);
                                                        reply.append("End Date Updated:\n");
                                                        viewEvent(e, event.getGuild(), reply);
                                                    }
                                                    break;
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
                        String[] removeArgs = {"event"};
                        if (args.length < 2) {
                            argsMsg(removeArgs, event.getChannel());
                        } else {
                            if ("event".equals(args[1])) {
                                if (args.length < 3) {
                                    reply.append("Which event would you like to remove?\n");
                                    addSession(sig, "eventRemoveFlag", listEvents(reply));
                                } else {
                                    String eventName = cat(args, 2);
                                    CalendarEvent e = calendar.get(eventName);
                                    if (e == null) {
                                        reply.append("No event found by name: ").append(eventName).append("\n");
                                        reply.append("Which event would you like to remove?\n");
                                        addSession(sig, "eventRemoveFlag", listEvents(reply));
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
                                            addSession(sig, "rsvpGoingFlag", listEvents(reply));
                                        } else {
                                            addSession(sig, "rsvpStayingFlag", listEvents(reply));
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
                                    addSession(sig, "viewEventFlag", listEvents(reply));
                                } else {
                                    String eventName = cat(args, 2);
                                    CalendarEvent e = calendar.get(eventName);
                                    if (e == null) {
                                        reply.append("No event found by name: ").append(eventName).append("\n");
                                        reply.append("Which event would you like to view?\n");
                                        addSession(sig, "viewEventFlag", listEvents(reply));
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
                            reply.append("Which event would you like to ping?\n");
                            addSession(sig, "pingEventFlag", listEvents(reply));
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
                                     addSession(sig, "pingEventFlag", listEvents(reply));
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
                        if(ownerID == null || event.getAuthor().getId().equals(ownerID)) {
                            ownerID = event.getAuthor().getId();
                            persistence.addObject("owner", ownerID);
                        } else{
                            reply.append("The owner has already been set");
                        }
                        break;
                    case "shutdown":
                        if(event.getAuthor().getId().equals(ownerID)){
                            event.getChannel().sendMessage("Shutting down").complete();
                            System.exit(1);
                        } else {
                            reply.append("Only Josh can do that, dingus");
                        }
                        break;
                    default:

                        break;

                }

            } if(session != null) {
                if (session.getSessionData(Boolean.class, "createEventName") != null) {
                    if (calendar.contains(messageContent)) {
                        reply.append("An active event already exists with that name. Please input a new name.");
                    } else {
                        CalendarEvent e = new CalendarEvent(event.getAuthor(), messageContent);
                        reply.append("Please input a start time for the event in `").append(dateFormatString).append("` format");
                        session.addSessionData("createEventDate", e);
                        session.removeSessionData("createEventName");
                    }
                } else if (session.getSessionData(CalendarEvent.class, "createEventDate") != null) {
                    CalendarEvent data = session.getSessionData(CalendarEvent.class, "createEventDate");
                    Date d = parseDate(messageContent, reply);
                    if (d != null) {
                        data.setStart(d);
                        registerEvent(data, reply);
                        session.removeSessionData("createEventDate");
                    }
                } else if (session.getSessionData(CalendarEvent[].class, "eventRemoveFlag") != null) {
                    CalendarEvent[] data = session.getSessionData(CalendarEvent[].class, "eventRemoveFlag");
                    CalendarEvent e = eventLookup(messageContent, data, reply);
                    if (e != null) {
                        removeEvent(reply, e.getName());
                        session.removeSessionData("eventRemoveFlag");
                    }
                } else if (session.getSessionData(CalendarEvent[].class, "rsvpGoingFlag") != null) {
                    CalendarEvent[] data = session.getSessionData(CalendarEvent[].class, "rsvpGoingFlag");
                    CalendarEvent e = eventLookup(messageContent, data, reply);
                    if (e != null) {
                        rsvpForEvent(true, event.getAuthor(), e.getName(), reply);
                        session.removeSessionData("rsvpGoingFlag");
                    }
                } else if (session.getSessionData(CalendarEvent[].class, "rsvpStayingFlag") != null) {
                    CalendarEvent[] data = session.getSessionData(CalendarEvent[].class, "rsvpStayingFlag");
                    CalendarEvent e = eventLookup(messageContent, data, reply);
                    if (e != null) {
                        rsvpForEvent(false, event.getAuthor(), e.getName(), reply);
                        session.removeSessionData("rsvpGoingFlag");
                    }
                } else if (session.getSessionData(CalendarEvent[].class, "viewEventFlag") != null) {
                    CalendarEvent[] data = session.getSessionData(CalendarEvent[].class, "viewEventFlag");
                    CalendarEvent e = eventLookup(messageContent, data, reply);
                    if (e != null) {
                        viewEvent(e, event.getGuild(), reply);
                        session.removeSessionData("viewEventFlag");
                    }
                } else if (session.getSessionData(CalendarEvent[].class, "pingEventFlag") != null) {
                    CalendarEvent[] data = session.getSessionData(CalendarEvent[].class, "pingEventFlag");
                    CalendarEvent e = eventLookup(messageContent, data, reply);
                    if (e != null) {
                        reply.append("What would you like your message to say?");
                        session.addSessionData("pingWaitingForMessageFlag", e);
                        session.removeSessionData("pingEventFlag");
                    }
                } else if (session.getSessionData(CalendarEvent.class, "pingWaitingForMessageFlag") != null) {
                    CalendarEvent data = session.getSessionData(CalendarEvent.class, "pingWaitingForMessageFlag");
                    pingEvent(data, event.getAuthor(), reply, messageContent);
                    session.removeSessionData("pingWaitingForMessageFlag");
                } else {
                    sessions.remove(sig);
                }
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
        reply.append("Event `").append(e.getName()).append("` successfully created.");
        logger.info("Created event: " + e);
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

    private Session addSession(Signature sig, String key, Serializable data){
        if(data != null) {
            Session s = new Session(sig);
            s.addSessionData(key, data);
            sessions.put(sig, s);
            return s;
        } else return null;
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
            reply.append("Unable to parse date. Format must be `").append(dateFormatString)
                    .append("`. To change format string, use `").append(cmdPrefix).append("set dateformatstring`.");
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

    private CalendarEvent eventLookup(String s, CalendarEvent[] events, StringBuilder reply){
        int x;
        CalendarEvent e = calendar.get(s);
        if(e == null){
            try {
                x = Integer.parseInt(s);
                if (x >= events.length) {
                    reply.append("Index out of range");
                    return null;
                }
                e = events[x];
            } catch (NumberFormatException ex) {
                logger.info("Bad integer parse");
                logger.info(ex.getMessage());

                return null;
            }
        }
        return e;
    }

    private CalendarEvent[] listEvents(StringBuilder reply){
        CalendarEvent[] events = calendar.toArr();
        if(events.length > 0) {
            Arrays.sort(events);
            int maxwidth = 12;
            for(CalendarEvent e : events){
                if(e.getName().length() > maxwidth){
                    maxwidth = e.getName().length();
                }
            }
            reply.append("```");
            Formatter formatter = new Formatter(reply, Locale.US);
            String formatString = "%-5.5s  %-" + maxwidth + "." + maxwidth + "s  %s\n";
            formatter.format(formatString, "Index", "Event Name", "Event Start" );
            for (int i = 0; i < events.length; i++) {
                formatter.format(formatString, i, events[i].getName(), events[i].getStart());
            }
            reply.append("```");
        } else {
            reply.append("No events to display");
            return null;
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
}
