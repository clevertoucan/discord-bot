import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import model.ListenerImpl;
import model.MessageHandler;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import net.dv8tion.jda.core.utils.SimpleLog.Level;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Joshua Owens on 1/30/2017.
 */
public class BotRunner {
    private static SimpleDateFormat format = new SimpleDateFormat("MMM d, YYYY");
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

            JDA jda = new JDABuilder(AccountType.BOT).setToken("MzExOTk2MjAzODI1MTAyODUw.C_ZTVg.0EzSOe4tAjhJVGw7JtCqmAGUe6s").buildBlocking();
            ListenerImpl.globalJDA = jda;
            if(!ListenerImpl.loadData()) {
                ListenerImpl.setTargetGuild(jda.getGuildById("302810413647527936"));
                for (Guild guild : jda.getGuilds()) {
                    ListenerImpl.linkServerToRole(guild);
                }
            }

            jda.addEventListener(new ListenerImpl());
            jda.shutdown(false);
            jda = new JDABuilder(AccountType.BOT).setToken("MzExOTk2MjAzODI1MTAyODUw.C_ZTVg.0EzSOe4tAjhJVGw7JtCqmAGUe6s").buildBlocking();
            jda.addEventListener(new ListenerImpl());
        } catch(LoginException | InterruptedException | RateLimitedException | IOException e){
            logger.log(Level.WARNING, e.getMessage());
        }

    }

}
