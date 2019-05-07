package model;

import controller.CalendarListenerImpl;
import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.EventListener;

public class Alarm {
    private CalendarListenerImpl listener;

    public Alarm(Context c, CalendarListenerImpl listener){
        Logger logger = LoggerFactory.getLogger("Alarm");
        new Thread(new Runnable() {
            @Override
            public void run() {
                long delta = (c.event.getStart().getTime() - c.time) - new Date().getTime();
                if (delta > 0) {
                    try {
                        logger.info("Alarm registered for " + new Date(c.event.getStart().getTime() - c.time) + " for event: " + c.event);
                        Thread.sleep(delta);
                    } catch (InterruptedException e) {
                        run();
                    }
                }
                listener.pingScheduledEvent(c);
            }
        }).start();
    }
}
