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
import java.util.Objects;

import runner.BotRunner;

public class CalendarEvent implements Comparable<CalendarEvent>, Serializable {
    private String name, description, location;
    private UserWrapper creator;
    private Date start, end;
    private HashSet<UserWrapper> attendees, absentees;

    public CalendarEvent(User c, String n){
        creator = new UserWrapper(c);
        name = n;
        attendees = new HashSet<>();
        absentees = new HashSet<>();

    }


    public void rsvpGoing(User user){
        UserWrapper x = new UserWrapper(user);
        absentees.remove(x);
        attendees.add(x);
    }

    public void rsvpNotGoing(User user){
        UserWrapper x = new UserWrapper(user);
        attendees.remove(x);
        absentees.add(x);
    }

    public User[] getAttendees(){
        User[] x = new User[attendees.size()];
        int i = 0;
        for(UserWrapper w : attendees){
            x[i] = w.user;
            i++;
        }
        return x;
    }

    public User[] getAbsentees(){
        User[] x = new User[absentees.size()];
        int i = 0;
        for(UserWrapper w : absentees){
            x[i] = w.user;
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
                ", creator=" + creator +
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

    private class UserWrapper implements Serializable{
        private User user;

        UserWrapper(User u){
            user = u;
        }

        private void readObject(ObjectInputStream in) throws IOException {
            user = BotRunner.globalJDA.retrieveUserById(in.readUTF()).complete();
        }

        private void writeObject(ObjectOutputStream out) throws  IOException {
            out.writeUTF(user.getId());
        }

        @Override
        public String toString() {
            return "UserWrapper{" +
                    "user=" + user +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserWrapper that = (UserWrapper) o;
            return Objects.equals(user, that.user);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user);
        }
    }
}
