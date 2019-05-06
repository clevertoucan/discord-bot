package controller;

import model.*;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.Presence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private ArrayList<String> baharQuotes = new ArrayList<>();
    private String ownerID, baharID;

    private String dateFormatString;
    private Boolean cleanup = false;
    private StringBuilder reply = new StringBuilder();

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
        cleanup = persistence.read(Boolean.class, "cleanup");
        if(cleanup == null){
            cleanup = false;
            persistence.addObject("cleanup", cleanup);
        }
        ownerID = persistence.read(String.class, "owner");

        File quotes = new File("BaharQuotes");
        if(quotes.exists()){
            try {
                BufferedReader reader = new BufferedReader(new FileReader(quotes));
                while(reader.ready()){
                    baharQuotes.add(reader.readLine());
                }
            } catch (IOException e){
                logger.error("IOException", e);
            }
        }
        /*
        baharQuotes = persistence.read(HashSet.class, "baharQuotes");
        if(baharQuotes == null){
            baharQuotes = new HashSet<>();
        }
        */

        sessions = new HashMap<>();

    }

    @Override
    public void onReady(ReadyEvent event) {
        logger.info("in onReady");
        updatePlayingMessage(event.getJDA().getPresence());
    }

    //TODO: Add "set event ping <"eventName"> <"timeBeforeEvent"> <"customMessage"> ", add "set verbose on/off"
    //TODO: Limit sensitive operations to server admins
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor() != event.getJDA().getSelfUser()) {
            Session session;
            Signature sig = new Signature(event.getAuthor(), event.getChannel(), event.getGuild());
            Context context = new Context(sig, event.getJDA());
            session = sessions.get(sig);

            Message message = event.getMessage();
            String messageContent = message.getContentDisplay();
            boolean isCommand = Pattern.compile("\\Q" + cmdPrefix + "\\E.+").matcher(messageContent).matches();

            if (isCommand) {
                context.getMessageHistory().addMessage(event.getMessage());
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
                                    addSession(context, "createEventName");
                                } else {
                                    String x = cat(args, 2);
                                    if(calendar.contains(x)){
                                        reply.append("An active event already exists with that name. Please input a new name.");
                                        addSession(context, "createEventName");
                                    } else {
                                        context.event = new CalendarEvent(event.getAuthor(), x);
                                        addSession(context, "createEventDate");
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
                        String[] setArgs = {"commandstring", "event", "dateformatstring, cleanup"};
                        if (args.length < 2) {
                            argsMsg(setArgs, event.getChannel());
                        } else {
                            if (args[1].equals("commandstring")) {
                                if (args.length < 3) {
                                    reply.append("Missing command string. Proper syntax is `")
                                            .append(cmdPrefix)
                                            .append("set commandstring <newCmdString>`");
                                } else {
                                    context.commandString = cat(args, 2);
                                    processEvent(context, this::setCommandString);
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
                                            context.event = e;
                                            arg = m.group(2);
                                            switch (args[2]) {
                                                case "description":
                                                    context.description = arg;
                                                    processEvent(context, this::setDescription);
                                                    break;
                                                case "location":
                                                    context.location = arg;
                                                    processEvent(context, this::setLocation);
                                                    break;
                                                case "start": {
                                                    Date d = parseDate(arg, reply);
                                                    if (d != null) {
                                                        context.date = d;
                                                        processEvent(context, this::setStartDate);
                                                    }
                                                    break;
                                                }
                                                case "end": {
                                                    Date d = parseDate(arg, reply);
                                                    if (d != null) {
                                                        context.date = d;
                                                        processEvent(context, this::setEndDate);
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
                                context.dateFormatString = cat(args, 2);
                                processEvent(context, this::setDateFormatString);
                            }
                        } else if(args[1].equals("cleanup")) {
                            if(args.length < 3){
                                reply.append("Missing `on`/`off`");
                            } else {
                                if(args[2].equals("on")){
                                    List<Permission> permissions = context.getGuild().getMember(event.getJDA().getSelfUser())
                                            .getPermissions(context.getChannel());
                                    for(Permission p : permissions){
                                        if(p.equals(Permission.MESSAGE_MANAGE)){
                                            context.messageDeleteOn = true;
                                        }
                                    }
                                    permissions = context.getGuild().getMember(event.getJDA().getSelfUser()).getPermissions();
                                    for(Permission p : permissions){
                                        if(p.equals(Permission.MESSAGE_MANAGE)){
                                            context.messageDeleteOn = true;
                                        }
                                    }
                                    if(context.messageDeleteOn != null && context.messageDeleteOn) {
                                        processEvent(context, this::setDeleteUserMessages);
                                    }else {
                                        reply.append("Cannot turn on cleanup: need `Manage Messages` permission");
                                    }
                                } else if(args[2].equals("off")){
                                    context.messageDeleteOn = false;
                                    processEvent(context, this::setDeleteUserMessages);
                                }
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
                                    context.events = listEvents();
                                    addSession(context, "removeEvent");
                                } else {
                                    String eventName = cat(args, 2);
                                    CalendarEvent e = calendar.get(eventName);
                                    if (e == null) {
                                        reply.append("No event found by name: ").append(eventName).append("\n");
                                        reply.append("Which event would you like to remove?\n");
                                        context.events = listEvents();
                                        addSession(context, "removeEvent");
                                    } else {
                                        context.event = calendar.get(eventName);
                                        processEvent(context, this::removeEvent);
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
                                processEvent(context, this::listEvents);
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
                                    context.rsvpGoing= args[1].equals("going");
                                    if (args.length < 3) {
                                        reply.append("Which event would you like to rsvp for?\n");
                                        context.events = listEvents();
                                        addSession(context, "rsvpEvent");
                                    } else {
                                        context.event= calendar.get(cat(args, 2));
                                        processEvent(context, this::rsvpForEvent);
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
                                    context.events = listEvents();
                                    addSession(context, "viewEvent");
                                } else {
                                    String eventName = cat(args, 2);
                                    CalendarEvent e = calendar.get(eventName);
                                    if (e == null) {
                                        reply.append("No event found by name: ").append(eventName).append("\n");
                                        reply.append("Which event would you like to view?\n");
                                        context.events = listEvents();
                                        addSession(context, "viewEvent");
                                    } else {
                                        context.event = e;
                                        processEvent(context, this::viewEvent);
                                    }
                                }
                            }
                        }
                        break;
                    // ping "event name" "message"
                    case "ping":
                        if(args.length < 2){
                            reply.append("Which event would you like to ping?\n");
                            context.events = listEvents();
                            addSession(context, "pingEvent");
                        } else {
                            Matcher m = Pattern.compile(".*\"(.*)\".*\"(.*)\"").matcher(messageContent);
                            if(m.matches()){
                                String eventName = m.group(1);
                                String outMessage = m.group(2);
                                CalendarEvent e = calendar.get(eventName);
                                if(e != null){
                                    if(e.getAttendees().length > 0) {
                                        if (outMessage != null) {
                                            context.event=e;
                                            context.pingMessageContent = outMessage;
                                            processEvent(context, this::pingEvent);
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
                                     context.events = listEvents();
                                     addSession(context, "pingEvent");
                                }
                            } else {
                                reply.append("Unable to parse message. Syntax for `ping` is:\n")
                                        .append("`").append(cmdPrefix)
                                        .append("ping <\"event name\"> <\"message\">`");
                            }
                        }
                        break;
                    case "help":
                        processEvent(context, this::help);
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

                    case "baharquote":
                        Random rand = new Random();
                        int index = rand.nextInt(baharQuotes.size());
                        reply.append(baharQuotes.get(index));
                        break;
                    case "bahar":
                        logger.info("Retrieving bahar quotes");
                        if(baharQuotes.isEmpty()) {
                            List<Guild> guilds = event.getJDA().getGuilds();
                            for (Guild g : guilds) {
                                if (g.isMember(event.getJDA().getUserById(baharID))) {
                                    for (MessageChannel messageChannel : g.getTextChannels()) {
                                        for (Message m : messageChannel.getIterableHistory()) {
                                            if (m.getAuthor().getId().equals(baharID) && m.getReactions().size() > 0) {
                                                baharQuotes.add(m.getContentRaw());
                                            }
                                        }
                                    }
                                }
                            }
                            logger.info("Writing Bahar Quotes");
                            File quotes = new File("BaharQuotes");
                            boolean exists = quotes.exists();
                            if (!exists) {
                                try {
                                    exists = quotes.createNewFile();
                                } catch (IOException e) {
                                    logger.error("bahar quotes error", e);
                                }
                            }
                            if (exists) {
                                try {
                                    BufferedWriter writer = new BufferedWriter(new FileWriter(quotes, true));
                                    for (String s : baharQuotes) {
                                        writer.append(s).append("\n");
                                    }
                                    writer.close();
                                } catch (IOException e) {
                                    logger.error("bahar quotes error", e);
                                }
                            }
                            logger.info("Finished Bahar Quotes");
                        }
                        break;
                    default:

                        break;

                }

            } if(session != null) {
                context = session.getContext();
                context.getMessageHistory().addMessage(event.getMessage());
                switch (session.getFlag()) {
                    case "createEventName":
                        if (calendar.contains(messageContent)) {
                            reply.append("An active event already exists with that name. Please input a new name.");
                        } else {
                            context.event = new CalendarEvent(event.getAuthor(), messageContent);
                            reply.append("Please input a start time for the event in `").append(dateFormatString).append("` format");
                            session.setFlag("createEventDate");
                        }
                        break;
                    case "createEventDate":
                        Date d = parseDate(messageContent, reply);
                        if (d != null) {
                            context.event.setStart(d);
                            processEvent(context, this::registerEvent);
                            sessions.remove(session.getContext().getSignature());
                        }
                        break;
                    case "removeEvent":
                        context.event = eventLookup(messageContent, context.events, reply);
                        if (context.event != null) {
                            processEvent(context, this::removeEvent);
                            sessions.remove(session.getContext().getSignature());
                        }
                        break;
                    case "rsvpEvent": {
                        CalendarEvent e = eventLookup(messageContent, context.events, reply);
                        if (e != null) {
                            processEvent(context, this::rsvpForEvent);
                            sessions.remove(session.getContext().getSignature());
                        }
                        break;
                    }
                    case "viewEvent": {
                        CalendarEvent e = eventLookup(messageContent, context.events, reply);
                        if (e != null) {
                            context.event = e;
                            processEvent(context, this::viewEvent);
                            sessions.remove(session.getContext().getSignature());
                        }
                        break;
                    }
                    case "pingEvent":
                        context.event = eventLookup(messageContent, context.events, reply);
                        if (context.event != null) {
                            reply.append("What would you like your message to say?");
                            session.setFlag("pingMessage");
                        }
                        break;
                    case "pingMessage":
                        context.pingMessageContent = messageContent;
                        processEvent(context, this::pingEvent);
                        sessions.remove(session.getContext().getSignature());
                        break;
                    default:
                        sessions.remove(sig);
                        break;
                }
            }

            if (reply.length() > 0) {
                Message m = event.getChannel().sendMessage(reply.toString()).complete();
                context.getMessageHistory().addMessage(m);

                reply = new StringBuilder();
            }
        }
    }

    interface Cmd { boolean execute(Context c); }
    private void processEvent(Context c, Cmd func){
        boolean success = func.execute(c);
        reply.append("```Requested by ").append(familiarizeName(c.getGuild(), c.getUser()))
                .append(" on ").append(new Date()).append("```\n");

        if(cleanup != null && cleanup) {
            c.clearMessageHistory();
        }
    }

    private boolean setDeleteUserMessages(Context c){
        cleanup = c.messageDeleteOn;
        persistence.addObject("cleanup", cleanup);
        if(cleanup){
            reply.append("Turned on cleanup\n");
        } else {
            reply.append("Turned off cleanup\n");
        }
        return true;
    }

    private boolean setDescription(Context c){
        c.event.setDescription(c.description);
        reply.append("Description Updated:\n");
        return viewEvent(c);
    }

    private boolean setLocation(Context c){
        c.event.setLocation(c.location);
        reply.append("Description Updated:\n");
        return viewEvent(c);
    }

    private boolean setStartDate(Context c){
        c.event.setStart(c.date);
        reply.append("Description Updated:\n");
        return viewEvent(c);
    }

    private boolean setEndDate(Context c){
        c.event.setEnd(c.date);
        reply.append("Description Updated:\n");
        return viewEvent(c);
    }

    private boolean setCommandString(Context c){
        String s = c.commandString;
        Presence p = c.getJDA().getPresence();
        cmdPrefix = s;
        persistence.addObject("commandprefix", cmdPrefix);
        updatePlayingMessage(p);
        reply.append("Command String now set to `").append(cmdPrefix).append("`\n");
        logger.info("Command Prefix set to " + s);
        return true;
    }

    private boolean setDateFormatString(Context c){
        String formatString = c.dateFormatString;
        dateFormatString = formatString;
        persistence.addObject("dateformatstring", dateFormatString);
        reply.append("Date Format String now set to `").append(formatString).append("`\n");
        logger.info("Date Format String set to " + formatString);
        return true;
    }

    private boolean registerEvent(Context c){
        CalendarEvent e = c.event;
        calendar.add(e);
        persistence.addObject("calendar", calendar);
        reply.append("Event `").append(e.getName()).append("` successfully created.\n");
        viewEvent(c);
        logger.info("Created event: " + e);
        return true;
    }

    private boolean pingEvent(Context c){
        CalendarEvent calendarEvent = c.event;
        User author = c.getUser();
        String msg = c.pingMessageContent;
        for(User u:calendarEvent.getAttendees()){
            reply.append(u.getAsMention()).append(" ");
        }
        reply.append("\n").append(msg).append("\n");
        logger.info("User " + author + " has pinged event " + calendarEvent);
        return true;
    }

    private boolean viewEvent(Context c){
        Guild guild = c.getGuild();
        CalendarEvent e = c.event;
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
            reply.append("```\n");
        }
        return true;
    }


    private String familiarizeName(Guild g, User u){
        Member m = g.getMember(u);
        String name = u.getName();
        if(m != null){
            if(m.getNickname() != null) {
                name = m.getNickname();
                int x = name.indexOf('|');
                if (x >= 0) {
                    name = name.substring(x + 2);
                }
            }
        }
        return name;
    }

    private Session addSession(Context context, String flag){
            Session s = new Session(context, flag);
            sessions.put(context.getSignature(), s);
            return s;
    }

    private boolean help(Context context){
        reply.append("```").append(cmdPrefix).append("create event <name> - creates an event\n\n")
                .append(cmdPrefix).append("set commandstring <new cmdstring> - sets the command prefix\n\n")
                .append(cmdPrefix).append("set dateformatstring <new dateFormatString> - sets the date formatting pattern. See Java Date documentation\n\n")
                .append(cmdPrefix).append("set cleanup <on/off> - when user message deletion is on, the bot will automatically clean up commands from users\n\n")
                .append(cmdPrefix).append("set event description/location/start/end <\"event name\"> <\"arg\"> - modify various attributes of an event\n\n")
                .append(cmdPrefix).append("remove event <event name> - removes a specified event\n\n")
                .append(cmdPrefix).append("list events - lists all currently active events\n\n")
                .append(cmdPrefix).append("view event <event name> - view all event information\n\n")
                .append(cmdPrefix).append("ping <\"event name\"> <\"message\"> - pings all users going to an event with the specified message")
                .append("```\n");
        return true;
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
            String name = familiarizeName(g, u);
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

    private CalendarEvent[] listEvents(){
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
            reply.append("```\n");
        } else {
            reply.append("No events to display\n");
            return null;
        }
        return events;
    }

    private boolean listEvents(Context c){
        listEvents();
        return true;
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

    private boolean removeEvent(Context c){
        try{
            calendar.remove(c.event.getName());
            persistence.addObject("calendar", calendar);
            reply.append("Removed event `").append(c.event.getName()).append("`\n");
            logger.info("Removed event: " + c.event);
        } catch (Exception e){
            reply.append("Unable to remove event, see log");
            logger.error("Problem removing message: ", e);
        }
        return true;
    }

    private boolean rsvpForEvent(Context c){
        User user = c.getUser();
        boolean going = c.rsvpGoing;
        if(going) {
            c.event.rsvpGoing(user);
            persistence.addObject("calendar", calendar);
            reply.append("You have successfully rsvp'd for `").append(c.event.getName()).append("`\n");
            logger.info("User " + user + " has rsvp'd 'going' for event " + c.event);
        } else {
            c.event.rsvpNotGoing(user);
            persistence.addObject("calendar", calendar);
            reply.append("You have successfully anti-rsvp'd for `").append(c.event.getName()).append("`\n");
            logger.info("User " + user + " has rsvp'd 'not going' for event " + c.event);
        }
        return true;
    }

    private void updatePlayingMessage(Presence p){
        p.setGame(Game.playing(cmdPrefix + "help for cmd list"));
    }
}
