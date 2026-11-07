package local.minimal.sony;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.widget.Button;

/** Center icon and label as one compact group; preserve native Button focus and accessibility. */
public final class ShortcutButton extends Button {
    private final Drawable icon;
    private final int iconSize;
    private final int gap;
    public ShortcutButton(Context c,Drawable icon){
        super(c);this.icon=icon;icon.setCallback(this);
        float density=getResources().getDisplayMetrics().density;
        iconSize=Math.round(52*density);gap=Math.round(8*density);
        setTextSize(22);
    }
    public void setIconFocused(boolean value){
        if(icon instanceof SystemIconDrawable)((SystemIconDrawable)icon).setFocused(value);
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas){
        Paint text=getPaint();text.setColor(getCurrentTextColor());
        Paint.FontMetrics fm=text.getFontMetrics();
        float total=iconSize+gap+fm.descent-fm.ascent;
        float top=(getHeight()-total)/2f;
        int left=(getWidth()-iconSize)/2;
        icon.setBounds(left,Math.round(top),left+iconSize,Math.round(top)+iconSize);icon.draw(canvas);
        Paint.Align old=text.getTextAlign();text.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(getText().toString(),getWidth()/2f,top+iconSize+gap-fm.ascent,text);
        text.setTextAlign(old);
    }
    @Override protected boolean verifyDrawable(Drawable who){return who==icon||super.verifyDrawable(who);}
}
