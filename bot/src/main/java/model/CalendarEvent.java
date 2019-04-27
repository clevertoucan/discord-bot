package model;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.Date;

public class CalendarEvent {
    String name, description;
    User creator;
    Date start, end;

    public CalendarEvent(User c, String n){
        creator = c;
        name = n;
    }
}
