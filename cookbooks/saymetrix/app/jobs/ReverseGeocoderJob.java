package jobs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import models.Incident;
import models.IncidentDetails;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.libs.WS;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.net.ConnectException;
import java.util.List;

/**
 * Job to periodically reverse geocode incidents using the
 * <a href="http://developers.google.com/maps/documentation/geocoding/#ReverseGeocoding">Google Maps API</a>.
 * Can reverse geocode all incidents for which the address has not yet previously been determined.
 * Additionally can be used to reverse geocode a single incident.
 */
@Every("5mn")
public class ReverseGeocoderJob extends MultiNodeJob {
    private static final String GEO_SERVICE_URL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=%s," +
            "%s&sensor=false";
    private static final String NOT_FOUND = "Unknown";
    private Incident incident;

    /**
     * Use this constructor to create a job to reverse geocode all incidents for which the
     * street address is unknown.
     */
    public ReverseGeocoderJob() {
    }

    /**
     * Use this constructor create a job to reverse geocode a single incident.
     *
     * @param incident
     */
    public ReverseGeocoderJob(Incident incident) {
        this.incident = incident;
    }

    /**
     * Start reverse geocoding job.
     */
    @Override
    public void execute() {
        if (Play.runingInTestMode() || Play.id.contains("dev")) {
            Logger.debug("Running in dev/test mode - cancelled ReverseGeocoderJob.");
            return;
        }

        try {
            if (incident == null) {
                reverseGeocode();
            } else {
                reverseGeocode(incident);
            }
        } catch (ConnectException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reverse geocode all incidents in the system that have not yet
     * been processed. Due to service / network issues, Incidents may
     * not have be reverse geocoded when they were originally reported.
     */
    void reverseGeocode() throws ConnectException {
        // Update in batches so as to limit memory consumption
        Query q = JPA.em().createQuery("SELECT i FROM Incident i WHERE LENGTH(i.address) = 0 OR i.address IS NULL")
                .setFirstResult(0).setMaxResults(50);

        List<Incident> incidents;
        String jsonResponse;
        int count = 0;
        do {
            incidents = q.getResultList();
            count += incidents.size();
            for (Incident incident : incidents) {
                reverseGeocode(incident);
            }
            //  No more incidents left to reverse geocode?
            if (incidents.size() == 0) {
                break;
            }
        } while (true);
        if(count != 0) {
            Logger.info("Reverse geocoded " + count + " incident(s)");
        }
    }

    /**
     * Reverse geocode a single Incident. The Incident is updated with the
     * address set and the full JSON response from the geocoding service is returned.
     *
     * @param incident
     * @return full json details from geocoding service.
     * @throws ConnectException
     */
    void reverseGeocode(Incident incident) throws ConnectException {
        JsonElement json = reverseGeocode(incident.latitude, incident.longitude);

        if ("ZERO_RESULTS".equals(extractStatus(json))) {
            /*
             * Set the address to something non-null so that no attempt to reprocess this
             * incident is attempted next time a full pass of non reverse geocoded incidents is made.
             */
            incident.address = NOT_FOUND;
        } else {
            incident.address = extractFormattedAddress(json);
            // Save the JSON - we can always post-process it to analyse problems
            IncidentDetails details = IncidentDetails.find("byIncident", incident).first();
            if(details == null) {
                Logger.debug("Creating new IncidentDetails");
                details = new IncidentDetails(incident, json.toString());
            } else {
                Logger.debug("IncidentDetails already exist. Overwriting");
                details.addressJson = json.toString();
            }
            details.save();
        }
        incident.save();
    }

    /**
     * Connect to Google to reverse geocode the specified latitude and longitude.
     *
     * @param latitude
     * @param longitude
     * @return
     * @throws ConnectException
     */
    static JsonElement reverseGeocode(double latitude, double longitude) throws ConnectException {
        String url = String.format(GEO_SERVICE_URL, String.valueOf(latitude), String.valueOf(longitude));

        WS.HttpResponse response = WS.url(url).get();
        if (!response.success()) {
            throw new ConnectException(response.getStatusText() + " " + url);
        }

        return response.getJson();
    }

    /**
     * Extract the formatted street address from the reverse geolocation
     * Json response.
     *
     * @param response
     * @return the formatted address or an empty string if one couldn't be found.
     */
    static String extractFormattedAddress(JsonElement response) {
        String address = "";

        JsonArray results = response.getAsJsonObject().getAsJsonArray("results");
        for (JsonElement el : results) {
            String addr = el.getAsJsonObject().getAsJsonPrimitive("formatted_address").getAsString();
            // Find the longest address (assumption is it will be the most accurate one)
            if (addr.length() > address.length()) {
                address = addr;
            }
        }

        return address;
    }

    static String extractStatus(JsonElement response) {
        return response.getAsJsonObject().getAsJsonPrimitive("status").getAsString();
    }
}