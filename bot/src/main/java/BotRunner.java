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

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by Joshua Owens on 1/30/2017.
 */
public class BotRunner {
    static Logger logger = Logger.getLogger("BotLogger");
    public static void main(String[] args){
        try{
            FileHandler handler = new FileHandler("bot.log", true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);

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
            logger.log(Level.SEVERE, e.getMessage());
        }

    }

}
