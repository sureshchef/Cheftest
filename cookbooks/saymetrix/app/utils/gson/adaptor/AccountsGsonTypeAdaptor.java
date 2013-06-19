package utils.gson.adaptor;

import com.google.gson.*;
import models.Account;

import java.lang.reflect.Type;

public class AccountsGsonTypeAdaptor implements JsonSerializer<Account>,
        JsonDeserializer<Account> {

    /**
     * Converts account to account Value Object
     *
     * @param t
     * @param type
     * @param jsc
     * @return
     */
    public JsonElement serialize(Account t, Type type,
            JsonSerializationContext jsc) {
        return new GsonBuilder().create().toJsonTree(t.getValueObject());
    }

    /**
     * Expects a key returned as Account element.
     *
     * @param je
     * @param type
     * @param jdc
     * @return
     * @throws JsonParseException
     */
    public Account deserialize(JsonElement je, Type type,
            JsonDeserializationContext jdc) throws JsonParseException {
        /**
         * account:DANU
         */
        String key = je.getAsString();
        if (key == null) {
            return null;
        }
        return Account.find("byKey", key).first();
    }
}
