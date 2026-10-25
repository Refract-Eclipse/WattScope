package dev.wattscope;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w320dp-h480dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GuideLayoutTest {
    @Test public void smallScreenLargeTextKeepsActionsAndScrollableInstructions() throws Exception {
        Configuration configuration=new Configuration(RuntimeEnvironment.getApplication().getResources().getConfiguration());
        configuration.fontScale=1.5f;
        RuntimeEnvironment.getApplication().getResources().updateConfiguration(configuration,null);
        Prefs.get(RuntimeEnvironment.getApplication()).edit().clear().putString("background_guide_brand","VIVO").commit();
        MonitorStore.running=false;MonitorStore.reset(0);
        ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup();
        try{
            RecordingRegressionTest.find(c.get().getWindow().getDecorView(),"开始监测").performClick();
            shadowOf(Looper.getMainLooper()).idle();
            AlertDialog guide=ShadowAlertDialog.getLatestAlertDialog();
            View decor=guide.getWindow().getDecorView();
            decor.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));
            decor.layout(0,0,320,480);
            ScrollView scroll=guide.findViewById(R.id.guide_scroll);
            assertTrue("instructions must scroll instead of hiding actions",scroll.canScrollVertically(1));
            assertTrue(guide.getButton(AlertDialog.BUTTON_POSITIVE).isShown());
            assertTrue(guide.getButton(AlertDialog.BUTTON_NEGATIVE).isShown());
            android.graphics.Rect bounds=new android.graphics.Rect();
            assertTrue(guide.getButton(AlertDialog.BUTTON_POSITIVE).getGlobalVisibleRect(bounds));
            assertTrue(bounds.bottom<=480);assertTrue(bounds.height()>0);
            File folder=new File("build/guide-preview");folder.mkdirs();
            write(decor,new File(folder,"first-start-small-large-font.png"));
            scroll.scrollTo(0,scroll.getChildAt(0).getHeight());
            write(decor,new File(folder,"first-start-small-bottom.png"));
            guide.dismiss();
        }finally{c.pause().stop().destroy();}
    }
    private static void write(View view,File path) throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        try(FileOutputStream out=new FileOutputStream(path)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();
    }
}
