package dev.wattscope;

import org.junit.Test;
import java.util.Locale;
import static org.junit.Assert.*;

public class BackgroundGuidanceTest {
    @Test public void identifiesSubBrandsWithoutSubstringGuessing() {
        assertEquals(BackgroundGuidance.VIVO,BackgroundGuidance.detect("vivo","iQOO"));
        assertEquals(BackgroundGuidance.XIAOMI,BackgroundGuidance.detect("Xiaomi","Redmi"));
        assertEquals(BackgroundGuidance.XIAOMI,BackgroundGuidance.detect("Xiaomi","POCO"));
        assertEquals(BackgroundGuidance.OPPO,BackgroundGuidance.detect("OPPO","OPPO"));
        assertEquals(BackgroundGuidance.ONEPLUS,BackgroundGuidance.detect("OPPO","OnePlus"));
        assertEquals(BackgroundGuidance.REALME,BackgroundGuidance.detect("OPPO","realme"));
        assertEquals(BackgroundGuidance.HONOR,BackgroundGuidance.detect("HUAWEI","HONOR"));
        assertEquals(BackgroundGuidance.HUAWEI,BackgroundGuidance.detect("HUAWEI","HUAWEI"));
        assertEquals(BackgroundGuidance.SAMSUNG,BackgroundGuidance.detect("samsung","samsung"));
        assertEquals(BackgroundGuidance.PIXEL,BackgroundGuidance.detect("HTC","google"));
        assertEquals(BackgroundGuidance.OTHER,BackgroundGuidance.detect("unknown","not-vivo"));
    }
    @Test public void manufacturerIsFallbackWhenBrandIsUnknown(){
        assertEquals(BackgroundGuidance.VIVO,BackgroundGuidance.detect("vivo","generic"));
        assertEquals(BackgroundGuidance.OTHER,BackgroundGuidance.detect(null,null));
        assertEquals(BackgroundGuidance.OTHER,BackgroundGuidance.detect("",""));
    }
    @Test public void normalizationIsIndependentOfDeviceLocale(){
        Locale original=Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr","TR"));
            assertEquals(BackgroundGuidance.VIVO,BackgroundGuidance.detect("unknown","  VIVO  "));
            assertEquals(BackgroundGuidance.XIAOMI,BackgroundGuidance.detect("XIAOMI",null));
        }finally{Locale.setDefault(original);}
    }
    @Test public void invalidSavedChoiceFallsBackToAutomaticRatherThanCrashing(){
        assertNull(BackgroundGuidance.override("REMOVED_BRAND"));
        assertNull(BackgroundGuidance.override(null));
        assertNull(BackgroundGuidance.override(""));
        assertEquals(BackgroundGuidance.OTHER,BackgroundGuidance.override("OTHER"));
    }
}
