package model;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import runner.BotRunner;

public class CalendarEvent implements Comparable<CalendarEvent>, Serializable {
    private String name, description, location, creatorID;
    private Date start, end;
    private HashSet<String> attendees, absentees;

    public CalendarEvent(String cID, String n){
        creatorID = cID;
        name = n;
        attendees = new HashSet<>();
        absentees = new HashSet<>();

    }


    public void rsvpGoing(User user){
        absentees.remove(user.getId());
        attendees.add(user.getId());
    }

    public void rsvpNotGoing(User user){
        attendees.remove(user.getId());
        absentees.add(user.getId());
    }

    public User[] getAttendees(){
        User[] x = new User[attendees.size()];
        int i = 0;
        for(String s : attendees){
            User u = BotRunner.globalJDA.retrieveUserById(s).complete();
            x[i] = u;
            i++;
        }
        return x;
    }

    public User[] getAbsentees(){
        User[] x = new User[absentees.size()];
        int i = 0;
        for(String s : absentees){
            User u = BotRunner.globalJDA.retrieveUserById(s).complete();
            x[i] = u;
            i++;
        }
        return x;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String getLocation(){
        return location;
    }

    public void setLocation(String loc){
        location = loc;
    }

    @Override
    public String toString() {
        return "CalendarEvent{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", creatorID=" + creatorID +
                ", start=" + start +
                ", end=" + end +
                ", attendees=" + attendees +
                ", absentees=" + absentees +
                '}';
    }

    @Override
    public int compareTo(@NotNull CalendarEvent o) {
        return start.compareTo(o.start);
    }
}
