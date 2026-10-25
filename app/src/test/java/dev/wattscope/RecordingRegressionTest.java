package dev.wattscope;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPowerManager;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28,35})
public class RecordingRegressionTest {
    @Before public void reset() {
        MonitorStore.running=false; MonitorStore.reset(0);
        Prefs.get(RuntimeEnvironment.getApplication()).edit().clear().commit();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("android.permission.POST_NOTIFICATIONS");
    }
    @Test public void recordingHoldsCpuAndStopReleasesIt() {
        ServiceController<PowerMonitorService> c=Robolectric.buildService(PowerMonitorService.class).create();
        PowerMonitorService s=c.get();
        s.onStartCommand(new Intent().setAction(PowerMonitorService.ACTION_START),0,1);
        PowerManager.WakeLock lock=ShadowPowerManager.getLatestWakeLock();
        assertNotNull("screen-off recording needs the accepted partial wake lock",lock);
        assertTrue(lock.isHeld()); assertTrue(MonitorStore.running);
        s.onStartCommand(new Intent().setAction(PowerMonitorService.ACTION_START),0,2);
        assertSame(lock,ShadowPowerManager.getLatestWakeLock());
        s.onStartCommand(new Intent().setAction(PowerMonitorService.ACTION_STOP),0,3);
        assertFalse(lock.isHeld()); assertFalse(MonitorStore.running);c.destroy();
    }
    @Test public void serviceDestructionReleasesCpu() {
        ServiceController<PowerMonitorService> c=Robolectric.buildService(PowerMonitorService.class).create();
        c.get().onStartCommand(new Intent().setAction(PowerMonitorService.ACTION_START),0,1);
        PowerManager.WakeLock lock=ShadowPowerManager.getLatestWakeLock();
        assertNotNull(lock);c.destroy();assertFalse(lock.isHeld());
    }
    @Test public void firstStartExplainsBackgroundRecordingBeforeStartingService() {
        ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup();
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        find(c.get().getWindow().getDecorView(),"开始监测").performClick();
        AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("first start must show explanation",d);assertTrue(d.isShowing());
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        d.cancel();assertNull(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService());
        c.pause().stop().destroy();
    }
    @Test public void recordingRenewsBeyondInitialTimeoutAndReleasesOnlyOnce() {
        Prefs.get(RuntimeEnvironment.getApplication()).edit().putInt("interval",5000).commit();
        ServiceController<PowerMonitorService> c=Robolectric.buildService(PowerMonitorService.class).create();
        PowerMonitorService s=c.get();
        s.onStartCommand(new Intent().setAction(PowerMonitorService.ACTION_START),0,1);
        PowerManager.WakeLock lock=ShadowPowerManager.getLatestWakeLock();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(185));
        assertTrue(MonitorStore.running);assertTrue(lock.isHeld());
        assertTrue(MonitorStore.history.size()>20);
        s.onStartCommand(new Intent().setAction(PowerMonitorService.ACTION_STOP),0,2);
        assertFalse(lock.isHeld());c.destroy();
    }
    static View find(View v,String text) {
        if(v instanceof TextView && ((TextView)v).getText().toString().equals(text))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){
            View child=find(((ViewGroup)v).getChildAt(i),text);if(child!=null)return child;
        }
        return null;
    }
}
