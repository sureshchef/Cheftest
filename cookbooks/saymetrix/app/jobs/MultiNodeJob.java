package jobs;

import org.joda.time.DateTime;
import org.joda.time.Period;
import play.Logger;
import play.cache.Cache;
import play.jobs.Job;

/**
 * Ensures that a job will only ever concurrently run on a single node in a cluster.
 */
public abstract class MultiNodeJob extends Job {
    private final String className = this.getClass().getSimpleName();
    protected final String cacheName = /*Play.id + */className;

    public abstract void execute();

    public void doJob() {
        boolean isRunning = Cache.get(cacheName) != null;
        if (isRunning) {
            Logger.debug("%s cancelled - already running on another node", className);
            return;
        }
        DateTime timestamp = DateTime.now();
        // Possible race condition here
        Cache.safeSet(cacheName, DateTime.now().toString(), "10mn");

        try {
            /*
             * In an effort to ensure that two nodes don't attempt to run a job
             * at *exactly* the same instant wait a random period before actually
             * starting the job.
             */
            long delay = (long) (Math.random() * 2000);
            Thread.sleep(delay);
            Logger.debug("%s start", className);
            execute();
        } catch (InterruptedException e) {
        } finally {
            if (!isRunning) {
                Cache.safeDelete(cacheName);
            }
        }
        Logger.debug("%s end (%s)", className, new Period(timestamp, DateTime.now()));
    }
}
