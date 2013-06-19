package models;

import com.google.gson.annotations.Expose;
import flexjson.JSONSerializer;
import models.deadbolt.Role;
import models.deadbolt.RoleHolder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jasypt.util.password.StrongPasswordEncryptor;
import play.Play;
import play.data.validation.Email;
import play.data.validation.Password;
import play.data.validation.Required;
import play.data.validation.Unique;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "webuser")
public class WebUser extends Model implements RoleHolder {
    @Expose
    @Required(message = "This is a required field")
    @Unique(message = "This email address is already in use")
    @Email
    @Column(unique = true, nullable = false)
    public String email;

    @Password
    @Transient
    public String password;

    @Column(name = "password", nullable = false)
    public String passwordHash;

    @Expose
    @Required(message = "This is a required field")
    @Column(nullable = false)
    public String firstname;

    @Expose
    @Required(message = "This is a required field")
    @Column(nullable = false)
    public String lastname;

    @Expose
    @Required(message = "A Webuser role must be selected")
    @ManyToOne
    public ApplicationRole role;

    @ManyToMany(mappedBy = "users", cascade = CascadeType.REMOVE)
    public Collection<Filter> personalFilters;
    
    @Column(name = "pw_reset_token")
    public String passwordResetToken;

    private static final StrongPasswordEncryptor ENCRYPTOR = new StrongPasswordEncryptor();

    public void setPassword(String password) {
        this.password = password;
        this.passwordHash = encryptPassword(password);
    }

    public static boolean isValidLogin(String email, String password) {
        boolean valid = false;

        WebUser u = WebUser.find("byEmail", email).first();
        if(u != null) {
            valid = checkPassword(password, u.passwordHash);
        }

        return valid;
    }

    public static WebUser getByEmail(String email) {
        return find("byEmail", email).first();
    }

    @Override
    public List<? extends Role> getRoles() {
        return Arrays.asList(role);
    }

    public boolean isAdmin() {
        return "admin".equals(role.name);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static JSONSerializer createSerializer() {
        return new JSONSerializer().include(
            "sEcho", "iTotalRecords", "iTotalDisplayRecords", "aaData.email",
            "aaData.firstname", "aaData.lastname", "aaData.role.name",
            "aaData.role.longname", "name", "longname", "id",
            "email", "firstname", "lastname", "role.name", "role.longname"
        ).exclude("*").prettyPrint(Play.mode == Play.Mode.DEV);
    }

    static boolean checkPassword(String plainPassword, String encryptedPassword) {
        return ENCRYPTOR.checkPassword(plainPassword, encryptedPassword);
    }

    static String encryptPassword(String password) {
        return ENCRYPTOR.encryptPassword(password);
    }
    
    /**
     * Generate a unique token to be used to identify a WebUser when reseting a password.
     *
     * @return
     */
    public static String generateToken() {
        return UUID.randomUUID().toString().toUpperCase();
    }
    
    public static String validateNewPassword(String password, String confirmPassword) {
        String error = "";
        if (password == null) {
            error = "Password must have a value.";
        } else if (password.length() < 8) {
            error = "Password must be at least 8 characters long.";
        } else if (password.contains(" ")) {
            error = "Password cannot contain spaces.";
        } else if (!password.equals(confirmPassword)) {
            error = "Password don't match.";
        }
        return error;
    }
}
