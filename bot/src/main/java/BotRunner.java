import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import model.ListenerImpl;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Joshua Owens on 1/30/2017.
 */
public class BotRunner {

    public static void main(String[] args){
        try{
            JDA jda = new JDABuilder(AccountType.BOT).setToken("MjM2NTgzNTQ3MDkwMTA4NDI3.C3Gliw.d9LaQFStsXF7O0MTYxlnKyUs6Dc").buildBlocking();
            jda.addEventListener(new ListenerImpl());


        } catch(LoginException e){
            e.printStackTrace();
        } catch(InterruptedException e){
            e.printStackTrace();
        } catch(RateLimitedException e){
            e.printStackTrace();
        }

    }

}
