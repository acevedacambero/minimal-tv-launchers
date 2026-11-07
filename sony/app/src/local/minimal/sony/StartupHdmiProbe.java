package local.minimal.sony;

import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.FrameLayout;

/** Checks for a usable HDMI picture in a muted session before the launcher is drawn. */
final class StartupHdmiProbe {
    interface Result {
        void onSignalChecked(boolean hasVideo);
    }

    private static final long TIMEOUT_MS=6500;
    private static final long STABLE_MS=750;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final FrameLayout parent;
    private final String inputId;
    private final Result result;
    private final Runnable timeout=()->finish(false,"timeout");
    private final Runnable poll=this::checkSignal;
    private TvView preview;
    private TvInputManager manager;
    private int inputState=TvInputManager.INPUT_STATE_DISCONNECTED;
    private boolean tuned,videoAvailable;
    private long stableSince=-1;
    private boolean stopped;

    StartupHdmiProbe(FrameLayout parent,String inputId,Result result) {
        this.parent=parent;
        this.inputId=inputId;
        this.result=result;
    }

    void start() {
        manager=parent.getContext().getSystemService(TvInputManager.class);
        preview=new TvView(parent.getContext());
        preview.setCallback(new TvView.TvInputCallback() {
            @Override public void onVideoAvailable(String id) {
                if(inputId.equals(id)) videoAvailable=true;
            }
            @Override public void onVideoUnavailable(String id,int reason) {
                if(!inputId.equals(id)) return;
                videoAvailable=false;
                stableSince=-1;
                Log.i("SonyStartupHdmi","VIDEO_UNAVAILABLE reason="+reason);
            }
            @Override public void onConnectionFailed(String id) {
                finish(false,"connection-failed");
            }
            @Override public void onDisconnected(String id) {
                finish(false,"disconnected");
            }
        });
        parent.addView(preview,new FrameLayout.LayoutParams(1,1));
        preview.setStreamVolume(0);
        handler.postDelayed(timeout,TIMEOUT_MS);
        Log.i("SonyStartupHdmi","CHECKING "+inputId);
        checkSignal();
    }

    private void checkSignal() {
        if(manager!=null && manager.getTvInputInfo(inputId)!=null) inputState=manager.getInputState(inputId);
        // This firmware can report video available even for a port in standby.
        if(inputState!=TvInputManager.INPUT_STATE_CONNECTED) {
            stableSince=-1;
        } else if(!tuned) {
            tuned=true;
            try {
                preview.tune(inputId,TvContract.buildChannelUriForPassthroughInput(inputId));
            } catch(RuntimeException e) {
                Log.w("SonyStartupHdmi","TUNE_FAILED",e);
                finish(false,"tune-failed");
                return;
            }
        } else if(videoAvailable) {
            long now=SystemClock.uptimeMillis();
            if(stableSince<0) stableSince=now;
            if(now-stableSince>=STABLE_MS) {
                finish(true,"stable-video");
                return;
            }
        }
        handler.postDelayed(poll,250);
    }

    void stop() {
        if(stopped) return;
        stopped=true;
        handler.removeCallbacks(timeout);
        handler.removeCallbacks(poll);
        preview.setCallback(null);
        preview.reset();
        parent.removeView(preview);
    }

    private void finish(boolean hasVideo,String reason) {
        if(stopped) return;
        stop();
        Log.i("SonyStartupHdmi","CHECKED hasVideo="+hasVideo+" reason="+reason+" inputState="+inputState);
        result.onSignalChecked(hasVideo);
    }
}
