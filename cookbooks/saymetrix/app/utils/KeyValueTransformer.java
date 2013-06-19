package utils;

import flexjson.transformer.AbstractTransformer;
import org.javatuples.KeyValue;

/**
 * Handle transformation of {@link KeyValue} objects to JSON.
 */
public class KeyValueTransformer extends AbstractTransformer {

    @Override
    public void transform(Object o) {
        KeyValue element = (KeyValue)o;

        getContext().writeOpenObject();
        getContext().writeName("date");
        getContext().writeQuoted(element.getKey().toString());
        getContext().writeComma();
        getContext().writeName("count");
        getContext().writeQuoted(element.getValue().toString());
        getContext().writeCloseObject();
    }

    @Override
    public Boolean isInline() {
        return true;
    }
}
