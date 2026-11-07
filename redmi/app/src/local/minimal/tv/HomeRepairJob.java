package local.minimal.tv;
import android.app.job.JobParameters;
import android.app.job.JobService;

/** Checks the opted-in HOME binding through the system scheduler. */
public final class HomeRepairJob extends JobService {
    @Override public boolean onStartJob(JobParameters p) {
        HomeRepair.runJob(this,p.getJobId());return false;
    }
    @Override public boolean onStopJob(JobParameters p) {return false;}
}
