package model;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.audio.AudioConnection;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.audio.factory.DefaultSendFactory;
import net.dv8tion.jda.core.audio.factory.DefaultSendSystem;
import net.dv8tion.jda.core.audio.factory.IAudioSendSystem;
import net.dv8tion.jda.core.audio.factory.IPacketProvider;
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.RoleImpl;
import net.dv8tion.jda.core.entities.impl.VoiceChannelImpl;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by Joshua Owens on 1/30/2017.
 */
public class ListenerImpl extends ListenerAdapter {
    public static HashMap<Guild, Role> roleMap = new HashMap<Guild, Role>();
    public static Guild targetGuild;
    static Logger logger = Logger.getLogger("BotLogger");

    public void onGuildMemberJoin(GuildMemberJoinEvent event){
        if(event.getGuild().getId().equals(targetGuild.getId())) {
            Member member = event.getMember();
            JDA jda = member.getJDA();
            for(Guild guild: jda.getGuilds()){
                if(roleMap.containsKey(guild)){
                    logger.log(Level.INFO, "Added user: " + member.getEffectiveName()
                            + " from server: " + guild.getName()
                            + " to role: " + roleMap.get(guild).getName());
                    targetGuild.getController().addRolesToMember(member, roleMap.get(guild)).queue();
                }
            }
        }
    }

    public void onGuildMemberLeave(GuildMemberLeaveEvent event){
        User user = event.getMember().getUser();
        Guild guild = event.getGuild();
        if(!guild.getId().equals(targetGuild.getId())) {
            targetGuild.getController().
                    removeRolesFromMember(targetGuild.getMember(user), roleMap.get(guild)).queue();
        }
    }

    public void onGuildJoin(GuildJoinEvent event){
        linkServerToRole(event.getGuild());
    }

    public void onMessageReceived(MessageReceivedEvent event){
        String[] message = event.getMessage().getContent().split(" ");

        if(message[0].charAt(0) == '!'){
            StringBuilder reply = new StringBuilder();
            if(message[0].equals("!setunitednations")){
                if(event.getAuthor().getId().equals(targetGuild.getOwner().getUser().getId())){
                    if(message.length > 1){
                        for(int i = 2; i < message.length; i++){
                            message[1] += " " + message[2];
                        }
                        for(Guild guild: roleMap.keySet()){
                            if(guild.getName().equals(message[1])){
                                setTargetGuild(guild);
                                return;
                            }
                        }
                    } else {
                        reply.append("Missing server name.\n");
                    }
                } else {
                    reply.append("This command must be run by the server owner.\n");
                }
            }
        }
    }

    private static File file = new File("C:\\Users\\scary\\programing\\discord-bot\\bot\\src\\main\\resources\\config.preferences");

    void setTargetGuild(Guild guild){
        try{
            FileOutputStream out = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(out);
            BufferedWriter bwriter = new BufferedWriter(writer);

            bwriter.write(guild.getId());
        } catch (FileNotFoundException e){
            logger.log(Level.WARNING, "Save file not found, data will not persist!");
        } catch (IOException e){
            logger.log(Level.WARNING, e.getMessage());
        }
        targetGuild = guild;
    }

    public static Guild getTargetGuild(JDA jda){
        try {
            FileInputStream in = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader breader = new BufferedReader(reader);

            String id = breader.readLine();
            for(Guild guild: jda.getGuilds()){
                if(guild.getId().equals(id)){
                    return guild;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean linkServerToRole(Guild guild){
        if(!guild.getId().equals(targetGuild.getId())){
            if(!roleMap.containsKey(guild)){
                try {
                    Role role = targetGuild.getController().createRole().block();
                    role.getManager().setName(guild.getName()).block();
                    roleMap.put(guild, role);
                    logger.log(Level.INFO, "Adding role: " + role.getName()
                            + "From guild: " + guild.getName());
                    return true;
                } catch (RateLimitedException e) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
            } else {
                roleMap.put(guild,
                        targetGuild.getRolesByName(guild.getName(), true).get(0));
                return true;
            }
        }
        return false;
    }
}
