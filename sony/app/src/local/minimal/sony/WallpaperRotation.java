package local.minimal.sony;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Shares one background and timer across the home pages and the app list. */
final class WallpaperRotation {
    private static final String[] WALLPAPERS={
        "wallpaper_alpine", "wallpaper_bamboo", "wallpaper_coast", "wallpaper_dunes", "wallpaper_aurora"
    };
    private static final long INTERVAL_MS=60*60*1000L;
    private static final int FADE_MS=650;

    private final ImageView picture;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable advance=this::updateAndSchedule;
    private final Runnable finishFade=this::showCurrent;
    private Drawable current;
    private int index=-1;
    private boolean running;

    WallpaperRotation(Context context) {
        picture=new ImageView(context);
        picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        picture.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void attachTo(FrameLayout stage) {
        if(picture.getParent() instanceof ViewGroup) ((ViewGroup)picture.getParent()).removeView(picture);
        if(current==null) updateWallpaper(System.currentTimeMillis());
        stage.addView(picture,new FrameLayout.LayoutParams(-1,-1));
    }

    void start() {
        if(current==null || running) return;
        running=true;
        updateAndSchedule();
    }

    void stop() {
        if(!running) return;
        running=false;
        handler.removeCallbacks(advance);
        handler.removeCallbacks(finishFade);
        showCurrent();
    }

    private Drawable loadCurrent() {
        Context context=picture.getContext();
        int id=context.getResources().getIdentifier(WALLPAPERS[index],"drawable",context.getPackageName());
        return context.getDrawable(id);
    }

    private void showCurrent() {
        picture.setImageDrawable(current);
    }

    private void updateAndSchedule() {
        long now=System.currentTimeMillis();
        updateWallpaper(now);
        long nextHour=now+INTERVAL_MS-Math.floorMod(now,INTERVAL_MS);
        handler.postDelayed(advance,Math.max(0,nextHour-System.currentTimeMillis()));
    }

    private void updateWallpaper(long now) {
        int next=(int)Math.floorMod(Math.floorDiv(now,INTERVAL_MS),WALLPAPERS.length);
        if(next==index) return;
        Drawable previous=current;
        index=next;
        current=loadCurrent();
        if(previous==null) {
            showCurrent();
        } else {
            TransitionDrawable fade=new TransitionDrawable(new Drawable[]{previous,current});
            picture.setImageDrawable(fade);
            fade.startTransition(FADE_MS);
            handler.postDelayed(finishFade,FADE_MS);
        }
        Log.i("SonyWallpapers","DISPLAY "+WALLPAPERS[index]);
    }
}
