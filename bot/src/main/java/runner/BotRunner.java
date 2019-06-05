package runner;

import controller.CalendarListenerImpl;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.JDALogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static JDA globalJDA;
    public static void main(String[] args){
        Logger logger = LoggerFactory.getLogger("runner.BotRunner");

        try{
            /*
            File file = new File(format.format(new Date()) + ".log");
            String str = file.getAbsolutePath();
            if(!file.exists()) {
                file.createNewFile();
            }
            */
            File prefs = new File("apitoken");
            //File prefs = new File("dev-token");
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
            globalJDA = new JDABuilder(token).build().awaitReady();
            
            globalJDA.addEventListener(new CalendarListenerImpl());
        } catch (Exception e){
            logger.error("CRITICAL - Unspecified error", e);
        }

    }

}
