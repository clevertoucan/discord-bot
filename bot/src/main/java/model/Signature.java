package model;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Signature {
    String userID, channelID, guildID;

    public Signature(@NotNull String userID, String channelID, String guildID) {
        this.userID = userID;
        this.channelID = channelID;
        this.guildID = guildID;
    }

    public Signature(@NotNull User user, MessageChannel channel, Guild guild){
        this(user.getId(), channel.getId(), guild.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Signature signature = (Signature) o;
        return Objects.equals(userID, signature.userID) &&
                Objects.equals(channelID, signature.channelID) &&
                Objects.equals(guildID, signature.guildID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID, channelID, guildID);
    }
}
