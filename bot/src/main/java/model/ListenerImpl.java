package model;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.TextChannelImpl;
import net.dv8tion.jda.core.entities.impl.UserImpl;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.SimpleLog;
import net.dv8tion.jda.core.utils.SimpleLog.Level;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Joshua Owens on 1/30/2017.
 * Main entrypoint for processing events across all connected servers
 */
public class ListenerImpl extends ListenerAdapter {
    public static JDA globalJDA;
    private static HashMap<Guild, Role> roleMap = new HashMap<Guild, Role>();
    private static HashMap<Role, Set<Channel>> associates = new HashMap<Role, Set<Channel>>();
    private static HashMap<String, Role> specialRoles = new HashMap<>();
    private static HashMap<String, Channel> specialChannels = new HashMap<>();
    private static Guild targetGuild;
    private static SimpleLog logger = SimpleLog.getLog("Listener");
    private static MessageChannel out;

    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild().equals(targetGuild)) {
            Member member = event.getMember();
            LinkedList<Role> addedRoles = new LinkedList<>();
            for (Guild guild : roleMap.keySet()) {
                if (guild.getMember(member.getUser()) != null) {
                    try {
                        logger.log(Level.INFO, "Added user: " + member.getEffectiveName()
                                + " from server: " + guild.getName()
                                + " to role: " + roleMap.get(guild).getName());
                        addedRoles.add(roleMap.get(guild));
                        if (guild.getOwner().equals(guild.getMember(member.getUser()))) {
                            Role modRole = specialRoles.get("world-leader");
                            if (modRole != null) {
                                addedRoles.add(modRole);
                            }
                        }
                    }catch (PermissionException e){
                        out.sendMessage("Attempted to add " + guild.getOwner().getUser().getName() + " to "
                                + specialRoles.get("world-leader").getName() +
                                ", but the bot doesn't have the required permission level.").queue();
                        logger.warn(e.getMessage());
                    }
                }
            }
            targetGuild.getController().addRolesToMember(member, addedRoles).queue();
        } else if(roleMap.containsKey(event.getGuild())){
            Member member = targetGuild.getMember(event.getMember().getUser());
            targetGuild.getController().addRolesToMember(member, roleMap.get(event.getGuild())).queue();
        }
    }

    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        User user = event.getMember().getUser();
        Guild guild = event.getGuild();
        if (!guild.equals(targetGuild) && !user.equals(globalJDA.getSelfUser())) {
            targetGuild.getController().
                    removeRolesFromMember(targetGuild.getMember(user), roleMap.get(guild)).queue();
        }
    }

    public void onGuildJoin(GuildJoinEvent event) {
        //TODO assign roles to users on join
        if(linkServerToRole(event.getGuild())){
            try {
                TextChannel textChannel= targetGuild.getController().
                        createTextChannel(
                                event.getGuild()
                                        .getName().replace(' ', '-')
                                        .toLowerCase() + "-delegation"
                        ).block();
                VoiceChannel voiceChannel = targetGuild.getController()
                        .createVoiceChannel(
                                event.getGuild()
                                .getName() + " Caucus"
                        ).block();
                linkChannelToRole(textChannel, roleMap.get(event.getGuild()));
                linkChannelToRole(voiceChannel, roleMap.get(event.getGuild()));
                Member owner = targetGuild.getMember(event.getGuild().getOwner().getUser());
                if(owner != null){
                    Role role = specialRoles.get("world-leader");
                    if(role != null) {
                        try {
                            targetGuild.getController().addRolesToMember(owner, role).block();
                        } catch (PermissionException e){
                            out.sendMessage("Attempted to add " + owner.getUser().getName() + " to " + role.getName() +
                                    ", but the bot doesn't have the required permission level.").queue();
                        }
                    }
                }
                out.sendMessage( event.getGuild().getName()
                        + " has joined the United Nations. \n" +
                        "```" + getAssociationsString(event.getGuild()) + "```").queue();

                logger.info("The server " + event.getGuild().getName() + " has added the bot to their server.");
            } catch (RateLimitedException e) {
                logger.log(Level.FATAL, e.getMessage());
            }
        }

    }

    public void onGuildLeave(GuildLeaveEvent event) {
        delinkServerFromRole(event.getGuild());
        targetGuild.getPublicChannel().sendMessage("@everyone " + event.getGuild().getName()
                + " has left the United Nations. " +
                "The associated role and channels have been deleted.\n");
        logger.info("The server " + event.getGuild().getName() +" has removed the bot from their server.");
    }

    public void onMessageReceived(MessageReceivedEvent event) {
        Channel botChannel = specialChannels.get("bot-commands");
        if (botChannel == null || event.getChannel().equals(botChannel)) {
            if (!event.getAuthor().equals(globalJDA.getSelfUser())) {
                String[] message = event.getMessage().getContent().split(" +");
                Member member = targetGuild.getMember(event.getAuthor());
                if (message.length > 0) {
                    if (message[0].charAt(0) == '!') {
                        StringBuilder reply = new StringBuilder();

                        switch (message[0]) {
                            case "!linkchannel":
                                if (member.getRoles().contains(specialRoles.get("moderator"))
                                        || member.getRoles().contains(specialRoles.get("admin"))
                                        || member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                                    if (message.length >= 3) {
                                        List<TextChannel> channels = event.getMessage().getMentionedChannels();
                                        List<Role> roles = event.getMessage().getMentionedRoles();

                                        int i = 2;
                                        if (message[1].charAt(0) == '\"') {
                                            message[1] = message[1].substring(1);
                                            if (message[1].charAt(message[1].length() - 1) == '\"') {
                                                message[1] = message[1].substring(0, message[1].length() - 1);
                                            } else {
                                                for (; i < message.length; i++) {
                                                    if (message[i].charAt(message[i].length() - 1) == '\"') {
                                                        message[i] = message[i].substring(0, message[i].length() - 1);
                                                        message[1] += " " + message[i];
                                                        i++;
                                                        message[2] = message[i];
                                                        break;
                                                    }
                                                    message[1] += " " + message[i];
                                                }
                                            }
                                        }

                                        if (message[2].charAt(0) == '\"') {
                                            message[2] = message[2].substring(1);
                                            if (message[2].charAt(message[2].length() - 1) == '\"') {
                                                message[2] = message[2].substring(0, message[2].length() - 1);
                                            } else {
                                                i++;
                                                for (; i < message.length; i++) {
                                                    if (message[i].charAt(message[i].length() - 1) == '\"') {
                                                        message[i] = message[i].substring(0, message[i].length() - 1);
                                                        message[2] += " " + message[i];
                                                        break;
                                                    }
                                                    message[2] += " " + message[i];
                                                }
                                            }
                                        }
                                        try {
                                            boolean success;
                                            Role role;
                                            if (roles.size() > 0) {
                                                role = roles.get(0);
                                            } else {
                                                role = targetGuild.getRolesByName(message[2], true).get(0);
                                            }
                                            if (channels.size() > 0) {
                                                for (Channel c : channels) {
                                                    success = linkChannelToRole(c, role);
                                                    if (success) {
                                                        logger.info("Linked Channel: " + message[1] + " to role: " + role.getName()
                                                                + " at the request of user: " + event.getAuthor().getName());
                                                        reply = reply.append("Linked Channel: ").append(c.getName()).append(" to role: ")
                                                                .append(role.getName()).append("\n");
                                                    }
                                                }
                                            } else {
                                                List<TextChannel> channelList;
                                                List<VoiceChannel> voiceChannels;
                                                if ((channelList = targetGuild.getTextChannelsByName(message[1], true)).size() > 0) {
                                                    success = linkChannelToRole(channelList.get(0), role);
                                                } else if ((voiceChannels = targetGuild.getVoiceChannelsByName(message[1], true)).size() > 0) {
                                                    success = linkChannelToRole(voiceChannels.get(0), role);
                                                } else {
                                                    success = false;
                                                    logger.log(Level.WARNING, "Something weird happened");
                                                }
                                                if (success) {
                                                    logger.info("Linked Channel: " + message[1] + " to role: " + role.getName()
                                                            + " at the request of user: " + event.getAuthor().getName());
                                                    reply = reply.append("Linked Channel: ").append(message[1]).append(" to role: ")
                                                            .append(role.getName()).append("\n");
                                                }
                                            }
                                        } catch (IndexOutOfBoundsException e) {
                                            event.getChannel().sendMessage("Couldn't parse role or channel").queue();
                                            logger.info(e.getMessage());
                                        }
                                    } else {
                                        reply = reply.append("linkchannel needs a role and at lease one channel to link.\n")
                                                .append("```Usage:\n\tlinkchannel <\"channel_name\"> <\"role_name\">")
                                                .append("\n\tlinkchannel <#text-channel1 #text-channel2...> <\"role_name\"")
                                                .append("\n\tlinkchannel <\"channel_name\"> <@role_name>")
                                                .append("\n\tlinkchannel <#text-channel1 #text-channel2...> <@role_name>```");
                                    }
                                } else {
                                    reply = reply.append("You must have a role of ").append(specialRoles.get("moderator").getName())
                                            .append(" to use that command.");
                                }
                                break;
                            //*********** Delink *********/
                            case "!delink":
                                if (member.getRoles().contains(specialRoles.get("moderator"))
                                        || member.getRoles().contains(specialRoles.get("admin"))
                                        || member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                                    if (message.length >= 3) {
                                        List<TextChannel> channels = event.getMessage().getMentionedChannels();
                                        List<Role> roles = event.getMessage().getMentionedRoles();

                                        int i = 2;
                                        if (message[1].charAt(0) == '\"') {
                                            message[1] = message[1].substring(1);
                                            if (message[1].charAt(message[1].length() - 1) == '\"') {
                                                message[1] = message[1].substring(0, message[1].length() - 1);
                                            } else {
                                                for (; i < message.length; i++) {
                                                    if (message[i].charAt(message[i].length() - 1) == '\"') {
                                                        message[i] = message[i].substring(0, message[i].length() - 1);
                                                        message[1] += " " + message[i];
                                                        i++;
                                                        message[2] = message[i];
                                                        break;
                                                    }
                                                    message[1] += " " + message[i];
                                                }
                                            }
                                        }

                                        if (message[2].charAt(0) == '\"') {
                                            message[2] = message[2].substring(1);
                                            if (message[2].charAt(message[2].length() - 1) == '\"') {
                                                message[2] = message[2].substring(0, message[2].length() - 1);
                                            } else {
                                                i++;
                                                for (; i < message.length; i++) {
                                                    if (message[i].charAt(message[i].length() - 1) == '\"') {
                                                        message[i] = message[i].substring(0, message[i].length() - 1);
                                                        message[2] += " " + message[i];
                                                        break;
                                                    }
                                                    message[2] += " " + message[i];
                                                }
                                            }
                                        }
                                        try {
                                            boolean success;
                                            Role role;
                                            if (roles.size() > 0) {
                                                role = roles.get(0);
                                            } else {
                                                role = targetGuild.getRolesByName(message[2], true).get(0);
                                            }
                                            if (channels.size() > 0) {
                                                for (Channel c : channels) {
                                                    success = delinkChannelFromRole(c, role);
                                                    if (success) {
                                                        logger.info("Delinked Channel: " + message[1] + " from role: " + role.getName()
                                                                + " at the request of user: " + event.getAuthor().getName());
                                                        reply = reply.append("Delinked Channel: ").append(c.getName()).append(" from role: ")
                                                                .append(role.getName()).append("\n");
                                                    }
                                                }
                                            } else {
                                                List<TextChannel> channelList;
                                                List<VoiceChannel> voiceChannels;
                                                if ((channelList = targetGuild.getTextChannelsByName(message[1], true)).size() > 0) {
                                                    success = delinkChannelFromRole(channelList.get(0), role);
                                                } else if ((voiceChannels = targetGuild.getVoiceChannelsByName(message[1], true)).size() > 0) {
                                                    success = delinkChannelFromRole(voiceChannels.get(0), role);
                                                } else {
                                                    success = false;
                                                    logger.log(Level.WARNING, "Something weird happened");
                                                }
                                                if (success) {
                                                    logger.info("Delinked Channel: " + message[1] + " from role: " + role.getName()
                                                            + " at the request of user: " + event.getAuthor().getName());
                                                    reply = reply.append("Delinked Channel: ").append(message[1]).append(" from role: ")
                                                            .append(role.getName()).append("\n");
                                                }
                                            }
                                        } catch (IndexOutOfBoundsException e) {
                                            logger.log(Level.WARNING, "Role or channel not found");
                                            e.printStackTrace();
                                        }
                                    } else {
                                        reply = reply.append("delink needs a role and at lease one channel to link.\n")
                                                .append("```Usage:\n\tdelink <\"channel_name\"> <\"role_name\">")
                                                .append("\n\tdelink <#text-channel1 #text-channel2...> <\"role_name\"")
                                                .append("\n\tdelink <\"channel_name\"> <@role_name>")
                                                .append("\n\tdelink <#text-channel1 #text-channel2...> <@role_name>```");
                                    }
                                } else {
                                    reply = reply.append("You must have a role of ").append(specialRoles.get("moderator").getName())
                                            .append(" to use that command.");
                                }
                                break;
                            //********* List Servers ********/
                            case "!servers":
                                reply = reply.append(String.format("```United Nations Host Server: %4s\n\n", targetGuild.getName()));
                                for (Guild guild : globalJDA.getGuilds()) {
                                    if (!guild.equals(targetGuild)) {
                                        reply = reply.append(getAssociationsString(guild));
                                    }
                                }
                                reply = reply.append("Special Channels:");
                                if (specialChannels.size() > 0) {
                                    for (Map.Entry<String, Channel> entry : specialChannels.entrySet()) {
                                        reply.append("\n\tFlag: ").append(entry.getKey()).append(", Channel: ").append(entry.getValue().getName());
                                    }
                                } else {
                                    reply = reply.append("   None");
                                }
                                reply = reply.append("\n\nSpecial Roles:");
                                if (specialRoles.size() > 0) {
                                    for (Map.Entry<String, Role> entry : specialRoles.entrySet()) {
                                        reply.append("\n\tFlag: ").append(entry.getKey()).append(", Role: ").append(entry.getValue().getName());
                                    }
                                } else {
                                    reply = reply.append("\t  None");
                                }
                                logger.info("Sent server list in channel: " + event.getChannel().getName() +
                                        " at request of user: " + event.getAuthor().getName());
                                reply = reply.append("```");
                                break;
                            //********** Save ****************/
                            case "!save":
                                logger.info("Save requested in: " + event.getChannel().getName() +
                                        " by user: " + event.getAuthor().getName());
                                if (saveData()) {
                                    reply = reply.append("Save successful.");
                                } else {
                                    reply = reply.append("Save unsuccessful.");
                                }
                                break;
                            case "!logs":
                                File file = new File("bot.log");
                                if (file.exists()) {
                                    try {
                                        logger.info("Sent logfile located at: " + file.getAbsolutePath() +
                                                " in channel: " + event.getChannel().getName() +
                                                " at request of user: " + event.getAuthor().getName());
                                        event.getChannel().sendFile(file, null).queue();
                                    } catch (IOException e) {
                                        logger.warn(e.getMessage());
                                    }
                                }
                                break;
                            case "!help":
                                reply = reply.append("```Available commands: ")
                                        .append("\n\n!linkchannel <Voice/Text Channel> <Role> " +
                                                "\n\tLinks a voice or text channel to a UN role")
                                        .append("\n\n!delinkchannel <Voice/Text Channel> <Role> " +
                                                "\n\tRemoves a voice or text channel from a UN role")
                                        .append("\n\n!setspecialrole <flag> <Role>")
                                        .append("\n\tAssign a flag to a role")
                                        .append("\n\tCurrently supported flags:")
                                        .append("\n\tadmin - server administrators")
                                        .append("\n\tmoderator - server moderators")
                                        .append("\n\tbot - bots")
                                        .append("\n\n!setspecialchannel <flag> <Channel>")
                                        .append("\n\tAssign a flag to a channel")
                                        .append("\n\tCurrently supported flags:")
                                        .append("\n\tbot-commands - designate a channel specifically for bot commands")
                                        .append("\n\n!servers\n\tLists the servers currently connected " +
                                                "to the UN")
                                        .append("\n\n!logs\n\tRetrieves the logfile of this bot")
                                        .append("\n\n!save\n\tSaves the current associations (mostly used when things go wrong)")
                                        .append("```");

                                break;

                            case "!info":
                                reply = reply.append("```This bot was created to help manage the UN discord server. This bot will:" +
                                        "\n\n-Assign users to roles based on the linked servers they are a part of" +
                                        "\n\n-Create roles and dummy channels for any server that adds the bot to their discord" +
                                        "\n\n-Remove roles from users who leave the linked servers" +
                                        "\n\n-Delete channels and roles associated with linked discord servers if those servers kicks " +
                                        "the bot or the server is deleted" +
                                        "```");
                                break;
                            case "!shutdown":
                                event.getChannel().sendMessage("Bye bye!").queue();
                                logger.info("Shutdown requested by " + event.getAuthor().getName());
                                saveData();
                                System.exit(1);

                                break;

                            case "!setspecialchannel":
                                if (member.getRoles().contains(specialRoles.get("moderator"))
                                        || member.getRoles().contains(specialRoles.get("admin"))
                                        || member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                                    int i = 2;
                                    if (message[1].charAt(0) == '\"') {
                                        message[1] = message[1].substring(1);
                                        if (message[1].charAt(message[1].length() - 1) == '\"') {
                                            message[1] = message[1].substring(0, message[1].length() - 1);
                                        } else {
                                            for (; i < message.length; i++) {
                                                if (message[i].charAt(message[i].length() - 1) == '\"') {
                                                    message[i] = message[i].substring(0, message[i].length() - 1);
                                                    message[1] += " " + message[i];
                                                    i++;
                                                    message[2] = message[i];
                                                    break;
                                                }
                                                message[1] += " " + message[i];
                                            }
                                        }
                                    }
                                    List<TextChannel> channels = event.getMessage().getMentionedChannels();
                                    TextChannel channel = null;
                                    if (channels.size() > 0) {
                                        channel = channels.get(0);
                                    } else {
                                        if (message[2].charAt(0) == '\"') {
                                            message[2] = message[2].substring(1);
                                            if (message[2].charAt(message[2].length() - 1) == '\"') {
                                                message[2] = message[2].substring(0, message[2].length() - 1);
                                            } else {
                                                i++;
                                                for (; i < message.length; i++) {
                                                    if (message[i].charAt(message[i].length() - 1) == '\"') {
                                                        message[i] = message[i].substring(0, message[i].length() - 1);
                                                        message[2] += " " + message[i];
                                                        break;
                                                    }
                                                    message[2] += " " + message[i];
                                                }
                                            }
                                        }
                                        channels = targetGuild.getTextChannelsByName(message[2], true);
                                        if (channels.size() > 0) {
                                            channel = channels.get(0);
                                        }
                                    }
                                    if (channel != null) {
                                        specialChannels.put(message[1], channel);
                                        saveData();
                                        reply = reply.append("Added flag ").append(message[1]).append(" to channel ").append(channel.getName());
                                        logger.info("Added flag " + message[1] + " to channel " + channel.getName());
                                    } else {
                                        reply = reply.append("Could not parse the role specified");
                                    }
                                } else {
                                    reply = reply.append("You must have a role of ").append(specialRoles.get("moderator").getName())
                                            .append(" to use that command.");
                                }
                                break;
                            case "!setspecialrole":
                                if (member.getRoles().contains(specialRoles.get("moderator"))
                                        || member.getRoles().contains(specialRoles.get("admin"))
                                        || member.getPermissions().contains(Permission.ADMINISTRATOR)) {
                                    int i = 2;
                                    if (message[1].charAt(0) == '\"') {
                                        message[1] = message[1].substring(1);
                                        if (message[1].charAt(message[1].length() - 1) == '\"') {
                                            message[1] = message[1].substring(0, message[1].length() - 1);
                                        } else {
                                            for (; i < message.length; i++) {
                                                if (message[i].charAt(message[i].length() - 1) == '\"') {
                                                    message[i] = message[i].substring(0, message[i].length() - 1);
                                                    message[1] += " " + message[i];
                                                    i++;
                                                    message[2] = message[i];
                                                    break;
                                                }
                                                message[1] += " " + message[i];
                                            }
                                        }
                                    }
                                    List<Role> roles = event.getMessage().getMentionedRoles();
                                    Role role = null;
                                    if (roles.size() > 0) {
                                        role = roles.get(0);
                                    } else {
                                        if (message[2].charAt(0) == '\"') {
                                            message[2] = message[2].substring(1);
                                            if (message[2].charAt(message[2].length() - 1) == '\"') {
                                                message[2] = message[2].substring(0, message[2].length() - 1);
                                            } else {
                                                i++;
                                                for (; i < message.length; i++) {
                                                    if (message[i].charAt(message[i].length() - 1) == '\"') {
                                                        message[i] = message[i].substring(0, message[i].length() - 1);
                                                        message[2] += " " + message[i];
                                                        break;
                                                    }
                                                    message[2] += " " + message[i];
                                                }
                                            }
                                        }
                                        roles = targetGuild.getRolesByName(message[2], true);
                                        if (roles.size() > 0) {
                                            role = roles.get(0);
                                        }
                                    }
                                    if (role != null) {
                                        specialRoles.put(message[1], role);
                                        saveData();
                                        reply = reply.append("Added flag ").append(message[1]).append(" to role ").append(role.getName());
                                        logger.info("Added flag " + message[1] + " to role " + role.getName());
                                    } else {
                                        reply = reply.append("Could not parse the role specified");
                                    }
                                } else {
                                    reply = reply.append("You must have a role of ").append(specialRoles.get("moderator").getName())
                                            .append(" to use that command.");
                                }
                                break;
                            //TODO: !clear, !beef

                        }
                        if (!reply.toString().isEmpty()) {
                            event.getChannel().sendMessage(reply.toString()).queue();
                        }
                    }
                }
            }
        }
    }

    private String getAssociationsString(Guild guild){
        StringBuilder reply = new StringBuilder();
        reply = reply.append(String.format("Server Name:\t\t%s\n", guild.getName()))
                .append(String.format("Designated Role:\t%s\n", roleMap.get(guild).getName()))
                .append(String.format("Leader:\t\t\t %s\n", guild.getOwner().getUser().getName()))
                .append("Linked Channels:\t");
        if (associates.size() > 0) {
            if (associates.get(roleMap.get(guild)) != null) {
                reply = reply.append("\n");
                for (Channel channel : associates.get(roleMap.get(guild))) {
                    reply = reply.append(String.format("\t%s\n", channel.getName()));
                }
            } else {
                reply = reply.append("None\n");
            }
        } else {
            reply = reply.append("None\n");
        }
        reply = reply.append("\n");
        return reply.toString();
    }

    public static boolean setTargetGuild(Guild guild){
        targetGuild = guild;
        return saveData();
    }

    private boolean delinkChannelFromRole(Channel channel, Role role){
        try {
            PermissionOverride allPerm = channel.getPermissionOverride(targetGuild.getPublicRole());
            if(allPerm== null){
                allPerm = channel.createPermissionOverride(targetGuild.getPublicRole()).block();
            }
            PermissionOverride perm = channel.getPermissionOverride(role);
            if(perm == null){
                perm = channel.createPermissionOverride(role).block();
            }
            if(channel instanceof TextChannel) {
                allPerm.getManager().grant(Permission.MESSAGE_READ).queue();
                perm.delete().queue();
            } else if(channel instanceof VoiceChannel){
                allPerm.getManager().grant(Permission.VOICE_CONNECT).queue();
                perm.delete().queue();
            } else {
                logger.log(Level.WARNING, "Channel is not voice or text channel");
                return false;
            }
            Set<Channel> channels = associates.get(role);
            if(channels != null){
                channels.remove(channel);
            }
            saveData();
            return true;
        } catch (RateLimitedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean linkChannelToRole(Channel channel, Role role){
        try {
            PermissionOverride allPerm = channel.getPermissionOverride(targetGuild.getPublicRole());
            if(allPerm== null){
                allPerm = channel.createPermissionOverride(targetGuild.getPublicRole()).block();
            }
            PermissionOverride perm = channel.getPermissionOverride(role);
            if(perm == null){
                perm = channel.createPermissionOverride(role).block();
            }
            if(channel instanceof TextChannel) {
                allPerm.getManager().deny(Permission.MESSAGE_READ).block();
                perm.getManager().grant(Permission.MESSAGE_READ).block();
            } else if(channel instanceof VoiceChannel){
                allPerm.getManager().deny(Permission.VOICE_CONNECT).block();
                perm.getManager().grant(Permission.VOICE_CONNECT).block();
            } else {
                logger.log(Level.WARNING, "Channel is not voice or text channel");
                return false;
            }
            Set<Channel> channels = associates.get(role);
            if(channels == null){
                channels = new HashSet<>();
            }
            channels.add(channel);
            associates.put(role, channels);
            saveData();
            return true;
        } catch (RateLimitedException e) {
            logger.log(Level.FATAL, e.getMessage());
        }
        return false;
    }

    public static boolean linkServerToRole(Guild guild){
        if(!guild.getId().equals(targetGuild.getId())){
            if(!roleMap.containsKey(guild)){
                try {
                    Role role;
                    List<Role> roles;
                    if((roles = targetGuild.getRolesByName(guild.getName(), true)).size() > 0){
                        role = roles.get(0);
                        if(role != null) {
                            roleMap.put(guild, role);
                            for(Member member: targetGuild.getMembers()){
                                if(guild.isMember(member.getUser()) && !member.getRoles().contains(role)){
                                    targetGuild.getController().addRolesToMember(member, role).queue();
                                    logger.info("Added " + role.getName() + " to user: " + member.getUser().getName());
                                }
                            }
                            saveData();
                            return true;
                        } else {
                            if(delinkServerFromRole(guild)) {
                                return linkServerToRole(guild);
                            }
                        }
                    } else {
                        role = targetGuild.getController().createRole().block();
                        role.getManager().setName(guild.getName()).block();
                        roleMap.put(guild, role);
                        for(Member member: targetGuild.getMembers()){
                            if(guild.isMember(member.getUser()) && !member.getRoles().contains(role)){
                                targetGuild.getController().addRolesToMember(member, role).block();
                                logger.info("Added " + role.getName() + " to user: " + member.getUser().getName());
                            }
                        }
                        logger.log(Level.INFO, guild.getId() + " " + role.getId());
                        saveData();
                        logger.log(Level.INFO, "Adding role: \"" + role.getName()
                                + "\" From guild: " + guild.getName());
                        return true;
                    }
                } catch (RateLimitedException e) {
                    logger.log(Level.FATAL, e.getMessage());
                }
            }
        }
        return false;
    }

    private static boolean delinkServerFromRole(Guild guild){
        try {
            Role role = roleMap.get(guild);
            if(role != null) {
                if (associates.get(role) != null) {
                    for (Channel channel : associates.get(role)) {
                        channel.delete().block();
                    }
                }
                role.delete().queue();
            }
        } catch (RateLimitedException e) {
            logger.log(Level.FATAL, e.getMessage());
        }
        associates.remove(roleMap.get(guild));
        roleMap.remove(guild);
        saveData();
        return true;

    }

    public static boolean saveData(){
        File dataFile = new File("data.ser");
        try {
            if(!dataFile.exists()){
                if(!dataFile.createNewFile()){
                    return false;
                }
            }
            String targetGuildID = targetGuild.getId();

            HashMap<String, String> roleMapID = new HashMap<String, String>();
            for(Map.Entry<Guild, Role> entry: roleMap.entrySet()){
                roleMapID.put(entry.getKey().getId(), entry.getValue().getId());
            }

            HashMap<String, Set<String>> associatesID = new HashMap<String, Set<String>>();
            for (Map.Entry<Role, Set<Channel>> entry : associates.entrySet()) {
                Set<String> temp = new HashSet<String>();
                for (Channel channel : entry.getValue()) {
                    temp.add(channel.getId());
                }
                associatesID.put(entry.getKey().getId(), temp);
            }

            HashMap<String, String> specialRolesID = new HashMap<>();
            for(Map.Entry<String, Role> entry: specialRoles.entrySet()){
                specialRolesID.put(entry.getKey(), entry.getValue().getId());
            }
            HashMap<String, String> specialChannelsID = new HashMap<>();
            for(Map.Entry<String, Channel> entry: specialChannels.entrySet()){
                specialChannelsID.put(entry.getKey(), entry.getValue().getId());
            }
            FileOutputStream fos = new FileOutputStream(dataFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(new Data(targetGuildID, roleMapID, associatesID, specialRolesID, specialChannelsID));
            logger.log(Level.INFO, "Save Successful");
            fos.close();
            oos.close();
            return true;
        } catch (IOException e) {
            logger.log(Level.FATAL, e.getMessage());
        }
        return false;
    }

    public static  boolean loadData(){
        File dataFile = new File("data.ser");
        if(dataFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream("data.ser");
                ObjectInputStream ois = new ObjectInputStream(fis);
                Data data = (Data) ois.readObject();
                fis.close();
                ois.close();
                targetGuild = globalJDA.getGuildById(data.getTargetGuildID());
                for(Map.Entry<String, String> entry: data.getRoleMapID().entrySet()){
                    roleMap.put(globalJDA.getGuildById(entry.getKey()),
                                targetGuild.getRoleById(entry.getValue()));
                }
                for(Map.Entry<String, String> entry: data.specialRolesID.entrySet()){
                    specialRoles.put(entry.getKey(), targetGuild.getRoleById(entry.getValue()));
                }
                for(Map.Entry<String, String> entry: data.specialChannelsID.entrySet()){
                    Channel channel = targetGuild.getTextChannelById(entry.getValue());
                    if(channel == null){
                        channel = targetGuild.getVoiceChannelById(entry.getValue());
                    }
                    specialChannels.put(entry.getKey(), channel);
                }
                for(Map.Entry<String, Set<String>> entry: data.getAssociations().entrySet()){
                    Set<Channel> temp = new HashSet<Channel>();
                    for(String string: entry.getValue()){
                        Channel channel;
                        if((channel = targetGuild.getTextChannelById(string)) == null){
                            channel = targetGuild.getVoiceChannelById(string);
                        }
                        temp.add(channel);
                    }
                    associates.put(targetGuild.getRoleById(entry.getKey()), temp);
                }
                out = (MessageChannel) specialChannels.get("bot-commands");
                if(out == null){
                    out = targetGuild.getPublicChannel();
                }
                logger.log(Level.INFO, "Load Successful");
                return true;
            } catch (IOException | ClassNotFoundException e) {
                logger.log(Level.FATAL, e.getMessage());
            }
        } else {
            logger.log(Level.WARNING, "Load file not found");
        }
        return false;
    }

    public static class Data implements Serializable{
        String targetGuildID;
        HashMap<String, String> roleMapID;
        HashMap<String, Set<String>> associations;
        HashMap<String, String> specialRolesID = new HashMap<>();
        HashMap<String, String> specialChannelsID = new HashMap<>();

        private Data(String targetGuildID,
                    HashMap<String, String> roleMapID,
                    HashMap<String, Set<String>> associations,
                    HashMap<String, String> specialRolesID,
                    HashMap<String, String> specialChannelsID) {

            this.targetGuildID = targetGuildID;
            this.roleMapID = roleMapID;
            this.associations = associations;
            this.specialRolesID = specialRolesID;
            this.specialChannelsID = specialChannelsID;
        }

        private String getTargetGuildID() {
            return targetGuildID;
        }

        private HashMap<String, String> getRoleMapID() {
            return roleMapID;
        }

        private HashMap<String, Set<String>> getAssociations() {
            return associations;
        }
    }

}
