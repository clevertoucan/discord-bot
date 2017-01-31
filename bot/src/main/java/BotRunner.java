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
    private static Logger logger = Logger.getLogger("bot");
    public static void main(String[] args){
        try{
            JDA jda = new JDABuilder(AccountType.BOT).setToken("MjM2NTgzNTQ3MDkwMTA4NDI3.C3Gliw.d9LaQFStsXF7O0MTYxlnKyUs6Dc").buildBlocking();
            jda.addEventListener(new ListenerImpl());


        } catch(LoginException e){
            logger.log(Level.SEVERE,e.getMessage());
        } catch(InterruptedException e){
            logger.log(Level.SEVERE,e.getMessage());
        } catch(RateLimitedException e){
            logger.log(Level.SEVERE,e.getMessage());
        }

    }
}
