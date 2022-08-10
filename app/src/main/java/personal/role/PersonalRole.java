package personal.role;

import java.io.Serializable;

public class PersonalRole implements Serializable {
    private static final long serialVersionUID = 2;
    private String userId;
    private String roleId;
    private String name;
    private String color;

    public PersonalRole(String user, String role) {
        userId = user;
        roleId = role;
        name = "New Personal Role";
    }

    public String getUserId() {
        return userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String id) {
        roleId = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setColor(String color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }
    public String getColor() {
        return color;
    }
    
}
