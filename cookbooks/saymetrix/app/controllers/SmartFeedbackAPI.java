package controllers;

import models.*;
import models.enumerations.Frequency;
import models.enumerations.LocationTech;
import models.enumerations.Position;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Scope;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides SmartFeedback-compatible API for legacy mobile apps.
 */
public class SmartFeedbackAPI extends Controller {
    private static final String CLASSNAME = "SmartFeedbackAPI";
    private static final String REP_FAILURE_TEMPLATE = CLASSNAME + "/report_failure.txt";
    private static final String MSG_REGISTER_FAILURE = "msg.register.failure";
    private static final String MSG_REGISTER_SUCCESS = "msg.register.success";
    private static final String MSG_REPORT_FAILURE = "msg.report.failure";
    private static final String MSG_REPORT_SUCCESS = "msg.report.success";
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern(
            "dd-MM-yyyy HH:mm:ss");

    public static void register(String msisdn) {
        Map templateObjects = new HashMap();
        Template t;
        // TODO Return an error template if msisdn is missing
        MobileSubscriber subscriber = MobileSubscriber.find(msisdn);

        // Subscriber not provisioned
        if (subscriber == null) {
            AuditEvent.create(AuditEvent.Type.MOBILE_ACTIVATE_FAIL, msisdn).save();
            response.status = Http.StatusCode.FORBIDDEN;
            templateObjects.put("msisdn", msisdn);
            t = TemplateLoader.load(MSG_REGISTER_FAILURE, generateRegisterMessage(false));
            renderXml(t.render(templateObjects));
        } else {
            AuditEvent.create(AuditEvent.Type.MOBILE_ACTIVATE_SUCCESS, msisdn).save();
            try {
                // The old Vodafone SmartFeedback app can only handle numeric tokens
                if(Play.id.contains("vfie")) {
                    subscriber.register(true);
                } else {
                    subscriber.register(false);
                }
            } catch(IllegalStateException e) {
                // User previously registered - we'll just let them register again
            }
            templateObjects.put("token", subscriber.token);
            t = TemplateLoader.load(MSG_REGISTER_SUCCESS, generateRegisterMessage(true));
            renderXml(t.render(templateObjects));
        }
    }


    public static void report() {
        Template t;
        Map templateObjects = new HashMap();
        response.contentType = "text/xml";
        String token; // User-identifying token

        // Is there a token present in the request?
        if ((token = params.get("uid")) == null) {
            response.status = Http.StatusCode.FORBIDDEN;
            renderArgs.put("message",
                    "The parameter uid has not been specified.");
            render(REP_FAILURE_TEMPLATE);
        }

        // Check that the subscriber exists
        MobileSubscriber sub = MobileSubscriber.find("byToken", token).first();
        if (sub == null) {
            AuditEvent.create(AuditEvent.Type.BAD_TOKEN, token).save();
            response.status = Http.StatusCode.FORBIDDEN;
            t = TemplateLoader.load(MSG_REPORT_FAILURE, generateReportMessage(false));
            renderXml(t.render(templateObjects));
        }

        // Validate the remaining request parameters
        ArgumentHolder holder = new ArgumentHolder(params);
        if (!holder.isValid()) {
            // TODO: Should return Bad Request rather than Forbidden in certain circumstance
            response.status = Http.StatusCode.FORBIDDEN;
            renderArgs.put("message", holder.getErrorMessage());
            render(REP_FAILURE_TEMPLATE);
        }

        try {
            Incident incident = holder.toIncident(sub);
            incident.validateAndCreate();
            templateObjects.put("firstname", sub.firstname);
            t = TemplateLoader.load(MSG_REPORT_SUCCESS, generateReportMessage(true));
            renderXml(t.render(templateObjects));
        } catch (Exception exception) {
            Logger.warn("Malformed request: %s", request);
            response.status = Http.StatusCode.FORBIDDEN;
            renderArgs.put("message", "Malformed request");
            render(REP_FAILURE_TEMPLATE);
        }
    }

    static String generateRegisterMessage(boolean success) {
        // TODO Cache these messages
        StringBuffer sb = new StringBuffer("<message>");
        if(success) {
            sb.append("success</message><id>${token}</id><info>");
            sb.append(SystemSetting.get(MSG_REGISTER_SUCCESS, null));
        } else {
            sb.append("failure</message><info>");
            sb.append(SystemSetting.get(MSG_REGISTER_FAILURE, null));
        }
        sb.append("</info>");

        return sb.toString();
    }

    static String generateReportMessage(boolean success) {
        // TODO Cache these messages
        StringBuffer sb = new StringBuffer("<message>");
        if(success) {
            sb.append("success</message><info>");
            sb.append(SystemSetting.get(MSG_REPORT_SUCCESS, null));
        } else {
            sb.append("failure</message><info>");
            sb.append(SystemSetting.get(MSG_REPORT_FAILURE, null));
        }
        sb.append("</info>");

        return sb.toString();
    }

    /*
     * Responsible for validating incidents reported from the SmartFeedback
     * mobile app. It's more forgiving than SmartFeedback in that it doesn't
     * required that all fields must have a value (e.g. "") even if those fields
     * are optional.
     */
    static class ArgumentHolder {

