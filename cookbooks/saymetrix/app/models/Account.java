package models;

import com.google.gson.annotations.Expose;
import flexjson.JSONSerializer;
import models.valueobject.AccountValueObject;
import play.Play;
import play.data.validation.Email;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Unique;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account")
@org.hibernate.annotations.Filter(name="manager")
public class Account extends Model {

    @Expose
    @Unique(message = "That key is already in use")
    @Required(message = "This is a required field")
    @Column(name = "akey", unique = true, length = 16, nullable = false)
    @MinSize(value = 4, message = "Key must contain at least 4 characters")
    public String key;
    @Expose
    @Unique
    @Required(message = "This is a required field")
    @Column(unique = true, length = 64, nullable = false)
    @MinSize(4)
    public String name;
    // Operator account manager for the account
    @Expose
    @ManyToOne
    @Required
    @JoinColumn(name = "manager_id", nullable = false)
    public WebUser manager;
    // Primary customer contact for the account
    @Expose
    @Email
    @Column(name = "contact_email")
    @MinSize(4)
    public String contact;
    @OneToMany(mappedBy = "account")
    public List<MobileSubscriber> subscribers = new ArrayList<MobileSubscriber>();

    public Account() {
    }

    public Account(String key, String name) {
        this.key = key;
        this.name = name;
    }
    
    public void setKey(String key){
        this.key = key.toUpperCase();
    }

    /*
     * Need to override findById to force Hibernate Filters to
     * be enforced for this method.
     *
     * https://groups.google.com/d/topic/play-framework/tPOljoFE8e8/discussion
     */
    public static Account findById(Long id) {
        return Account.find("byId", id).first();
    }

    public static Account findByKey(String key) {
        return Account.find("byKey", key).first();
    }

    public String toString() {
        return String.format("key=%s, name=%s, managerId=%s, contact=%s", key, name, manager, contact);
    }

    public AccountValueObject getValueObject() {
        return new AccountValueObject(key, name);
    }

    public static JSONSerializer createSerializer() {
        return new JSONSerializer().include(
                "sEcho", "iTotalRecords", "iTotalDisplayRecords",
                "aaData.key", "aaData.name", "aaData.contact",
                "aaData.manager.firstname", "aaData.manager.lastname", "aaData.id",
                "key", "name", "contact", "id", "manager.firstname", "manager.lastname", "manager.id"
        ).exclude("*").prettyPrint(Play.mode == Play.Mode.DEV);
    }
}
