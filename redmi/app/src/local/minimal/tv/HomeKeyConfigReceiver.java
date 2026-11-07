package local.minimal.tv;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Exported only to DUMP-authorized callers such as ADB shell. */
public final class HomeKeyConfigReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        if(!"local.minimal.tv.CONFIG_HOME".equals(intent.getAction())||!intent.hasExtra("enabled"))return;
        boolean enabled=intent.getBooleanExtra("enabled",false);
        HomeRepair.setEnabled(c,enabled);
        if(enabled){HomeRepair.repair(c,"adb-config");HomeRepair.schedule(c);}
        setResultCode(0);
    }
}
