import controller.CalendarListenerImpl;
import controller.VotingListenerImpl;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import net.dv8tion.jda.core.utils.SimpleLog.Level;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Joshua Owens on 1/30/2017.
 */
public class BotRunner {
    private static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-d");
    private static SimpleLog.LogListener logListener = new SimpleLog.LogListener() {
        Calendar lastMessage = Calendar.getInstance();
        @Override
        public void onLog(SimpleLog simpleLog, Level level, Object o) {
            Calendar now = Calendar.getInstance();
            if(now.get(Calendar.DATE) > lastMessage.get(Calendar.DATE)
                    || now.get(Calendar.MONTH) > lastMessage.get(Calendar.MONTH)
                    || now.get(Calendar.YEAR) > lastMessage.get(Calendar.YEAR)){
                try {
                    SimpleLog.removeFileLog(new File(format.format(lastMessage.getTime()) + ".log"));
                    File file = new File(format.format(now.getTime()) + ".log");
                    SimpleLog.addFileLogs(file, file);
                    lastMessage = now;
                } catch (IOException e) {
                    SimpleLog.getLog("Main").fatal("Could not create new log file: " + e.getMessage());
                }
            }
        }

        @Override
        public void onError(SimpleLog simpleLog, Throwable throwable) {
            onLog(simpleLog, Level.FATAL, throwable);
        }
    };
    public static void main(String[] args){
        SimpleLog.addListener(logListener);
        SimpleLog logger = SimpleLog.getLog("Main");
        try{
            File file = new File(format.format(new Date()) + ".log");
            String str = file.getAbsolutePath();
            if(!file.exists()) {
                file.createNewFile();
            }
            SimpleLog.addFileLogs(file, file);
            File prefs = new File("apitoken");
            if(!prefs.exists()){
                logger.warn("API Token not found, exiting...");
                System.exit(-1);
            }
            BufferedReader reader = new BufferedReader(new FileReader(prefs));
            String token = reader.readLine();
            if(token == null){
                logger.warn("API Token empty, exiting...");
                System.exit(-1);
            }
            JDA jda = new JDABuilder(AccountType.BOT).setToken(token).buildBlocking();

            jda.addEventListener(new CalendarListenerImpl());
        } catch(LoginException | InterruptedException | RateLimitedException | IOException e){
            logger.log(Level.WARNING, e.getMessage());
        }

    }

}
