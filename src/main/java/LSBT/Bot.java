package LSBT;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Bot extends ListenerAdapter {
    private TextChannel channel;
    private Message message = null;

    public static void main(String[] args) {

        String token = System.getenv("API_TOKEN");
        if (token == null) {
            System.out.println("Sorry, unable to find API_TOKEN environment variable");
            return;
        }

        JDA jda = JDABuilder.createLight(token, Collections.emptyList())
                .addEventListeners(new Bot())
                .enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .enableIntents(GatewayIntent.GUILD_EMOJIS_AND_STICKERS)
                .setActivity(Activity.listening("sua mae gemendo"))
                .build();

        // Sets the global command list to the provided commands (removing all others)
        jda.updateCommands().addCommands(
                Commands.slash("ping", "Calculate ping of the bot"),
                Commands.slash("ban", "Ban a user from the server")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)) // only usable with ban permissions
                        .setGuildOnly(true) // Ban command only works inside a guild
                        .addOption(OptionType.USER, "user", "The user to ban", true) // required option of type user (target to ban)
                        .addOption(OptionType.STRING, "reason", "The ban reason"),
                Commands.slash("corno", "Diz que alguem eh corno")
                        .addOption(OptionType.USER, "user", "O Corno", true),
                Commands.slash("clean", "Limpa as mensagens")
                        .addOption(OptionType.USER, "user", "O Corno", true)
//                Commands.slash("msg", "manda msg")
        ).queue();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Role role = event.getGuild().getRoleById("1105665463880519792");
        event.getGuild().addRoleToMember(event.getMember(), role).queue();
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getMessageId().equals("1105672898854801482") && !event.getUser().isBot()) {
            if (!event.getMember().getRoles().isEmpty()) {
                if (event.getMember().getRoles().contains(event.getGuild().getRoleById("1105665463880519792"))) {
                    Role roleColocar = event.getGuild().getRoleById("1105665806555156480");
                    Role roleTirar = event.getGuild().getRoleById("1105665463880519792");
                    event.getGuild().addRoleToMember(event.getMember(), roleColocar).queue();
                    event.getGuild().removeRoleFromMember(event.getMember(), roleTirar).queue();
                } else {
                    event.getUser().openPrivateChannel().queue(channel -> {
                        String message = "Voce ja tem acesso aos chats, " + event.getUser().getName() + " corno(a).";
                        channel.sendMessage(message).queue();
                    });
                }
            } else {
                Role roleColocar = event.getGuild().getRoleById("1105665806555156480");
                event.getGuild().addRoleToMember(event.getMember(), roleColocar).queue();
            }
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event){
        channel = event.getGuild().getTextChannelById("1105667587477610519");
        message = channel.sendMessage(event.getUser().getName() + " saiu do servidor").complete();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // make sure we handle the right command
        switch (event.getName()) {
            case "ping":
                long time = System.currentTimeMillis();
                event.reply("Pong!").setEphemeral(true) // reply or acknowledge
                        .flatMap(v ->
                                event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                        ).queue(); // Queue both reply and edit
                break;
            case "ban":
                // double check permissions, don't trust discord on this!
                if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
                    event.reply("You cannot ban members! Nice try.").setEphemeral(true).queue();
                    break;
                }
                User target = event.getOption("user", OptionMapping::getAsUser);
                // optionally check for member information
                Member member = event.getOption("user", OptionMapping::getAsMember);
                if (!event.getMember().canInteract(member)) {
                    event.reply("You cannot ban this user.").setEphemeral(true).queue();
                    break;
                }
                // Before starting our ban request, tell the user we received the command
                // This sends a "Bot is thinking..." message which is later edited once we finished
                event.deferReply().queue();
                String reason = event.getOption("reason", OptionMapping::getAsString);
                AuditableRestAction<Void> action = event.getGuild().ban(target, 0, TimeUnit.SECONDS); // Start building our ban request
                if (reason != null) // reason is optional
                    action = action.reason(reason); // set the reason for the ban in the audit logs and ban log
                action.queue(v -> {
                    // Edit the thinking message with our response on success
                    event.getHook().editOriginal("**" + target.getAsTag() + "** was banned by **" + event.getUser().getAsTag() + "**!").queue();
                }, error -> {
                    // Tell the user we encountered some error
                    event.getHook().editOriginal("Some error occurred, try again!").queue();
                    error.printStackTrace();
                });
            case "corno":
                User user = event.getOption("user", OptionMapping::getAsUser);
                if (user != null) {
                    event.reply(user.getAsMention() + " eh um corno.").setEphemeral(false).queue();
                }else {
                    event.reply("Usuario invalido, tente novamente.").setEphemeral(false).queue();
                }
//            case "msg":
//                channel = event.getGuild().getTextChannelById("1105666280113049730");
//                message = channel.sendMessage("Boas-vindas ao "+event.getGuild().getName()+"! \n\n" +
//                        "Por favor nao spammar mensagens de qualquer tipo nos chats.\n" +
//                        "SEM MACAQUISSE OU BAN!\n\nClique no emote abaixo para ter acesso aos chats.").complete();
//                message.addReaction(Emoji.fromCustom("minipeng", Long.parseLong("1030494386141810819"), true)).queue();
        }


    }

}


