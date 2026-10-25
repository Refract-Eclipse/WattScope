package dev.wattscope;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class BackgroundSettingsTest {
    // Fake only the external launch boundary: the real gateway chooses every intent and fallback.
    public static class SettingsHost extends Activity {
        int deniedAttempts; boolean security;
        final List<Intent> attempts=new ArrayList<>();
        @Override public void startActivity(Intent intent){
            attempts.add(intent);
            if(attempts.size()<=deniedAttempts){
                if(security)throw new SecurityException("device policy");
                throw new ActivityNotFoundException("missing settings activity");
            }
        }
    }
    private ActivityController<SettingsHost> c;
    private SettingsHost host;
    @Before public void setUp(){
        c=Robolectric.buildActivity(SettingsHost.class).setup();host=c.get();
        shadowOf(host.getSystemService(PowerManager.class)).setIgnoringBatteryOptimizations(host.getPackageName(),false);
    }
    @After public void clean(){c.pause().stop().destroy();}
    @Test public void directRequestTargetsActualInstalledApplication(){
        BackgroundSettings.requestExemption(host);
        assertEquals(1,host.attempts.size());
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,host.attempts.get(0).getAction());
        assertEquals("package:dev.wattscope.guidetrial",host.attempts.get(0).getDataString());
        assertEquals(Boolean.FALSE,BackgroundSettings.exemption(host));
    }
    @Test public void grantedExemptionIsNotRequestedAgain(){
        shadowOf(host.getSystemService(PowerManager.class)).setIgnoringBatteryOptimizations(host.getPackageName(),true);
        BackgroundSettings.requestExemption(host);assertTrue(host.attempts.isEmpty());
    }
    @Test public void missingRequestFallsBackToOptimizationList(){
        host.deniedAttempts=1;BackgroundSettings.requestExemption(host);
        assertEquals(2,host.attempts.size());assertEquals(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,host.attempts.get(1).getAction());
    }
    @Test public void blockedOptimizationPagesFallBackToAppDetails(){
        host.deniedAttempts=2;host.security=true;BackgroundSettings.requestExemption(host);
        assertEquals(3,host.attempts.size());assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,host.attempts.get(2).getAction());
        assertEquals("package:dev.wattscope.guidetrial",host.attempts.get(2).getDataString());
    }
    @Test public void blockedAppDetailsFallBackToGeneralSettings(){
        host.deniedAttempts=3;BackgroundSettings.requestExemption(host);
        assertEquals(4,host.attempts.size());assertEquals(Settings.ACTION_SETTINGS,host.attempts.get(3).getAction());
    }
    @Test public void allSettingsUnavailableLeaveManualInstructionsAndNoFalseGrant(){
        host.deniedAttempts=4;host.security=true;BackgroundSettings.requestExemption(host);
        assertEquals(4,host.attempts.size());assertNotNull(ShadowToast.getTextOfLatestToast());
        assertEquals(Boolean.FALSE,BackgroundSettings.exemption(host));
    }
    @Test public void openingAppDetailsDoesNotRequestAnExemption(){
        BackgroundSettings.openApp(host);assertEquals(1,host.attempts.size());
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,host.attempts.get(0).getAction());
    }
}
