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
            if(checkForChanges()){
                logger.info("Remote master is ahead of local. Pulling and restarting...");
                pullAndRestart();
            }
            logger.info("Local is caught up with remote");
            //File prefs = new File("apitoken");
            File prefs = new File("dev-token");
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

    public static void pullAndRestart(){
        try {
            Process process = Runtime.getRuntime().exec("git pull");
            process.waitFor();
            process = Runtime.getRuntime().exec("mvn package");
            process.waitFor();
            if(process.exitValue() == 0) {
                Runtime.getRuntime().exec("java -jar target/discord-bot*.jar");
                System.exit(0);
            }
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }

    public static boolean checkForChanges(){
        try {
            Process localCheck = Runtime.getRuntime().exec("git rev-list --count master");
            Process remoteCheck = Runtime.getRuntime().exec("git rev-list --count origin/master");
            StringBuilder localOutput = new StringBuilder();
            StringBuilder remoteOutput = new StringBuilder();
            BufferedReader localReader = new BufferedReader(new InputStreamReader(localCheck.getInputStream()));
            BufferedReader remoteReader = new BufferedReader(new InputStreamReader(remoteCheck.getInputStream()));
            String line;
            while ((line = localReader.readLine()) != null) {
                localOutput.append(line);
            }
            while((line = remoteReader.readLine()) != null){
                remoteOutput.append(line);
            }
            localCheck.waitFor();
            remoteCheck.waitFor();

            int localChanges = Integer.parseInt(localOutput.toString());
            int remoteChanges = Integer.parseInt(remoteOutput.toString());
            return remoteChanges > localChanges;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

}
