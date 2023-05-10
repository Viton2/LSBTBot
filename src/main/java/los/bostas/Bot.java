package los.bostas;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
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

        try (InputStream input = Bot.class.getClassLoader().getResourceAsStream("config.properties")) {

            Properties prop = new Properties();

            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }

            //load a properties file from class path, inside static method
            prop.load(input);

            //get the property value and print it out
//            JDA bot = JDABuilder.createDefault(prop.getProperty("api.config.token"))
//                    .setActivity(Activity.listening("sua mae gemendo"))
//                    .setStatus(OnlineStatus.ONLINE)
//                    .build();

        // args[0] would be the token (using an environment variable or config file is preferred for security)
        // We don't need any intents for this bot. Slash commands work without any intents!
        JDA jda = JDABuilder.createLight(prop.getProperty("api.config.token"), Collections.emptyList())
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
        ).queue();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        channel = event.getGuild().getTextChannelById("1105628592760696902");
        message = channel.sendMessage("Bem-vindo(a) " + event.getMember().getAsMention() + " ao " + event.getGuild().getName() + "! Reaja a esta mensagem para obter acesso aos chats de voz e texto.").complete();
        message.addReaction(Emoji.fromCustom("teste", Long.parseLong("1105626501476536392"), false)).queue();
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (message != null) {
            // Código para adicionar o cargo aqui
            if (event.getMessageId().equals(message.getId()) && !event.getUser().isBot()) {
                Role role = event.getGuild().getRoleById("1105628763586302053");
                event.getGuild().addRoleToMember(event.getMember(), role).queue();
            }
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event){
        channel = event.getGuild().getTextChannelById("1105628592760696902");
        message = channel.sendMessage(event.getMember() + " saiu do servidor").complete();
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
        }


    }

}