        private Scope.Params params;
        private StringBuffer errorMessage = new StringBuffer(
                "Incorrect message format: missing param(s): ");
        /*
         * The following attributes follow the naming conventions used in the
         * SmartFeedback protocol.
         */
        String imsi, imei, cellid, lac;
        Double lat, lon;
        String locType, accuracy, inout, freq, event, description;
        DateTime created;
        String addText, handset, os;

        ArgumentHolder(Scope.Params params) {
            this.params = params;
        }

        boolean isValid() {
            boolean valid = true;

            /*
             * TODO The argument names in the strings below are the names from
             * SmartFeedback. These MUST be reviewed before deciding on naming
             * for SayMetrix incident model fields.
             */
            imsi = params.get("imsi");

            imei = params.get("imei");

            cellid = params.get("cellid");

            lac = params.get("lac");
            if ((lat = params.get("lat", Double.class)) == null) {
                valid = false;
                errorMessage.append("lat ");
            }

            if ((lon = params.get("lon", Double.class)) == null) {
                valid = false;
                errorMessage.append("lon ");
            }

            locType = params.get("locType");

            accuracy = params.get("accuracy");

            if (params.get("inout") == null) {
                valid = false;
                errorMessage.append("inout ");
            }
            inout = params.get("inout");

            if (params.get("freq") == null) {
                valid = false;
                errorMessage.append("freq ");
            }
            freq = params.get("freq");

            if (params.get("event") == null) {
                valid = false;
                errorMessage.append("event ");
            }
            event = params.get("event");

            String dateTimeString;
            if ((dateTimeString = params.get("time")) == null) {
                valid = false;
                errorMessage.append("time ");
            } else {
                try {
                    if(dateTimeString.contains("T")) {
                        created = DateTime.parse(dateTimeString);
                    } else {
                        created = FORMATTER.parseDateTime(dateTimeString);
                    }
                } catch (IllegalArgumentException e) {
                    valid = false;
                    errorMessage = new StringBuffer("Malformed date or time");
                }
            }

            addText = params.get("add_text");
            if (params.get("handset") == null) {
                valid = false;
                errorMessage.append("handset ");
            }

            handset = params.get("handset");
            if (params.get("os") == null) {
                valid = false;
                errorMessage.append("os ");
            }

            os = params.get("os");

            return valid;
        }

        Incident toIncident(MobileSubscriber mobileSubscriber) {
            Incident incident = new Incident();
            incident.comment = addText;
            incident.cellId = cellid;
            incident.date = created;
            incident.frequency = getFrequencyEnumFromFreqString(freq);
            incident.imei = imei;
            incident.incidentType = getIncidentTypeFromEvent(event);
            incident.latitude = lat;
            incident.longitude = lon;
            try {
                incident.locationTech = LocationTech.valueOf(locType.toUpperCase());
            } catch(IllegalArgumentException e) {
                // locationTech is optional
            }
            incident.phoneType = handset;
            incident.position = getPositionFrom(inout);
            incident.subscriber = mobileSubscriber;
            incident.imsi = imsi;
            return incident;
        }

        String getErrorMessage() {
            return errorMessage.toString();
        }

        private Frequency getFrequencyEnumFromFreqString(String freq) {
            Frequency frequency;
            int frequencyAsInt = Integer.parseInt(freq);
            switch (frequencyAsInt) {
                case (0):
                    frequency = Frequency.ONCE;
                    break;
                case (1):
                    frequency = Frequency.SELDOM;
                    break;
                case (2):
                    frequency = Frequency.OFTEN;
                    break;
                case (3):
                    frequency = Frequency.ALWAYS;
                    break;
                default:
                    frequency = Frequency.ONCE;
            }
            return frequency;
        }

        private IncidentType getIncidentTypeFromEvent(String event) {
            String incidentKey = getIncidentKeyFrom(event);
            return IncidentType.findByKey(incidentKey);
        }

        private String getIncidentKeyFrom(String event) {
            String incidentKey = null;
            int eventAsInt = Integer.parseInt(event);
            switch (eventAsInt) {
                case (0):
                    incidentKey = "voice_no_coverage";
                    break;
                case (1):
                    incidentKey = "voice_network_busy";
                    break;
                case (2):
                    incidentKey = "voice_poor_sound";
                    break;
                case (3):
                    incidentKey = "voice_dropped_call";
                    break;
                case (4):
                    incidentKey = "voice_other";
                    break;
                case (5):
                    incidentKey = "data_no_coverage";
                    break;
                case (6):
                    incidentKey = "data_no_communication";
                    break;
                case (7):
                    incidentKey = "data_slow_connection";
                    break;
                case (8):
                    incidentKey = "data_dropped_connection";
                    break;
                case (9):
                    incidentKey = "data_other";
                    break;
                case (10):
                    incidentKey = "other_other";
                    break;
            }
            return incidentKey;
        }

        private Position getPositionFrom(String inout) {
            Position position = null;
            int postionAsInt = Integer.parseInt(inout);
            switch (postionAsInt) {
                case (0):
                    position = Position.INDOOR;
                    break;
                case (1):
                    position = Position.OUTDOOR;
                    break;
                case (2):
                    position = Position.ONTHEMOVE;
                    break;
            }
            return position;
        }
    }
}
