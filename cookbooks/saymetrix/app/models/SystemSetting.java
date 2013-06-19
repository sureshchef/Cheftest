package models;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * SystemSettings are used to store system-wide configuration properties.
 */
@Entity
@Table(name = "system_setting")
public class SystemSetting extends Model {
    private static final String CACHE_PREFIX = Play.id + "-setting-";

    public enum Type {
        // WARN Do not change the integers below
        INTEGER(0),
        STRING(1),
        BOOLEAN(2);

        final int id;

        private Type(int id) {
            this.id = id;
        }
    }

    /*
    * type and typeId are setup like this to allow us to use
    * Type id rather than ordinal for JPA.
    */
    @Transient
    Type type;
    @Column(name = "type_id")
    int typeId;
    @Column(name = "key_", unique =  true)
    public String key;
    public String value;

    public SystemSetting(Type t, String key) {
        this.key = key;
        setType(t);
    }

    /**
     *
     * @param key key whose associated value is to be returned as an int.
     * @param def the value to be returned in the event that there is no value associated with the key,
     *            or the associated value cannot be interpreted as an int.
     * @return the int value represented by the string associated with the key, or def if the
     * associated value does not exist or cannot be interpreted as an int.
     */
    public static int getInt(String key, int def) {
        SystemSetting setting = get(key);

        int value = def;

        if(setting != null) {
            if(setting.value != null) {
                try {
                    value = Integer.valueOf(setting.value);
                } catch(NumberFormatException e) {
                    Logger.warn("SystemSetting '%s' contained non-integer value '%s'", key, setting.value);
                }
            }
        }

        return value;
    }

    /**
     * Returns the boolean value represented by the string associated with the specified key.
     * Valid strings are "true", "y", "t" and "1", which represent true, and "false", "n", "f"
     * and "0" which represents false. Case is ignored, so, for example, "TRUE" and
     * "False" are also valid.
     *
     * Returns the specified default if there is no value associated with the key, or if the
     * associated value is something other than "true" or "false", ignoring case as outlined
     * above.
     *
     * @param key key whose associated value is to be returned as a boolean.
     * @param def the value to be returned in the event that there is no value associated with
     *            key or the associated value cannot be interpreted as a boolean.
     * @return the boolean value represented by the string associated with key, or def if the
     * associated value does not exist or cannot be interpreted as a boolean.
     */
    public static boolean getBoolean(String key, boolean def) {
        SystemSetting setting = get(key);

        boolean value = def;

        if(setting != null && setting.value != null) {
            try {
              value = getBoolean(setting.value);
            } catch(IllegalArgumentException e) {
                Logger.warn("SystemSetting '%s' contained non-boolean value '%s'", key, setting.value);
            } catch(NullPointerException e) {
                Logger.warn("SystemSetting '%s' contained non-boolean value '%s'", key, setting.value);
            }
        }

        return value;
    }

    /**
     * Returns the value associated with the specified key. Returns the specified default if there
     * is no value associated with the key.
     *
     * @param key key whose associated value is to be returned.
     * @param def the value to be returned in the event that there is no value associated with the key.
     * @return the value associated with key, or def if no value is associated with key.
     */
    public static String get(String key, String def) {
        SystemSetting setting = get(key);

        String value = def;

        if(setting != null && setting.value != null) {
            value = setting.value;
        }

        return value;
    }

    /**
     * Get the SystemSetting with the specified key.
     *
     * @param key
     * @return SystemSetting with the specified key or null of it does not exist.
     */
    static SystemSetting get(String key) {
        SystemSetting setting = Cache.get(CACHE_PREFIX + key, SystemSetting.class);
        if(setting == null) {
            setting = SystemSetting.find("byKey", key).first();
            Cache.set(CACHE_PREFIX + key, setting, "30mn");
        }

        return setting;
    }

    /*
     * Determines whether the specified string represents a true or false boolean value.
     *
     * @param s
     * @return
     * @throws IllegalArgumentException if the string does not represent a boolean value.
     */
    protected static boolean getBoolean(String s) {
        boolean value;
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("y") || s.equalsIgnoreCase("t") || s.equals("1")) {
            value = true;
        } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("n") || s.equalsIgnoreCase("f") || s.equals("0")) {
            value = false;
        } else {
            throw new IllegalArgumentException();
        }

        return value;
    }

    public void setType(Type t) {
        // See note at type and typeId declaration
        this.type = t;
        this.typeId = type.id;
    }

    int getTypeId() {
        return typeId;
    }

    public void setTypeId(int id) {
        // See note at type and typeId declaration
        this.typeId = id;
        for(Type t : Type.values()) {
            if(t.id == id) {
                type = t;
            }
        }
    }
}
