package dev.wattscope;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={28,35})
public class BackgroundFlowTest {
    private ActivityController<MainActivity> c;
    @Before public void setUp() {
        MonitorStore.running=false;MonitorStore.reset(0);
        Prefs.get(RuntimeEnvironment.getApplication()).edit().clear().commit();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("android.permission.POST_NOTIFICATIONS");
        shadowOf(RuntimeEnvironment.getApplication().getSystemService(PowerManager.class))
                .setIgnoringBatteryOptimizations(RuntimeEnvironment.getApplication().getPackageName(),false);
        c=Robolectric.buildActivity(MainActivity.class).setup();
    }
    @After public void tearDown(){if(c!=null)c.pause().stop().destroy();}
    private void click(String text){View v=RecordingRegressionTest.find(c.get().getWindow().getDecorView(),text);assertNotNull(text,v);v.performClick();idle();}
    private void idle(){shadowOf(android.os.Looper.getMainLooper()).idle();}
    private AlertDialog dialog(){AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d);assertTrue(d.isShowing());return d;}
    @Test public void manualGuideOffersBrandSelection(){
        click("息屏记录设置");
        assertNotNull("users must be able to correct the suggested phone brand",
                RecordingRegressionTest.find(dialog().getWindow().getDecorView(),"选择手机品牌"));
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    @Test public void androidExemptionDoesNotSkipFirstVendorGuidance(){
        shadowOf(c.get().getSystemService(PowerManager.class)).setIgnoringBatteryOptimizations(c.get().getPackageName(),true);
        click("开始监测");
        assertTrue(dialog().isShowing());
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    @Test public void positiveStartsExactlyOnceAndRequestsRealPackage(){
        click("开始监测");dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();
        Intent service=shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService();
        assertNotNull(service);assertEquals(PowerMonitorService.ACTION_START,service.getAction());
        Intent settings=shadowOf(c.get()).getNextStartedActivity();
        assertNotNull(settings);assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,settings.getAction());
        assertEquals("package:"+c.get().getPackageName(),settings.getDataString());
        c.pause().resume();
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        assertNull(shadowOf(c.get()).getNextStartedActivity());
    }
    @Test public void skipStartsWithoutGrantAndDoesNotRepeatIntro(){
        click("开始监测");dialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();idle();
        assertNotNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        assertNull(shadowOf(c.get()).getNextStartedActivity());
        c.recreate();click("开始监测");
        assertNotNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        assertFalse(ShadowAlertDialog.getLatestAlertDialog().isShowing());
    }
    @Test public void unansweredIntroSurvivesRotationAndBackAllowsRetry(){
        click("开始监测");AlertDialog old=dialog();c.recreate();
        assertFalse(old.isShowing());dialog().cancel();idle();
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        click("开始监测");assertTrue(dialog().isShowing());
    }
    @Test @Config(sdk=35) public void deniedNotificationsAfterRecreationStillContinuesOnce(){
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions("android.permission.POST_NOTIFICATIONS");
        click("开始监测");dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        // Consume the platform permission activity before testing our own settings launch.
        shadowOf(c.get()).getNextStartedActivity();c.recreate();
        c.get().onRequestPermissionsResult(10,new String[]{"android.permission.POST_NOTIFICATIONS"},new int[]{PackageManager.PERMISSION_DENIED});
        assertNotNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,shadowOf(c.get()).getNextStartedActivity().getAction());
        c.get().onRequestPermissionsResult(10,new String[]{"android.permission.POST_NOTIFICATIONS"},new int[]{PackageManager.PERMISSION_DENIED});
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }

    @Test public void settingsReturnRefreshesAndroidButNeverClaimsOemGrant(){
        click("息屏记录设置");AlertDialog guide=dialog();
        assertTrue(((TextView)guide.findViewById(R.id.guide_system_status)).getText().toString().contains("未豁免"));
        guide.findViewById(R.id.guide_app_settings).performClick();idle();
        Intent out=shadowOf(c.get()).getNextStartedActivity();
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,out.getAction());
        assertEquals("package:"+c.get().getPackageName(),out.getDataString());
        c.pause().resume();
        assertTrue(((TextView)guide.findViewById(R.id.guide_system_status)).getText().toString().contains("未豁免"));
        shadowOf(c.get().getSystemService(PowerManager.class)).setIgnoringBatteryOptimizations(c.get().getPackageName(),true);
        c.pause().resume();
        assertTrue(((TextView)guide.findViewById(R.id.guide_system_status)).getText().toString().contains("已豁免"));
        assertTrue(((TextView)guide.findViewById(R.id.guide_vendor_status)).getText().toString().contains("需手动核对"));
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    @Test public void manualBrandChoiceSurvivesRotationAndAutomaticCanBeRestored(){
        click("息屏记录设置");AlertDialog guide=dialog();
        guide.findViewById(R.id.guide_brand_select).performClick();idle();
        AlertDialog choice=dialog();selectChoice(choice,"vivo / iQOO");
        assertTrue(((TextView)guide.findViewById(R.id.guide_vendor_steps)).getText().toString().contains("允许后台高耗电"));
        c.recreate();guide=dialog();
        assertTrue(((TextView)guide.findViewById(R.id.guide_brand_label)).getText().toString().contains("手动选择"));
        assertTrue(((TextView)guide.findViewById(R.id.guide_brand_label)).getText().toString().contains("vivo / iQOO"));
        guide.findViewById(R.id.guide_brand_select).performClick();idle();
        choice=dialog();choice.getListView().performItemClick(null,0,0);idle();
        assertTrue(((TextView)guide.findViewById(R.id.guide_brand_label)).getText().toString().contains("按品牌推荐"));
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    @Test public void unknownBrandCanUseGenericInstructionsWithoutGrantAssumptions(){
        click("息屏记录设置");AlertDialog guide=dialog();
        guide.findViewById(R.id.guide_brand_select).performClick();idle();
        selectChoice(dialog(),"其他 Android / 通用");
        assertTrue(((TextView)guide.findViewById(R.id.guide_vendor_steps)).getText().toString().contains("若系统没有额外开关"));
        assertTrue(((TextView)guide.findViewById(R.id.guide_vendor_status)).getText().toString().contains("需手动核对"));
    }
    @Test public void alreadyExemptFirstStartDoesNotLaunchAnotherSystemRequest(){
        shadowOf(c.get().getSystemService(PowerManager.class)).setIgnoringBatteryOptimizations(c.get().getPackageName(),true);
        click("开始监测");AlertDialog guide=dialog();
        assertEquals("开始记录",guide.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        guide.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();
        assertNotNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        assertNull(shadowOf(c.get()).getNextStartedActivity());
    }
    @Test public void openingSettingsDoesNotMarkFirstStartGuideAsSeen(){
        click("息屏记录设置");dialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();
        click("开始监测");assertTrue(dialog().isShowing());
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
    }
    private void selectChoice(AlertDialog choice,String text){
        android.widget.ListView list=choice.getListView();
        for(int i=0;i<list.getAdapter().getCount();i++){
            if(text.equals(list.getAdapter().getItem(i).toString())){
                list.performItemClick(null,i,i);idle();return;
            }
        }
        fail("missing phone choice: "+text);
    }
}
