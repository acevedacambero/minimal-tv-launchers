package local.minimal.tv;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class HomeBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent) {
        String action=intent.getAction();
        if(!Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
            && !Intent.ACTION_BOOT_COMPLETED.equals(action)
            && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        Log.i("MinimalHomeBoot","BOOT_OR_UPDATE action="+action+" optedIn="+HomeRepair.enabled(c));
        HomeRepair.repair(c,action);
        HomeRepair.schedule(c);
    }
}
