package dev.wattscope;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** One guide shared by onboarding and settings. All long text scrolls at system font size. */
final class BackgroundGuideDialog {
    private static final String BRAND_PREF = "background_guide_brand";
    private final Activity activity;
    private final boolean firstStart;
    private final AlertDialog dialog;
    private AlertDialog chooser;
    private final TextView systemStatus, brandLabel, vendorSteps;
    private final Button request;

    BackgroundGuideDialog(Activity activity, boolean firstStart, Runnable accept, Runnable skip, Runnable close) {
        this.activity = activity;
        this.firstStart = firstStart;
        LinearLayout body = Ui.column(activity);
        int padding = Ui.dp(activity, 24);
        body.setPadding(padding, Ui.dp(activity, 8), padding, Ui.dp(activity, 12));
        paragraph(body, R.string.bg_purpose, false);
        paragraph(body, R.string.bg_system_title, true);
        systemStatus = paragraph(body, R.string.bg_unknown, false);
        systemStatus.setId(R.id.guide_system_status);
        request = action(body, R.string.bg_request, R.id.guide_request, () -> BackgroundSettings.requestExemption(activity));
        paragraph(body, R.string.bg_vendor_title, true);
        TextView manual = paragraph(body, R.string.bg_manual_status, false);
        manual.setId(R.id.guide_vendor_status);
        manual.setTextColor(Ui.ORANGE);
        brandLabel = paragraph(body, R.string.bg_other, false);
        brandLabel.setId(R.id.guide_brand_label);
        action(body, R.string.bg_choose_brand, R.id.guide_brand_select, this::chooseBrand);
        paragraph(body, R.string.bg_version_note, false);
        TextView app = paragraph(body, R.string.bg_app, true);
        app.setText(activity.getString(R.string.bg_app, activity.getApplicationInfo().loadLabel(activity.getPackageManager())));
        vendorSteps = paragraph(body, R.string.bg_other_steps, false);
        vendorSteps.setId(R.id.guide_vendor_steps);
        action(body, R.string.bg_app_settings, R.id.guide_app_settings, () -> BackgroundSettings.openApp(activity));
        action(body, R.string.bg_phone_settings, R.id.guide_phone_settings, () -> BackgroundSettings.openPhone(activity));
        paragraph(body, R.string.bg_test_title, true);
        paragraph(body, R.string.bg_test, false);
        if (firstStart) paragraph(body, R.string.bg_first_action_note, false);
        ScrollView scroll = new ScrollView(activity);
        scroll.setId(R.id.guide_scroll);
        scroll.addView(body);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(firstStart ? R.string.bg_first_title : R.string.bg_title).setView(scroll);
        if (firstStart) {
            builder.setPositiveButton(R.string.bg_accept, (d, w) -> accept.run());
            builder.setNegativeButton(R.string.bg_skip, (d, w) -> {
                if (!Boolean.TRUE.equals(BackgroundSettings.exemption(activity))) skip.run();
            });
        } else builder.setPositiveButton(R.string.bg_close, null);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(d -> close.run());
    }

    void show() { dialog.show(); refresh(); }

    void refresh() {
        Boolean exempt = BackgroundSettings.exemption(activity);
        systemStatus.setText(exempt == null ? R.string.bg_unknown : exempt ? R.string.bg_exempt : R.string.bg_not_exempt);
        // Onboarding applies through the bottom action; settings offers an explicit request.
        request.setVisibility(firstStart ? View.GONE : View.VISIBLE);
        if (!firstStart) {
            request.setText(Boolean.TRUE.equals(exempt) ? R.string.bg_system_list : R.string.bg_request);
            request.setOnClickListener(v -> {
                if (Boolean.TRUE.equals(BackgroundSettings.exemption(activity))) BackgroundSettings.openOptimizationList(activity);
                else BackgroundSettings.requestExemption(activity);
            });
        }
        if (firstStart && dialog.isShowing()) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(Boolean.TRUE.equals(exempt) ? R.string.bg_start : R.string.bg_accept);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(Boolean.TRUE.equals(exempt) ? R.string.bg_cancel : R.string.bg_skip);
        }
        BackgroundGuidance manual = BackgroundGuidance.override(Prefs.get(activity).getString(BRAND_PREF, ""));
        BackgroundGuidance guide = manual == null ? BackgroundGuidance.detect(Build.MANUFACTURER, Build.BRAND) : manual;
        brandLabel.setText(activity.getString(manual == null ? R.string.bg_detected : R.string.bg_selected, activity.getString(guide.label)));
        vendorSteps.setText(guide.steps);
    }

    private void chooseBrand() {
        BackgroundGuidance[] guides = BackgroundGuidance.values();
        String[] labels = new String[guides.length + 1];
        labels[0] = activity.getString(R.string.bg_auto, activity.getString(BackgroundGuidance.detect(Build.MANUFACTURER, Build.BRAND).label));
        BackgroundGuidance selected = BackgroundGuidance.override(Prefs.get(activity).getString(BRAND_PREF, ""));
        int checked = 0;
        for (int i = 0; i < guides.length; i++) {
            labels[i + 1] = activity.getString(guides[i].label);
            if (selected == guides[i]) checked = i + 1;
        }
        chooser = new AlertDialog.Builder(activity).setTitle(R.string.bg_choose_brand)
                .setSingleChoiceItems(labels, checked, (d, position) -> {
                    Prefs.get(activity).edit().putString(BRAND_PREF, position == 0 ? "" : guides[position - 1].name()).apply();
                    refresh();
                    d.dismiss();
                }).setNegativeButton(R.string.bg_cancel, null).show();
    }

    void dismiss() {
        if (chooser != null) chooser.dismiss();
        dialog.dismiss();
    }

    private TextView paragraph(LinearLayout body, int text, boolean title) {
        TextView view = Ui.text(activity, activity.getString(text), title ? 15 : 14, title ? Ui.TEXT : Ui.MUTED, title);
        view.setLineSpacing(Ui.dp(activity, 3), 1);
        body.addView(view, new LinearLayout.LayoutParams(-1, -2));
        Ui.space(body, title ? 8 : 16);
        return view;
    }

    private Button action(LinearLayout body, int text, int id, Runnable onClick) {
        Button button = new Button(activity);
        button.setText(text);
        button.setId(id);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinHeight(Ui.dp(activity, 48));
        button.setTextColor(Ui.TEXT);
        button.setOnClickListener(v -> onClick.run());
        body.addView(button, new LinearLayout.LayoutParams(-1, -2));
        Ui.space(body, 8);
        return button;
    }
}
