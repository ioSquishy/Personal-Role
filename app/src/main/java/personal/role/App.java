package personal.role;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.permission.RoleUpdater;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import io.github.cdimascio.dotenv.Dotenv;

public class App implements Serializable {
    private static final long serialVersionUID = 0;

    public static final DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("TOKEN")).setIntents(Intent.GUILD_MEMBERS, Intent.GUILDS).login().join();

    private static ArrayList<Long> serverIds = new ArrayList<Long>();
    private static ArrayList<CustomServer> servers = new ArrayList<CustomServer>();

    private static transient ScheduledExecutorService autoSave = Executors.newSingleThreadScheduledExecutor();
    public static transient Runnable backup = () -> {
        try {
            saveData();
        } catch (IOException e) {
            api.getUserById("263049275196309506").join().openPrivateChannel().join().sendMessage("Data not saved.").join();
            e.printStackTrace();
        }
    };

    public static void main(String[] args) {
        System.out.println("yay");

        try {
            retrieveData();
        } catch (ClassNotFoundException | IOException e) {
            api.getUserById("263049275196309506").join().openPrivateChannel().join().sendMessage("Data not retreived.").join();
            e.printStackTrace();
        }

        autoSave.scheduleWithFixedDelay(backup, 1, 1, TimeUnit.MINUTES);

        new SlashCommandBuilder()
            .setName("ping")
            .setDescription("Pong!")
            .setDefaultEnabledForEveryone()
            .createGlobal(api).join();

        new SlashCommandBuilder()
            .setName("setrole")
            .setDescription("Update your personal role.")
            .setDefaultEnabledForEveryone()
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName("name")
                .setDescription("Set role name")
                .setRequired(false)
                .setType(SlashCommandOptionType.STRING)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("color")
                .setDescription("Set hex color")
                .setRequired(false)
                .setType(SlashCommandOptionType.STRING)
                .build())
            .createGlobal(api).join();

        new SlashCommandBuilder()
            .setName("updateroles")
            .setDescription("Updates role positions and additional options.")
            .setDefaultDisabled()
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName("hoisted")
                .setDescription("Set whether roles are hoisted or not")
                .setType(SlashCommandOptionType.BOOLEAN)
                .setRequired(false)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName("reset")
                .setDescription("THIS WILL DELETE ALL PERSONAL ROLES (Not your other roles)")
                .setType(SlashCommandOptionType.BOOLEAN)
                .setRequired(false)
                .build())
            .createGlobal(api).join();

        new SlashCommandBuilder()
            .setName("setrequiredrole")
            .setDescription("Set required role to use /setrole. If a user loses this role, their personal role will be deleted.")
            .setDefaultDisabled()
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName("role")
                .setDescription("Required role to use /setrole")
                .setType(SlashCommandOptionType.ROLE)
                .setRequired(true)
                .build())
            .createGlobal(api).join();

        new SlashCommandBuilder()
            .setName("help")
            .setDescription("Display command info in more detail.")
            .setDefaultEnabledForEveryone()
            .setEnabledInDms(false)
            .createGlobal(api).join();

        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            InteractionOriginalResponseUpdater response;
            CustomServer server = getServer(interaction.getServer().get().getIdAsString());
            boolean hasPerms = false;
            switch (interaction.getCommandName()) {
                case "setrole" :
                    response = interaction.respondLater(true).join();
                    //check if bot has perms
                    for (Role checkRoles1 : api.getYourself().getRoles(interaction.getServer().get())) {
                        if (checkRoles1.getAllowedPermissions().contains(PermissionType.MANAGE_ROLES)) {
                            hasPerms = true;
                            break;
                        }
                    }
                    if (!hasPerms) {
                        response.setContent("I need the `Manage Roles` Permission!").update();
                        return;
                    }
                    if (!interaction.getUser().getRoles(interaction.getServer().get()).contains(interaction.getServer().get().getRoleById(server.getRequiredRoleId()).get())) {
                        response.setContent("You do not have the required role to use this command!").setFlags(MessageFlag.EPHEMERAL).update();
                        return;
                    }
                    PersonalRole personalRole = server.getRole(interaction.getUser().getIdAsString());
                    //replace role if needed
                    if (!api.getRoleById(personalRole.getRoleId()).isPresent()) {
                        personalRole = server.replaceRole(interaction.getUser().getIdAsString());
                    }
                    //check if bot role is above personal role
                    if (!api.getYourself().canManageRole(api.getRoleById(personalRole.getRoleId()).get())) {
                        response.setContent("My role has to be above your personal role to update it!\n(Contact a staff member who has the `Manage Roles` permission to move me above your personal role!)").update();
                        return;
                    }
                    //update role
                    RoleUpdater role = api.getRoleById(personalRole.getRoleId()).get().createUpdater();
                    if (interaction.getOptionStringValueByName("color").isPresent()) {
                        String color = interaction.getOptionStringValueByName("color").get();
                        if (!color.startsWith("#")) {
                            color = "#" + color;
                        }
                        if (color.length() == 7) {
                            role.setColor(Color.decode(color));
                            personalRole.setColor(color);
                        } else {
                            response.setContent("Invalid Color. You must enter a hex code which you can get from https://g.co/kgs/y1cuUd").setFlags(MessageFlag.EPHEMERAL).update();
                            return;
                        }
                    }
                    if (interaction.getOptionStringValueByName("name").isPresent()) {
                        String name = interaction.getOptionStringValueByName("name").get();
                        role.setName(name);
                        personalRole.setName(name);
                    }
                    role.update().join();
                    //respond
                    response.setContent("Role Updated").setFlags(MessageFlag.EPHEMERAL).update();
                    server.updateRoles(server.isHoisted());
                    break;

                case "updateroles" :
                    response = interaction.respondLater(true).join();
                    for (Role checkRoles2 : api.getYourself().getRoles(interaction.getServer().get())) {
                        if (checkRoles2.getAllowedPermissions().contains(PermissionType.MANAGE_ROLES)) {
                            hasPerms = true;
                            break;
                        }
                    }
                    if (!hasPerms) {
                        response.setContent("I need the `Manage Roles` Permission!").update();
                        return;
                    }
                    if (interaction.getOptionBooleanValueByName("reset").isPresent() && interaction.getOptionBooleanValueByName("reset").get()) {
                        server.deleteAllRoles();
                        System.out.println("deleted all");
                        response.setContent("All personal roles deleted.").setFlags(MessageFlag.EPHEMERAL).update();
                        return;
                    }
                    server.updateRoles(interaction.getOptionBooleanValueByName("hoisted").orElse(server.isHoisted()));
                    response.setContent("Updated").setFlags(MessageFlag.EPHEMERAL).update();
                    break;

                case "setrequiredrole" :
                    response = interaction.respondLater(true).join();
                    Role requiredRole = interaction.getOptionRoleValueByName("role").get();
                    getServer(interaction.getServer().get().getIdAsString()).setRequiredRoleId(requiredRole.getIdAsString());
                    response.setContent("Required role set to: " + requiredRole.getMentionTag()).setFlags(MessageFlag.EPHEMERAL).update();
                    break;

                case "ping" :
                    interaction.createImmediateResponder().setContent("Pong! `" + api.getLatestGatewayLatency().toMillis() + "ms`\nFeedback/Support Server: https://discord.gg/fCbYCbHE6z").setFlags(MessageFlag.EPHEMERAL).respond();
                    break;

                case "help" :
                    EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Commands")
                        .setColor(Color.PINK)
                        .setDescription(
                            "**/setrole (name) (color)**\n" + 
                            "> Creates your own personal role that you can change the name and color of at any time if you have the required role to do so. Current required role (set by admins): " + api.getRoleById(server.getRequiredRoleId()).get().getMentionTag() + "\n" +
                            "> If you lose the required role to use this command, your personal role will be deleted." + "\n\n" + 

                            "**/updateroles (hoisted) (reset)**\n" +
                            "> Command only enabled for admins by default that when used will move all current and future personal roles under the role of the bot. **(If you move the bots role lower than already existing personal roles, they wont be moved!)**\n" + 
                            "> If the (hoisted) option is enabled, it will make all personal roles appear seperatly from other roles.\n" + 
                            "> If the (reset) option is enabled, the bot will delete all personal roles it has access too." + "\n\n" + 

                            "**/setrequiredrole (role)**\n" +
                            "> Command only enabled for admins by default that when used will set the required role for users to use the **/setrole** command.\n" + 
                            "> All users who have a personal role that don't have the new required role will lose their personal role." + "\n\n" +

                            "[Support Server](https://discord.gg/fCbYCbHE6z) · [Bot Invite](https://discord.com/api/oauth2/authorize?client_id=999916712046641222&permissions=268436480&scope=bot%20applications.commands)"
                        );
                    interaction.createImmediateResponder().addEmbed(embed).respond();
                    break;
            }
        });
        
        api.addServerLeaveListener(event -> {
            removeServer(event.getServer().getIdAsString());
        });

        api.addServerMemberLeaveListener(event -> {
            String userId = event.getUser().getIdAsString();
            CustomServer server = getServer(event.getServer().getIdAsString());
            if (server.hasRole(userId)) {
                server.deletePersonalRole(userId);
            }
        });

        api.addUserRoleRemoveListener(event -> {
            CustomServer server = getServer(event.getServer().getIdAsString());
            if (server.hasRole(event.getUser().getIdAsString())) {
                if (!event.getUser().getRoles(event.getServer()).contains(event.getServer().getRoleById(server.getRequiredRoleId()).get())) {
                    server.deletePersonalRole(event.getUser().getIdAsString());
                }
            }
        });
    }

    private static CustomServer getServer(String serverId) {
        int index = Collections.binarySearch(serverIds, Long.parseLong(serverId));
        if (index >= 0 && servers.get(index).getServerId().equals(serverId)) return servers.get(index);
        index = Math.abs(index)-1;
        serverIds.add(index, Long.parseLong(serverId));
        servers.add(index, new CustomServer(serverId, api.getServerById(serverId).get().getEveryoneRole().getIdAsString()));
        return servers.get(index);
    }

    private static void removeServer(String serverId) {
        int index = Collections.binarySearch(serverIds, Long.parseLong(serverId));
        if (index >= 0 && servers.get(index).getServerId().equals(serverId)) {
            serverIds.remove(index);
            servers.remove(index);
        }
    }

    private static void saveData() throws IOException {
        File file = new File("serverIds.ser");
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(serverIds);
        out.close();
        fos.close();

        file =  new File("servers.ser");
        file.createNewFile();
        fos = new FileOutputStream("servers.ser");
        out = new ObjectOutputStream(fos);
        out.writeObject(servers);
        out.close();
    }

    private static void retrieveData() throws ClassNotFoundException, IOException {
        FileInputStream fin = new FileInputStream("serverIds.ser");
        ObjectInputStream in = new ObjectInputStream(fin);
        serverIds = (ArrayList) in.readObject();
        in.close();
        fin.close();

        fin = new FileInputStream("servers.ser");
        in = new ObjectInputStream(fin);
        servers = (ArrayList) in.readObject();
        in.close();
        fin.close();

        System.out.println("data retrieved");

        //push updates to servers if added variables
        for (CustomServer server : servers) {
            server.updateServer();
        }
    }

}
