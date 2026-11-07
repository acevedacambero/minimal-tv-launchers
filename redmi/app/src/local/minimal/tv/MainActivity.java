package local.minimal.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextClock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Minimal UI; HOME persistence is handled separately by the opted-in recovery components. */
public final class MainActivity extends Activity {
    private static final int APP_LIST_INDEX=3;
    private boolean apps;
    private View appEntry;
    private int selected = 0;
    private final List<View> homeButtons = new ArrayList<>();
    private WeatherHeader weather;
    private boolean resumed;
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        showHome();
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); showHome(); }
    @Override protected void onResume(){
        super.onResume();
        resumed=true;
        if(HomeRepair.enabled(this))HomeRepair.repair(this,"desktop-open");
        if(weather!=null)weather.start();
    }
    @Override protected void onPause(){resumed=false;if(weather!=null)weather.stop();super.onPause();}
    private LinearLayout base(String heading) {
        if(weather!=null){weather.stop();weather=null;}
        FrameLayout stage=new FrameLayout(this);
        stage.setBackgroundColor(Color.rgb(12,24,32));
        int wallpaper=getResources().getIdentifier("wallpaper_lake","drawable",getPackageName());
        if(wallpaper!=0){
            ImageView picture=new ImageView(this);picture.setImageResource(wallpaper);
            picture.setScaleType(ImageView.ScaleType.CENTER_CROP);picture.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            stage.addView(picture,new FrameLayout.LayoutParams(-1,-1));
        }
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
        setContentView(stage);if(weather!=null&&resumed)weather.start();return root;
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
    private void showHome() {
        apps = false; homeButtons.clear();
        LinearLayout root = base("极简桌面");
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL); grid.setGravity(Gravity.CENTER);
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        String[] names = {"HDMI 1", "显示器设置", "OwnTV 字幕开发版", "应用列表", "BT", "Clash Meta", "SmartTube", "Televizo"};
        Runnable[] actions = {this::hdmi, this::monitorSettings,
            ()->openApp("OwnTV 字幕开发版","tv.own.owntv.captions.dev"),this::showApps,
            ()->openApp("BT","com.chinasoul.bt"),
            ()->openApp("Clash Meta","com.github.metacubex.clash.meta"),
            ()->openApp("SmartTube","com.teamsmart.videomanager.tv"),
            ()->openApp("Televizo","com.ottplay.ottplay")};
        String[] iconPackages={"com.chinasoul.bt","com.github.metacubex.clash.meta","com.teamsmart.videomanager.tv","com.ottplay.ottplay"};
        int[] systemIcons={0,3,2,1};
        final int columns=4;
        LinearLayout row=null;
        for (int i=0; i<names.length; i++) {
            if(i%columns==0) {
                row=new LinearLayout(this); row.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,dp(154));
                rowParams.setMargins(0,dp(8),0,dp(8)); grid.addView(row,rowParams);
            }
            final int index=i;
            Drawable icon=i==2?appIcon("tv.own.owntv.captions.dev"):(i<4?new SystemIconDrawable(systemIcons[i]):appIcon(iconPackages[i-4]));
            Button b=configureButton(new ShortcutButton(this,icon),names[i],actions[i]);b.setId(View.generateViewId());
            if(i==2)b.setTextSize(20);
            b.setOnFocusChangeListener((v,f)->{paint(b,f); if(f) selected=index;});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0, dp(154), 1);
            lp.setMargins(dp(8),0,dp(8),0); row.addView(b,lp); homeButtons.add(b);
            if(i==APP_LIST_INDEX) appEntry=b;
        }
        for(int i=0;i<homeButtons.size();i++) {
            View b=homeButtons.get(i);
            int column=i%columns;
            b.setNextFocusLeftId(homeButtons.get(column==0?i:i-1).getId());
            b.setNextFocusRightId(homeButtons.get(column==columns-1?i:i+1).getId());
            b.setNextFocusUpId(homeButtons.get(i<columns?i:i-columns).getId());
            b.setNextFocusDownId(homeButtons.get(i+columns<homeButtons.size()?i+columns:i).getId());
        }
        weather.connectToHomeGrid(homeButtons.get(0));
        homeButtons.get(Math.max(0,Math.min(homeButtons.size()-1,selected))).requestFocus();
    }
    private void launch(Intent i) {
        try { startActivity(i); Log.i("MinimalTV", "START_OK uid="+android.os.Process.myUid()+" "+i); }
        catch (RuntimeException e) {
            Log.e("MinimalTV", "START_FAILED", e);
            new AlertDialog.Builder(this).setTitle("无法打开").setMessage(e.getClass().getSimpleName()+"\n"+e.getMessage()+"\n可从应用列表打开原厂桌面恢复操作。")
                .setPositiveButton("返回",null).show();
        }
    }
    private void hdmi() {
        // Verified for MiTV-MFFU1 / OS3.0.109.0.UFFMAMN. Vendor source 23 != hardware ID 5.
        if (!"MiTV-MFFU1".equals(android.os.Build.MODEL) || !"OS3.0.109.0.UFFMAMN".equals(android.os.Build.DISPLAY)) {
            new AlertDialog.Builder(this).setTitle("需要重新验证信号源")
              .setMessage("型号或系统版本已变化，请通过原厂桌面选择 HDMI 1，并重新审计跳转参数。")
              .setPositiveButton("打开原厂桌面",(d,w)->factory()).setNegativeButton("返回",null).show(); return;
        }
        Intent i=new Intent("com.xiaomi.mitv.tvplayer.EXTSRC_PLAY");
        i.setComponent(new ComponentName("com.xiaomi.mitv.tvplayer","com.xiaomi.mitv.tvplayer.ExternalSourceActivity"));
        i.putExtra("input",23); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); launch(i);
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
    private void monitorSettings() {
        launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(new ComponentName("com.xiaomi.mitv.settings","com.xiaomi.mitv.settings.entry.MainActivity"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void factory() {
        HomeKeyService.restoreFactoryHome(this);
        // MainActivity is the exported vendor trampoline; UserMode requires signature HOME_STATE.
        launch(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName("com.mitv.tvhome","com.mitv.tvhome.MainActivity")));
    }
    private void showApps() {
        apps=true;
        LinearLayout root=base("应用列表");
        ScrollView scroll=new ScrollView(this); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        Button recovery=button("原厂桌面（恢复入口）",this::factory);
        listIcon(recovery,new SystemIconDrawable(4));addRow(list,recovery);
        LinkedHashMap<String,ResolveInfo> map=new LinkedHashMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            Intent query=new Intent(Intent.ACTION_MAIN).addCategory(category);
            for(ResolveInfo ri:getPackageManager().queryIntentActivities(query,0)) {
                if(!ri.activityInfo.packageName.equals(getPackageName()) && !ri.activityInfo.packageName.equals("com.mitv.tvhome"))
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
        if(apps) { selected=APP_LIST_INDEX; showHome(); appEntry.requestFocus(); }
        else { selected=0; homeButtons.get(0).requestFocus(); }
    }
    @Override public boolean onKeyDown(int code,KeyEvent event) {
        if(code==KeyEvent.KEYCODE_BACK) { onBackPressed(); return true; }
        return super.onKeyDown(code,event);
    }
}
