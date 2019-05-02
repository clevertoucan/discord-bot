package model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

public class GuildCalendar implements Serializable {
    private HashMap<String, CalendarEvent> events;

    public GuildCalendar(){
        events = new HashMap<>();
    }

    public CalendarEvent get(String name){
        return events.get(name);
    }

    public void add(CalendarEvent e){
        events.put(e.getName(), e);
    }

    public void remove(String name){
        events.remove(name);
    }

    public CalendarEvent[] toArr(){
        Collection<CalendarEvent> values = events.values();
        CalendarEvent[] returnValue = new CalendarEvent[values.size()];
        values.toArray(returnValue);
        return returnValue;
    }

    public boolean contains(String s){
        return events.containsKey(s);
    }

    public int size(){
        return events.size();
    }

    @Override
    public String toString() {
        return "GuildCalendar{" +
                "events=" + events +
                '}';
    }
}
