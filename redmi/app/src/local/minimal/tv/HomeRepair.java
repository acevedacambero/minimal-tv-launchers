package local.minimal.tv;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

/** Explicitly opted-in repair of this app's own accessibility component only. */
public final class HomeRepair {
    private static final String PREFS="home_recovery";
    private static final int FIRST_JOB=7410;
    private static final int RECOVERY_JOB=7413;
    private static final long RECOVERY_INTERVAL=15*60*1000L;
    private static final long[] DELAYS={10000,30000,90000};
    private static SharedPreferences preferences(Context c) {
        Context storage=c.createDeviceProtectedStorageContext();
        SharedPreferences preferences=storage.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!preferences.contains("enabled") && c.getSystemService(UserManager.class).isUserUnlocked()) {
            // Migrate the existing opt-in only after credential storage is available.
            SharedPreferences previous=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
            if(previous.contains("enabled")
                && !preferences.edit().putBoolean("enabled",previous.getBoolean("enabled",false)).commit())
                Log.e("MinimalHomeBoot","PREFERENCE_MIGRATION_FAILED");
        }
        return preferences;
    }
    public static boolean enabled(Context c) {return preferences(c).getBoolean("enabled",false);}
    public static void setEnabled(Context c,boolean enabled) {
        if(!preferences(c).edit().putBoolean("enabled",enabled).commit())
            throw new IllegalStateException("Cannot save HOME recovery preference");
        if(!enabled) {
            JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            for(int i=0;i<DELAYS.length;i++)jobs.cancel(FIRST_JOB+i);
            jobs.cancel(RECOVERY_JOB);
        }
        Log.i("MinimalHomeBoot","CONFIG enabled="+enabled);
    }
    public static synchronized void repair(Context c,String reason) {
        if(!enabled(c)){Log.i("MinimalHomeBoot","SKIP disabled reason="+reason);return;}
        if(c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)!=PackageManager.PERMISSION_GRANTED) {
            Log.e("MinimalHomeBoot","MISSING_WRITE_SECURE_SETTINGS");return;
        }
        ComponentName own=new ComponentName(c,HomeKeyService.class);
        String old=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean found=false;
        if(old!=null)for(String item:old.split(":"))if(own.equals(ComponentName.unflattenFromString(item)))found=true;
        boolean globalOff=Settings.Secure.getInt(c.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,0)!=1;
        try {
            if(!found) {
                String merged=(old==null||old.isEmpty()?"":old+":")+own.flattenToString();
                if(!Settings.Secure.putString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,merged)) {
                    Log.e("MinimalHomeBoot","COMPONENT_RESTORE_FAILED reason="+reason);
                    return;
                }
            }
            if(globalOff && !Settings.Secure.putInt(c.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,1)) {
                Log.e("MinimalHomeBoot","ACCESSIBILITY_ENABLE_FAILED reason="+reason);
                return;
            }
        } catch(SecurityException e) {
            Log.e("MinimalHomeBoot","REPAIR_DENIED reason="+reason,e);
            return;
        }
        if(!found || globalOff || !(reason.equals("recovery-job") || reason.equals("desktop-open")))
            Log.i("MinimalHomeBoot","REPAIR reason="+reason+" componentAdded="+!found+" globalReenabled="+globalOff);
    }
    public static void schedule(Context c) {
        if(!enabled(c))return;
        JobScheduler jobs=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        ComponentName service=new ComponentName(c,HomeRepairJob.class);
        if(jobs.getPendingJob(RECOVERY_JOB)==null) {
            JobInfo recovery=new JobInfo.Builder(RECOVERY_JOB,service)
                .setPersisted(true).setPeriodic(RECOVERY_INTERVAL).build();
            if(jobs.schedule(recovery)!=JobScheduler.RESULT_SUCCESS)
                Log.e("MinimalHomeBoot","SCHEDULE_FAILED "+RECOVERY_JOB);
        }
        for(int i=0;i<DELAYS.length;i++) {
            JobInfo job=new JobInfo.Builder(FIRST_JOB+i,service)
                .setMinimumLatency(DELAYS[i]).setOverrideDeadline(DELAYS[i]+15000).build();
            if(jobs.schedule(job)!=JobScheduler.RESULT_SUCCESS)Log.e("MinimalHomeBoot","SCHEDULE_FAILED "+(FIRST_JOB+i));
        }
    }
    static void runJob(Context c,int id) {
        repair(c,id==RECOVERY_JOB?"recovery-job":"startup-job-"+id);
    }
}
