package model;

import java.util.HashMap;

public class GuildCalendar {
    HashMap<String, CalendarEvent> events;

    public GuildCalendar(){
        events = new HashMap<>();
    }

    public CalendarEvent get(String name){
        return events.get(name);
    }

    public void add(CalendarEvent e){
        events.put(e.getName(), e);
    }
}
