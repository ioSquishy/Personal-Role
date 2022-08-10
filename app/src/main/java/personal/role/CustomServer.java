package personal.role;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;

public class CustomServer implements Serializable {
    private static final long serialVersionUID = 1;
    private ArrayList<Long> userIds = new ArrayList<Long>();
    private ArrayList<PersonalRole> personalRoles = new ArrayList<PersonalRole>();

    private String serverId;
    private boolean hoisted;
    private String requiredRoleId;

    public CustomServer(String server, String everyoneRoleId) {
        serverId = server;
        hoisted = false;
        requiredRoleId = everyoneRoleId;
    }

    public void updateServer() {
        //updates to push to all servers
    }

    public boolean isHoisted() {
        return hoisted;
    }

    public boolean hasRole(String userId) {
        int index = Collections.binarySearch(userIds, Long.parseLong(userId));
        return index >= 0;
    }


    public String getServerId() {
        return serverId;
    }

    public void setRequiredRoleId(String id) {
        requiredRoleId = id;
        System.out.println(requiredRoleId);
        checkPermissions();
    }

    public String getRequiredRoleId() {
        return requiredRoleId;
    }

    public void checkPermissions() {
        List<Long> toDelete = new ArrayList<Long>();
        for (long userId : userIds) {
            //if they DONT have the required role, delete their personal role
            if (!App.api.getUserById(userId).join().getRoles(App.api.getServerById(serverId).get()).contains(App.api.getRoleById(requiredRoleId).get())) {
                toDelete.add(userId);
            }
        }
        while (!toDelete.isEmpty()) {
            deletePersonalRole(toDelete.remove(0).toString());
        }
    }

    public void updateRoles(Server server, boolean hoist) {
        hoisted = hoist;
        List<Role> roles = server.getRoles();
        List<Role> newList = new ArrayList<Role>();
        List<Role> pesrRoles = new ArrayList<Role>();
        for (Role role : roles) {
            if (role.isManaged() && role.getName().equals("Personal Role") || role.getName().equals("Kanna")) {
                break;
            }
            boolean isPersonalRole = false;
            for (PersonalRole persRole : personalRoles) {
                if (persRole.getRoleId().equals(role.getIdAsString())) {
                    isPersonalRole = true;
                    break;
                }
            }
            if (isPersonalRole) {
                pesrRoles.add(role);
                role.updateDisplaySeparatelyFlag(hoist);
            } else {
                newList.add(role);
            }
        }
        newList.addAll(pesrRoles);
        server.reorderRoles(newList).join();
    }

    public PersonalRole getRole(String userId) {
        int index = Collections.binarySearch(userIds, Long.parseLong(userId));
        if (index >= 0 && personalRoles.get(index).getUserId().equals(userId)) return personalRoles.get(index);
        index = Math.abs(index)-1;
        userIds.add(index, Long.parseLong(userId));
        personalRoles.add(index, new PersonalRole(userId, App.api.getServerById(serverId).get().createRoleBuilder().setPermissions(new PermissionsBuilder().build()).create().join().getIdAsString()));
        try {
            updateRoles(App.api.getServerById(serverId).get(), hoisted);
        } catch (Error e) {}
        try {
            App.api.getRoleById(personalRoles.get(index).getRoleId()).get().addUser(App.api.getUserById(userId).get()).join();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return personalRoles.get(index);
    }

    public PersonalRole replaceRole(String userId) {
        int index = Collections.binarySearch(userIds, Long.parseLong(userId));
        String newRoleId = App.api.getServerById(serverId).get().createRoleBuilder().setPermissions(new PermissionsBuilder().build()).create().join().getIdAsString();
        App.api.getRoleById(newRoleId).get().updateDisplaySeparatelyFlag(hoisted).join();
        personalRoles.get(index).setRoleId(newRoleId);
        try {
            App.api.getRoleById(newRoleId).get().addUser(App.api.getUserById(userId).get()).join();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return personalRoles.get(index);
    }

    public void deletePersonalRole(String userId) {
        int index = Collections.binarySearch(userIds, Long.parseLong(userId));
        if (index >= 0 && personalRoles.get(index).getUserId().equals(userId)) {
            App.api.getRoleById(personalRoles.get(index).getRoleId()).get().delete().join();
            userIds.remove(index);
            personalRoles.remove(index);
        }
    }

    public void deleteAllRoles() {
        while (!userIds.isEmpty()) {
            deletePersonalRole(userIds.get(0).toString());
        }
    }
}