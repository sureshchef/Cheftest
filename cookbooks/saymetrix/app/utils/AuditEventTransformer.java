package utils;

import flexjson.BasicType;
import flexjson.TypeContext;
import flexjson.transformer.AbstractTransformer;
import models.AuditEvent;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.i18n.Messages;

import java.util.Locale;

/**
 * Serialize AuditEvents in a JSON format suitable for consumption by DataTables.
 */
public class AuditEventTransformer extends AbstractTransformer {
    // TODO Get locale from users profile
    private static final DateTimeFormatter FMT = DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("M-",
            Locale.ENGLISH) + " HH:mm");

    @Override
    public void transform(Object o) {
        AuditEvent e = (AuditEvent)o;
        boolean setContext = false;

        TypeContext ctx = getContext().peekTypeContext();

        if(ctx == null || ctx.getBasicType() != BasicType.OBJECT) {
            ctx = getContext().writeOpenObject();
            setContext = true;
        }

        if(!ctx.isFirst()) {
            getContext().writeComma();
        } else {
            ctx.setFirst(false);
        }

        getContext().writeName("timestamp");
        getContext().writeQuoted(FMT.print(e.timestamp));

        getContext().writeComma();
        getContext().writeName("type");
        getContext().writeQuoted(Messages.get(AuditEvent.Type.fromInt(e.getTypeId()).toString()));

        getContext().writeComma();
        getContext().writeName("actor");
        getContext().writeQuoted(e.actorEmail == null ? "" : e.actorEmail);

        getContext().writeComma();
        getContext().writeName("details");
        getContext().writeQuoted(e.details == null ? "" : e.details);

        if(setContext) {
            getContext().writeCloseObject();
        }
    }

    @Override
    public Boolean isInline() {
        return true;
    }
}
