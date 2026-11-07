package local.minimal.tv;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Four consistent, resolution-independent system shortcut glyphs, plus the recovery home. */
public final class SystemIconDrawable extends Drawable {
    private final int kind;
    private boolean focused;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    public SystemIconDrawable(int kind){this.kind=kind;}
    public void setFocused(boolean value){focused=value;invalidateSelf();}
    @Override public void draw(Canvas c){
        c.save();c.translate(getBounds().left,getBounds().top);
        c.scale(getBounds().width()/64f,getBounds().height()/64f);
        p.setStyle(Paint.Style.FILL);p.setColor(focused?Color.argb(24,14,78,91):Color.argb(35,151,223,228));
        c.drawRoundRect(0,0,64,64,18,18,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(focused?Color.rgb(19,77,89):Color.rgb(207,242,244));
        if(kind==0){
            Path path=new Path();path.moveTo(13,22);path.lineTo(51,22);path.lineTo(51,34);path.lineTo(43,43);path.lineTo(21,43);path.lineTo(13,34);path.close();c.drawPath(path,p);
            c.drawLine(21,28,43,28,p);for(int x=23;x<=41;x+=6)c.drawLine(x,33,x,36,p);
        }else if(kind==1){
            for(int y=15;y<=35;y+=20)for(int x=15;x<=35;x+=20)c.drawRoundRect(x,y,x+14,y+14,3,3,p);
        }else if(kind==2){
            c.drawCircle(32,32,14,p);c.drawCircle(32,32,5,p);
            for(int n=0;n<8;n++){c.save();c.rotate(n*45,32,32);c.drawLine(32,11,32,17,p);c.restore();}
        }else if(kind==3){
            c.drawRoundRect(11,15,53,43,4,4,p);c.drawLine(32,43,32,51,p);c.drawLine(23,52,41,52,p);
        }else{
            Path path=new Path();path.moveTo(12,30);path.lineTo(32,13);path.lineTo(52,30);c.drawPath(path,p);
            path=new Path();path.moveTo(18,27);path.lineTo(18,50);path.lineTo(28,50);path.lineTo(28,37);path.lineTo(36,37);path.lineTo(36,50);path.lineTo(46,50);path.lineTo(46,27);c.drawPath(path,p);
        }
        c.restore();
    }
    @Override public void setAlpha(int alpha){p.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
