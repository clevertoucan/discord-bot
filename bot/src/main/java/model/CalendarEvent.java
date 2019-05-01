package model;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class CalendarEvent implements Comparable<Date> {
    private String name, description, location;
    private User creator;
    private Date start, end;
    private HashSet<User> attendees, absentees;

    public CalendarEvent(User c, String n){
        creator = c;
        name = n;
        attendees = new HashSet<>();
        absentees = new HashSet<>();
    }


    public void rsvpGoing(User user){
        absentees.remove(user);
        attendees.add(user);
    }

    public void rsvpNotGoing(User user){
        attendees.remove(user);
        absentees.add(user);
    }

    public User[] getAttendees(){
        User[] x = new User[attendees.size()];
        attendees.toArray(x);
        return x;
    }

    public User[] getAbsentees(){
        User[] x = new User[absentees.size()];
        absentees.toArray(x);
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

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
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
        return name;
    }

    @Override
    public int compareTo(@NotNull Date o) {
        return start.compareTo(o);
    }
}
