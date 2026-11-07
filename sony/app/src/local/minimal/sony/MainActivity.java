package local.minimal.sony;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextClock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** BRAVIA home screen with remote navigation and native TV playback. */
public final class MainActivity extends Activity {
    private static final int COLUMNS=4;
    private static final int PAGE_SIZE=8;
    private static final int APP_LIST_INDEX=10;
    private static final String FROM_HOME_KEY="android.intent.extra.FROM_HOME_KEY";
    private static final ComponentName SONY_TV=new ComponentName("com.sony.dtv.tvx","com.sony.dtv.tvx.MainActivity");
    private static final String EXTERNAL_INPUT="com.sony.dtv.tvinput.external/.ExternalTvInputService/HW";
    private static final String ORIGINAL_LAUNCHER="com.oversea.aslauncher";
    private final Shortcut[] shortcuts={
        new Shortcut("电视",3,this::openHdmi1),
        appShortcut("银河奇异果","com.gitvdemo.video"),
        appShortcut("VidHub","com.oumi.utility.media.hub"),
        appShortcut("Televizo","com.ottplay.ottplay"),
        appShortcut("B站","com.chinasoul.bt"),
        appShortcut("Clash Meta","com.github.metacubex.clash.meta"),
        appShortcut("SmartTube","org.smarttube.stable"),
        appShortcut("OwnTV","tv.own.owntv.captions.sony"),
        new Shortcut("信号源",0,this::showTvInputs),
        new Shortcut("电视设置",2,this::settings),
        new Shortcut("应用列表",1,this::showApps),
        appShortcut("Disney+","com.disney.disneyplus")
    };
    private boolean apps;
    private int selected = 0;
    private final List<View> homeButtons = new ArrayList<>();
    private WeatherHeader weather;
    private WallpaperRotation wallpaper;
    private boolean resumed;
    private FrameLayout startupScreen;
    private StartupHdmiProbe startupProbe;
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        wallpaper=new WallpaperRotation(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        openHomeIntent(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openHomeIntent(intent);
    }
    @Override protected void onResume(){
        super.onResume();
        resumed=true;
        if(startupScreen!=null) startStartupProbe();
        else {
            if(homeButtons.isEmpty()&&!apps) showHome();
            wallpaper.start();
        }
        if(weather!=null)weather.start();
    }
    @Override protected void onPause(){
        resumed=false;
        cancelStartupProbe();
        wallpaper.stop();
        if(weather!=null)weather.stop();
        super.onPause();
    }
    private void openHomeIntent(Intent intent) {
        cancelStartupProbe();
        if(!shouldCheckStartupSignal(intent)) {
            showHome();
            return;
        }
        if(weather!=null) weather.stop();
        weather=null;
        wallpaper.stop();
        apps=false;
        homeButtons.clear();
        startupScreen=new FrameLayout(this);
        startupScreen.setBackgroundColor(Color.BLACK);
        setContentView(startupScreen);
        if(resumed) startStartupProbe();
    }
    private boolean shouldCheckStartupSignal(Intent intent) {
        if(!Intent.ACTION_MAIN.equals(intent.getAction()) || !intent.hasCategory(Intent.CATEGORY_HOME)) return false;
        int boot=Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1);
        if(boot<0) return false;
        android.content.SharedPreferences state=getSharedPreferences("sony_startup",MODE_PRIVATE);
        if(state.getInt("last_home_boot",-1)==boot) return false;
        state.edit().putInt("last_home_boot",boot).apply();
        return !intent.getBooleanExtra(FROM_HOME_KEY,false);
    }
    private void startStartupProbe() {
        if(startupProbe!=null) return;
        startupProbe=new StartupHdmiProbe(startupScreen,EXTERNAL_INPUT+2,hasVideo->{
            cancelStartupProbe();
            if(!hasVideo || !launch(hdmiIntent(EXTERNAL_INPUT+2))) showHome();
        });
        startupProbe.start();
    }
    private void cancelStartupProbe() {
        if(startupProbe!=null) startupProbe.stop();
        startupProbe=null;
        startupScreen=null;
    }
    private LinearLayout base(String heading) {
        if(weather!=null){weather.stop();weather=null;}
        FrameLayout stage=new FrameLayout(this);
        stage.setBackgroundColor(Color.rgb(12,24,32));
        wallpaper.attachTo(stage);
        View shade=new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.argb(90,4,13,23),Color.argb(25,4,13,23),Color.argb(150,4,13,23)}));
        stage.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(28), dp(48), dp(26));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        if(heading.equals("极简桌面")){
            weather=new WeatherHeader(this);header.addView(weather,new LinearLayout.LayoutParams(0,-1,1));
        }else{
        TextView title = new TextView(this);
        title.setText(heading); title.setTextColor(Color.WHITE); title.setTextSize(28);
        title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        title.setShadowLayer(dp(5),0,dp(1),Color.argb(90,0,0,0));
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        }
        TextClock clock=new TextClock(this);clock.setFormat24Hour("HH:mm");clock.setFormat12Hour("HH:mm");
        clock.setTextSize(30);clock.setTextColor(Color.WHITE);clock.setGravity(Gravity.RIGHT);
        clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        header.addView(clock,new LinearLayout.LayoutParams(dp(130),-2));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(74)));
        stage.addView(root,new FrameLayout.LayoutParams(-1,-1));
        setContentView(stage);
        if(resumed) {
            wallpaper.start();
            if(weather!=null) weather.start();
        }
        return root;
    }
    private Button button(String label, Runnable click) {
        return configureButton(new Button(this),label,click);
    }
    private Button configureButton(Button b,String label,Runnable click){
        b.setText(label);b.setTextSize(b instanceof ShortcutButton?22:24);b.setAllCaps(false);
        b.setFocusable(true); b.setFocusableInTouchMode(true);
        b.setStateListAnimator(null);
        b.setPadding(dp(18), dp(12), dp(18), dp(12));
        b.setOnFocusChangeListener((v, focus) -> paint(b, focus));
        b.setOnClickListener(v -> click.run()); paint(b, false); return b;
    }
    private void paint(Button b, boolean focus) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,focus
            ?new int[]{Color.argb(248,247,252,253),Color.argb(230,224,239,242)}
            :new int[]{Color.argb(174,25,43,56),Color.argb(150,15,29,43)});
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(focus ? 3 : 1), focus ? Color.WHITE : Color.argb(65,213,234,241));
        b.setBackground(bg); b.setTextColor(focus ? Color.rgb(15,40,53) : Color.rgb(240,248,251));
        b.setTypeface(Typeface.create(focus?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        b.setElevation(dp(focus ? 10 : 2));
        if(b instanceof ShortcutButton)((ShortcutButton)b).setIconFocused(focus);
        for(Drawable icon:b.getCompoundDrawables())if(icon instanceof SystemIconDrawable)((SystemIconDrawable)icon).setFocused(focus);
    }
    private Drawable appIcon(String packageName){
        try{return getPackageManager().getApplicationIcon(packageName).mutate();}
        catch(Exception e){return new SystemIconDrawable(1);}
    }
    private Shortcut appShortcut(String label,String packageName) {
        return new Shortcut(label,packageName,()->openApp(label,packageName));
    }
    private static final class Shortcut {
        final String label,packageName;
        final int systemIcon;
        final Runnable action;
        Shortcut(String label,int systemIcon,Runnable action) {
            this.label=label;
            this.systemIcon=systemIcon;
            this.packageName=null;
            this.action=action;
        }
        Shortcut(String label,String packageName,Runnable action) {
            this.label=label;
            this.systemIcon=0;
            this.packageName=packageName;
            this.action=action;
        }
    }
    private void showHome() {
        int initialSelection=selected;
        int start=selected/PAGE_SIZE*PAGE_SIZE;
        int end=Math.min(start+PAGE_SIZE,shortcuts.length);
        apps = false; homeButtons.clear();
        LinearLayout root = base("极简桌面");
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL); grid.setGravity(Gravity.CENTER);
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout row=null;
        for (int i=start; i<end; i++) {
            if(i%COLUMNS==0) {
                row=new LinearLayout(this); row.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,dp(154));
                rowParams.setMargins(0,dp(8),0,dp(8)); grid.addView(row,rowParams);
            }
            final int index=i;
            Shortcut shortcut=shortcuts[i];
            Drawable icon=shortcut.packageName==null?new SystemIconDrawable(shortcut.systemIcon):appIcon(shortcut.packageName);
            Button b=configureButton(new ShortcutButton(this,icon),shortcut.label,shortcut.action);
            b.setId(View.generateViewId());
            b.setOnFocusChangeListener((v,f)->{paint(b,f); if(f) selected=index;});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0, dp(154), 1);
            lp.setMargins(dp(8),0,dp(8),0); row.addView(b,lp); homeButtons.add(b);
        }
        for(int i=0;i<homeButtons.size();i++) {
            View b=homeButtons.get(i);
            int column=i%COLUMNS;
            b.setNextFocusLeftId(homeButtons.get(column==0?i:i-1).getId());
            b.setNextFocusRightId(homeButtons.get(column==COLUMNS-1?i:i+1).getId());
            b.setNextFocusUpId(homeButtons.get(i<COLUMNS?i:i-COLUMNS).getId());
            b.setNextFocusDownId(homeButtons.get(i+COLUMNS<homeButtons.size()?i+COLUMNS:i).getId());
        }
        if(start==0) weather.connectToHomeGrid(homeButtons.get(0));
        TextView page=new TextView(this);
        page.setText(start==0?"1 / 2   ·   ↓ 下一页":"2 / 2   ·   ↑ 上一页");
        page.setTextSize(14);
        page.setTextColor(Color.argb(230,233,245,251));
        page.setGravity(Gravity.CENTER);
        root.addView(page,new LinearLayout.LayoutParams(-1,dp(26)));
        homeButtons.get(initialSelection-start).requestFocus();
    }
    private boolean launch(Intent i) {
        try { startActivity(i); Log.i("MinimalTV", "START_OK uid="+android.os.Process.myUid()+" "+i); return true; }
        catch (RuntimeException e) {
            Log.e("MinimalTV", "START_FAILED", e);
            new AlertDialog.Builder(this).setTitle("无法打开").setMessage(e.getClass().getSimpleName()+"\n"+e.getMessage())
                .setPositiveButton("返回",null).show();
            return false;
        }
    }
    private void showTvInputs() {
        String[] inputs={"索尼电视（上次信号源）","HDMI 1","HDMI 2","HDMI 3","HDMI 4"};
        new AlertDialog.Builder(this).setTitle("信号源").setItems(inputs,(dialog,choice)->{
            if(choice==0) {
                launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setComponent(SONY_TV));
            } else {
                openHdmi(choice);
            }
        }).setNegativeButton("返回",null).show();
    }
    private void openHdmi1() { openHdmi(1); }
    private Intent hdmiIntent(String inputId) {
        return new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(inputId)).setComponent(SONY_TV);
    }
    private void openHdmi(int port) {
        // BRAVIA VH1 maps HDMI 1–4 to hardware IDs 2–5.
        String inputId=EXTERNAL_INPUT+(port+1);
        TvInputManager manager=getSystemService(TvInputManager.class);
        TvInputInfo input=manager==null?null:manager.getTvInputInfo(inputId);
        if(input==null || input.getType()!=TvInputInfo.TYPE_HDMI || !input.isPassthroughInput()) {
            new AlertDialog.Builder(this).setTitle("HDMI "+port).setMessage("请用遥控器的输入键选择此信号源。")
                .setPositiveButton("返回",null).show();
            return;
        }
        launch(hdmiIntent(inputId));
    }
    private void settings() { launch(new Intent(Settings.ACTION_SETTINGS)); }
    private void openApp(String title,String packageName) {
        Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(packageName);
        if(i==null)i=getPackageManager().getLaunchIntentForPackage(packageName);
        if(i==null) {
            new AlertDialog.Builder(this).setTitle("无法打开 "+title)
                .setMessage("请确认此应用已安装并启用。")
                .setPositiveButton("返回",null).show(); return;
        }
        launch(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void showApps() {
        apps=true;
        LinearLayout root=base("应用列表");
        ScrollView scroll=new ScrollView(this); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        Button original=button("原桌面",()->openApp("原桌面",ORIGINAL_LAUNCHER));
        listIcon(original,appIcon(ORIGINAL_LAUNCHER));addRow(list,original);
        LinkedHashMap<String,ResolveInfo> map=new LinkedHashMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            Intent query=new Intent(Intent.ACTION_MAIN).addCategory(category);
            for(ResolveInfo ri:getPackageManager().queryIntentActivities(query,0)) {
                if(!ri.activityInfo.packageName.equals(getPackageName()) && !ri.activityInfo.packageName.equals(ORIGINAL_LAUNCHER))
                    map.putIfAbsent(ri.activityInfo.packageName,ri);
            }
        }
        ArrayList<ResolveInfo> sorted=new ArrayList<>(map.values());
        sorted.sort((a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
        for(ResolveInfo ri:sorted) {
            Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(new ComponentName(ri.activityInfo.packageName,ri.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Button entry=button(ri.loadLabel(getPackageManager()).toString(),()->launch(i));
            listIcon(entry,ri.loadIcon(getPackageManager()));addRow(list,entry);
        }
        list.getChildAt(0).requestFocus();
    }
    private void addRow(LinearLayout list,View v) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(68)); lp.setMargins(dp(4),dp(5),dp(4),dp(5)); list.addView(v,lp);
    }
    private void listIcon(Button b,Drawable icon){
        icon.setBounds(0,0,dp(40),dp(40));b.setCompoundDrawables(icon,null,null,null);
        b.setCompoundDrawablePadding(dp(18));b.setTextSize(21);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
    }
    @Override public void onBackPressed() {
        if(startupScreen!=null) { cancelStartupProbe(); showHome(); }
        else if(apps) { selected=APP_LIST_INDEX; showHome(); }
        else if(selected>=PAGE_SIZE) { selected-=COLUMNS; showHome(); }
        else if(!homeButtons.isEmpty()) { selected=0; homeButtons.get(0).requestFocus(); }
    }
    @Override public boolean onKeyDown(int code,KeyEvent event) {
        if(code==KeyEvent.KEYCODE_BACK) { onBackPressed(); return true; }
        if(startupScreen!=null) { cancelStartupProbe(); showHome(); return true; }
        if(!apps && homeButtons.contains(getCurrentFocus())) {
            if(code==KeyEvent.KEYCODE_DPAD_DOWN && selected>=PAGE_SIZE-COLUMNS && selected<PAGE_SIZE) {
                selected+=COLUMNS;
                showHome();
                return true;
            }
            if(code==KeyEvent.KEYCODE_DPAD_UP && selected>=PAGE_SIZE) {
                selected-=COLUMNS;
                showHome();
                return true;
            }
        }
        return super.onKeyDown(code,event);
    }
}
