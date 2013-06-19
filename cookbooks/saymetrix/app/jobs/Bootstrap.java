package jobs;

import models.SummaryStat;
import models.WebUser;
import org.joda.time.DateTimeZone;
import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.test.Fixtures;

@OnApplicationStart
public class Bootstrap extends Job {

    @Override
    public void doJob() {
        // Ensure all date handling is in UTC
        DateTimeZone.setDefault(DateTimeZone.UTC);

        if(Play.mode.isProd() && !"enabled".equals(Play.configuration.getProperty("memcached"))) {
            Logger.error("Memcached not configured but is required in clustered environments.");
        }

        // If it's empty, initialize the database with some reference data
        if (WebUser.count() == 0) {
            Logger.info("Loading reference data");
            Fixtures.deleteAllModels();
            Fixtures.loadModels("initial-data-prod.yml");

            // If we're in development then add a bit more than the bare data
            if (Play.mode.isDev()) {
                Fixtures.loadModels("initial-data-dev.yml");
            }
        }
        // If there are no SummaryStats, run the Summarizer
        if(Play.mode.isProd() & SummaryStat.count() == 0) {
            new SummarizerJob().now();
        }
    }
}
