package model;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class Session {
    String flag;
    private Context context;
    private Date creationTime;

    public Session(Context c, String flag){
        context = c;
        creationTime = new Date();
        this.flag = flag;
    }

    public Date getCreationTime(){
        return creationTime;
    }

    public Context getContext(){
        return context;
    }

    public String getFlag(){
        return flag;
    }

    public void setFlag(String flag){
        this.flag = flag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(context.getSignature(), session.context.getSignature()) &&
                Objects.equals(creationTime, session.creationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context.getSignature(), creationTime);
    }
}
