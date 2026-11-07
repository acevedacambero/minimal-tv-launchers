package local.minimal.tv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Handle physical HOME, with the OEM home-window observer retained as a fallback. */
public final class HomeKeyService extends AccessibilityService {
    private static HomeKeyService active;
    private boolean redirectEnabled;
    private long lastLaunch;
    private final Handler main=new Handler(Looper.getMainLooper());
    @Override protected void onServiceConnected() {
        active=this;
        redirectEnabled=true;
        AccessibilityServiceInfo info=getServiceInfo();
        info.flags|=AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
        // Also arm bounded checks when the system binds us, even if OEM boot broadcasts are delayed.
        HomeRepair.schedule(this);
        Log.i("MinimalHomeKey","CONNECTED uid="+android.os.Process.myUid()+" keyFilter=true");
    }
    @Override protected boolean onKeyEvent(KeyEvent event){
        if(!redirectEnabled)return false;
        InputDevice device=event.getDevice();
        boolean xiaomiRemoteHome=event.getScanCode()==102 && device!=null
            && device.getVendorId()==0x2717 && device.getProductId()==0x32b8;
        if(event.getKeyCode()!=KeyEvent.KEYCODE_HOME && !xiaomiRemoteHome)return false;
        Log.i("MinimalHomeKey","PHYSICAL_HOME key="+event.getKeyCode()+" scan="+event.getScanCode()+" action="+event.getAction());
        if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())main.post(()->openDesktop("PHYSICAL_HOME_HANDLED"));
        return true;
    }
    private void openDesktop(String reason){
        if(!redirectEnabled)return;
        long now=SystemClock.uptimeMillis();
        if(now-lastLaunch<350)return;
        lastLaunch=now;
        try{
            startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));
            Log.i("MinimalHomeKey",reason);
        }catch(RuntimeException e){Log.e("MinimalHomeKey","HOME_FAILED",e);}
    }
    public static void restoreFactoryHome(Context context) {
        HomeRepair.setEnabled(context,false);
        if(active!=null){active.redirectEnabled=false;active.disableSelf();active=null;}
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(!redirectEnabled || event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            || !"com.mitv.tvhome".contentEquals(event.getPackageName()==null?"":event.getPackageName())) return;
        String cls=event.getClassName()==null?"":event.getClassName().toString();
        Log.i("MinimalHomeKey","FACTORY_WINDOW "+cls);
        if(cls.equals("com.mitv.tvhome.MainActivityUserMode") || cls.equals("com.mitv.tvhome.MainActivity")) {
            openDesktop("HOME_WINDOW_REDIRECTED");
        }
    }
    @Override public void onInterrupt() { }
}
