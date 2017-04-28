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

/**
 * Created by Joshua Owens on 1/30/2017.
 */
public class BotRunner {
    private static SimpleLog logger = SimpleLog.getLog("Main");
    public static void main(String[] args){
        try{
            File file = new File("bot.log");
            if(!file.exists()) {
                file.createNewFile();
            }
            SimpleLog.addFileLogs(file, file);

            JDA jda = new JDABuilder(AccountType.BOT).setToken("MjM2NTgzNTQ3MDkwMTA4NDI3.C3Gliw.d9LaQFStsXF7O0MTYxlnKyUs6Dc").buildBlocking();
            ListenerImpl.globalJDA = jda;
            if(!ListenerImpl.loadData()) {
                ListenerImpl.setTargetGuild(jda.getGuildById("302810413647527936"));
                for (Guild guild : jda.getGuilds()) {
                    ListenerImpl.linkServerToRole(guild);
                }
            }

            jda.addEventListener(new ListenerImpl());
        } catch(LoginException | InterruptedException | RateLimitedException | IOException e){
            logger.log(Level.WARNING, e.getMessage());
        }

    }

}
