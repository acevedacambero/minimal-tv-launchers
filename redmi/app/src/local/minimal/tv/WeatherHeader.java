package local.minimal.tv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fixed-city, key-free weather; no location access, background polling, or UI-thread networking. */
public final class WeatherHeader extends LinearLayout {
    public static final String ENDPOINT="https://api.open-meteo.com/v1/forecast?latitude=30.5928&longitude=114.3055&current=temperature_2m,weather_code,is_day&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=Asia%2FShanghai&forecast_days=2";
    private static final ZoneId ZONE=ZoneId.of("Asia/Shanghai");
    private static final long REFRESH=15*60*1000L,RETRY=5*60*1000L;
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"MinimalWeather");t.setDaemon(true);return t;});
    private static final AtomicBoolean FETCHING=new AtomicBoolean();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;
    private final TextView current,forecast,dataTime;
    private boolean active;
    private final Runnable tick=new Runnable(){@Override public void run(){if(!active)return;render();requestIfDue();ui.postDelayed(this,60000);}};
    public WeatherHeader(Context c){
        super(c);setOrientation(VERTICAL);setGravity(android.view.Gravity.CENTER_VERTICAL);
        prefs=c.getApplicationContext().getSharedPreferences("wuhan_weather",0);
        current=line(c,24,Color.WHITE,"sans-serif-medium");
        forecast=line(c,13,Color.argb(235,242,249,252),"sans-serif");
        int muted=Color.argb(185,225,238,244);
        dataTime=line(c,10,muted,"sans-serif");
        dataTime.setId(View.generateViewId());
        dataTime.setOnClickListener(v->showDataDetails());
        dataTime.setFocusable(true);
        dataTime.setOnFocusChangeListener((v,focused)->dataTime.setTextColor(focused?Color.WHITE:muted));
        addView(current);
        addView(forecast);
        addView(dataTime,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
        render();
    }
    void connectToHomeGrid(View firstShortcut) {
        firstShortcut.setNextFocusUpId(dataTime.getId());
        dataTime.setNextFocusDownId(firstShortcut.getId());
    }
    private void showDataDetails() {
        AlertDialog details=new AlertDialog.Builder(getContext())
            .setTitle("天气数据")
            .setMessage("Open-Meteo\nhttps://open-meteo.com/\n\nCC BY 4.0\nhttps://creativecommons.org/licenses/by/4.0/\n\n温度取整，天气代码转为中文描述。")
            .setPositiveButton("返回",null)
            .show();
        Linkify.addLinks((TextView)details.findViewById(android.R.id.message),Linkify.WEB_URLS);
    }
    private TextView line(Context c,int size,int color,String font){
        TextView v=new TextView(c);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(Typeface.create(font,Typeface.NORMAL));v.setSingleLine(true);return v;
    }
    public void start(){if(active)return;active=true;tick.run();}
    public void stop(){active=false;ui.removeCallbacks(tick);}
    private void render(){
        try{
            Data d=new Data(prefs.getString("json",""));
            boolean cached=prefs.getBoolean("failed",false)||System.currentTimeMillis()-d.observedAt>30*60*1000L;
            current.setText("武汉  "+rounded(d.temperature)+"°C  "+condition(d.code));
            String today=d.date.equals(LocalDate.now(ZONE).toString())?"今日":d.date.substring(5).replace('-','/');
            String tomorrow=today.equals("今日")?"明日":d.tomorrow.substring(5).replace('-','/');
            forecast.setText(today+" "+condition(d.todayCode)+" "+rounded(d.low)+"–"+rounded(d.high)+"°C   ·   "+tomorrow+" "+condition(d.tomorrowCode)+" "+rounded(d.tomorrowLow)+"–"+rounded(d.tomorrowHigh)+"°C");
            String timestamp=d.time.format(DateTimeFormatter.ofPattern("HH:mm"))+" 数据"+(cached?" · 缓存":"");
            dataTime.setText(timestamp);
            dataTime.setContentDescription(timestamp+"，查看天气数据详情");
            dataTime.setVisibility(VISIBLE);
        }catch(Exception missing){
            current.setText(prefs.getBoolean("failed",false)?"武汉天气暂不可用":"武汉天气更新中");
            forecast.setText("联网后自动更新实况与今明预报");
            dataTime.setVisibility(INVISIBLE);
        }
    }
    private void requestIfDue(){
        long now=System.currentTimeMillis();
        if(now-prefs.getLong("success",0)<REFRESH && !prefs.getBoolean("failed",false))return;
        if(now-prefs.getLong("attempt",0)<RETRY || !FETCHING.compareAndSet(false,true))return;
        prefs.edit().putLong("attempt",now).apply();
        WORK.execute(()->{
            HttpsURLConnection connection=null;
            try{
                connection=(HttpsURLConnection)new URL(ENDPOINT).openConnection();
                connection.setConnectTimeout(8000);connection.setReadTimeout(8000);connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("User-Agent","MinimalTV/1.7 WuhanWeather");
                if(connection.getResponseCode()!=200)throw new java.io.IOException("HTTP "+connection.getResponseCode());
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream in=connection.getInputStream()){
                    byte[] buffer=new byte[2048];int n;
                    while((n=in.read(buffer))!=-1){if(bytes.size()+n>65536)throw new java.io.IOException("Response too large");bytes.write(buffer,0,n);}
                }
                String json=new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
                Data d=new Data(json);
                long age=System.currentTimeMillis()-d.observedAt;
                if(age>3*60*60*1000L||age< -2*60*60*1000L||!d.date.equals(LocalDate.now(ZONE).toString()))throw new java.io.IOException("Weather data time mismatch");
                prefs.edit().putString("json",json).putLong("success",System.currentTimeMillis()).putBoolean("failed",false).apply();
                Log.i("MinimalWeather","UPDATED city=Wuhan time="+d.time+" temp="+d.temperature+" code="+d.code);
            }catch(Exception e){prefs.edit().putBoolean("failed",true).apply();Log.w("MinimalWeather","UPDATE_FAILED "+e.getClass().getSimpleName()+": "+e.getMessage());}
            finally{if(connection!=null)connection.disconnect();FETCHING.set(false);ui.post(()->{if(active)render();});}
        });
    }
    private static long rounded(double value){return Math.round(value);}
    public static String condition(int c){
        if(c==0)return "晴";if(c==1)return "晴间多云";if(c==2)return "多云";if(c==3)return "阴";
        if(c==45||c==48)return "雾";if(c>=51&&c<=55)return "毛毛雨";if(c==56||c==57||c==66||c==67)return "冻雨";
        if(c==61)return "小雨";if(c==63)return "中雨";if(c==65)return "大雨";
        if(c==71)return "小雪";if(c==73)return "中雪";if(c==75)return "大雪";if(c==77)return "雪粒";
        if(c==80)return "小阵雨";if(c==81)return "阵雨";if(c==82)return "强阵雨";
        if(c==85||c==86)return "阵雪";if(c>=95&&c<=99)return "雷雨";return "天气变化";
    }
    private static final class Data {
        final double temperature,low,high,tomorrowLow,tomorrowHigh;
        final int code,todayCode,tomorrowCode;
        final String date,tomorrow;
        final LocalDateTime time;
        final long observedAt;
        Data(String json)throws Exception{
            JSONObject root=new JSONObject(json),cur=root.getJSONObject("current"),daily=root.getJSONObject("daily");
            temperature=cur.getDouble("temperature_2m");code=cur.getInt("weather_code");
            time=LocalDateTime.parse(cur.getString("time"));observedAt=time.atZone(ZONE).toInstant().toEpochMilli();
            JSONArray dates=daily.getJSONArray("time"),mins=daily.getJSONArray("temperature_2m_min"),maxs=daily.getJSONArray("temperature_2m_max"),codes=daily.getJSONArray("weather_code");
            date=dates.getString(0);tomorrow=dates.getString(1);low=mins.getDouble(0);high=maxs.getDouble(0);tomorrowLow=mins.getDouble(1);tomorrowHigh=maxs.getDouble(1);todayCode=codes.getInt(0);tomorrowCode=codes.getInt(1);
            if(!Double.isFinite(temperature)||temperature< -70||temperature>65||code<0||code>99||low>high||tomorrowLow>tomorrowHigh)throw new java.io.IOException("Invalid weather data");
        }
    }
}
