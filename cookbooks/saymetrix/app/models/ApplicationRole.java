package models;

import models.deadbolt.Role;
import play.data.validation.Required;
import play.db.jpa.Model;
import play.i18n.Messages;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "role")
public class ApplicationRole extends Model implements Role {
    @Required
    @Column(nullable = false, unique = true)
    public String name;

    public ApplicationRole(String name) {
        this.name = name;
    }

    @Override
    public String getRoleName() {
        return name;
    }

    public String getLongname() {
        return Messages.get("role." + name);
    }

    public static ApplicationRole findByName(String name) {
        return ApplicationRole.find("byName", name).first();
    }

    @Override
    public String toString() {
        return name;
    }
}
