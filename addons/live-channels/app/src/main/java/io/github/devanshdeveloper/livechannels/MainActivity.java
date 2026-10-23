package io.github.devanshdeveloper.livechannels;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "live_channels_prefs";

    private final SourceManager sourceManager = new SourceManager();
    private SharedPreferences prefs;

    private LinearLayout llSources;
    private TextView tvEmpty;
    private Button btnOpenLast;
    private Button btnAddSource;
    private CheckBox chkOpenOnLaunch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sourceManager.load(prefs);

        llSources = findViewById(R.id.llSources);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnOpenLast = findViewById(R.id.btnOpenLast);
        btnAddSource = findViewById(R.id.btnAddSource);
        chkOpenOnLaunch = findViewById(R.id.chkOpenOnLaunch);

        btnOpenLast.setOnClickListener(v -> openLastSource());
        btnAddSource.setOnClickListener(v -> showAddSourceDialog());

        chkOpenOnLaunch.setChecked(sourceManager.isOpenOnLaunch());
        chkOpenOnLaunch.setOnCheckedChangeListener((btn, isChecked) -> {
            sourceManager.setOpenOnLaunch(isChecked);
            sourceManager.save(prefs);
        });

        // Populate default passthrough inputs if initial run and list is empty
        if (sourceManager.getSources().isEmpty()) {
            discoverDefaultInputs();
        }

        renderSources();

        // Check open on launch
        if (sourceManager.isOpenOnLaunch() && getIntent() != null && Intent.ACTION_MAIN.equals(getIntent().getAction())) {
            SourceEntry last = sourceManager.getLastOpenedSource();
            if (last != null) {
                launchSource(last);
            }
        }
    }

    private void discoverDefaultInputs() {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim != null) {
            try {
                for (TvInputInfo info : tim.getTvInputList()) {
                    if (info.isPassthroughInput() && !info.isHidden(this)) {
                        CharSequence label = info.loadLabel(this);
                        String name = label != null ? label.toString() : info.getId();
                        sourceManager.addSource(new SourceEntry(
                                UUID.randomUUID().toString(),
                                name,
                                SourceEntry.TYPE_INPUT,
                                info.getId()
                        ));
                    }
                }
                sourceManager.save(prefs);
            } catch (Exception ignored) {
            }
        }
    }

    private void renderSources() {
        llSources.removeAllViews();
        List<SourceEntry> list = sourceManager.getSources();

        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            btnOpenLast.setEnabled(false);
            btnOpenLast.setAlpha(0.5f);
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        btnOpenLast.setEnabled(true);
        btnOpenLast.setAlpha(1.0f);

        SourceEntry last = sourceManager.getLastOpenedSource();
        if (last != null) {
            btnOpenLast.setText(getString(R.string.last_opened_label, last.getName()));
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            final SourceEntry entry = list.get(i);
            View row = inflater.inflate(R.layout.item_source, llSources, false);

            TextView tvName = row.findViewById(R.id.tvSourceName);
            TextView tvType = row.findViewById(R.id.tvSourceType);
            Button btnOpen = row.findViewById(R.id.btnOpen);
            Button btnUp = row.findViewById(R.id.btnMoveUp);
            Button btnDown = row.findViewById(R.id.btnMoveDown);
            Button btnRename = row.findViewById(R.id.btnRename);
            Button btnDelete = row.findViewById(R.id.btnDelete);

            tvName.setText(entry.getName());
            tvType.setText(typeLabel(entry.getType()));

            btnOpen.setOnClickListener(v -> {
                sourceManager.setLastOpenedId(entry.getId());
                sourceManager.save(prefs);
                launchSource(entry);
            });

            btnUp.setEnabled(index > 0);
            btnUp.setAlpha(index > 0 ? 1f : 0.4f);
            btnUp.setOnClickListener(v -> {
                if (sourceManager.moveUp(index)) {
                    sourceManager.save(prefs);
                    renderSources();
                }
            });

            btnDown.setEnabled(index < list.size() - 1);
            btnDown.setAlpha(index < list.size() - 1 ? 1f : 0.4f);
            btnDown.setOnClickListener(v -> {
                if (sourceManager.moveDown(index)) {
                    sourceManager.save(prefs);
                    renderSources();
                }
            });

            btnRename.setOnClickListener(v -> showRenameDialog(entry));
            btnDelete.setOnClickListener(v -> showDeleteConfirmDialog(entry));

            llSources.addView(row);
        }
    }

    private String typeLabel(String type) {
        if (SourceEntry.TYPE_INPUT.equals(type)) return getString(R.string.type_input);
        if (SourceEntry.TYPE_TUNER.equals(type)) return getString(R.string.type_tuner);
        return getString(R.string.type_app);
    }

    private void openLastSource() {
        SourceEntry last = sourceManager.getLastOpenedSource();
        if (last != null) {
            launchSource(last);
        } else {
            Toast.makeText(this, R.string.no_sources, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchSource(SourceEntry entry) {
        try {
            if (SourceEntry.TYPE_INPUT.equals(entry.getType())) {
                Uri uri = TvContract.buildChannelUriForPassthroughInput(entry.getTarget());
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "vnd.android.cursor.item/channel")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else if (SourceEntry.TYPE_TUNER.equals(entry.getType())) {
                Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(entry.getTarget());
                if (intent == null) {
                    intent = getPackageManager().getLaunchIntentForPackage(entry.getTarget());
                }
                if (intent == null) {
                    intent = new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(TvContract.buildChannelUriForPassthroughInput(entry.getTarget()),
                                    "vnd.android.cursor.item/channel");
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(entry.getTarget());
                if (intent == null) {
                    intent = getPackageManager().getLaunchIntentForPackage(entry.getTarget());
                }
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getString(R.string.cant_open_source, entry.getName()), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, getString(R.string.cant_open_source, entry.getName() + " (" + e.getMessage() + ")"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.cant_open_source, entry.getName()), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddSourceDialog() {
        String[] types = new String[]{
                getString(R.string.type_input),
                getString(R.string.type_tuner),
                getString(R.string.type_app)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_add_title)
                .setItems(types, (dialog, which) -> {
                    if (which == 0) pickTvInput();
                    else if (which == 1) pickTuner();
                    else pickInstalledApp();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void pickTvInput() {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) {
            Toast.makeText(this, R.string.no_inputs_found, Toast.LENGTH_SHORT).show();
            return;
        }

        List<TvInputInfo> inputs = new ArrayList<>();
        List<String> names = new ArrayList<>();
        try {
            for (TvInputInfo info : tim.getTvInputList()) {
                if (info.isPassthroughInput() && !info.isHidden(this)) {
                    inputs.add(info);
                    CharSequence label = info.loadLabel(this);
                    names.add(label != null ? label.toString() : info.getId());
                }
            }
        } catch (Exception ignored) {
        }

        if (inputs.isEmpty()) {
            Toast.makeText(this, R.string.no_inputs_found, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_select_input)
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    TvInputInfo chosen = inputs.get(which);
                    sourceManager.addSource(new SourceEntry(
                            UUID.randomUUID().toString(),
                            names.get(which),
                            SourceEntry.TYPE_INPUT,
                            chosen.getId()
                    ));
                    sourceManager.save(prefs);
                    renderSources();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void pickTuner() {
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        List<String> tunerTargets = new ArrayList<>();
        List<String> tunerNames = new ArrayList<>();

        if (tim != null) {
            try {
                for (TvInputInfo info : tim.getTvInputList()) {
                    if (info.getType() == TvInputInfo.TYPE_TUNER) {
                        CharSequence label = info.loadLabel(this);
                        tunerNames.add(label != null ? label.toString() : "TV Tuner (" + info.getId() + ")");
                        tunerTargets.add(info.getId());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Also check if any package handles TV tuner / channels intent
        PackageManager pm = getPackageManager();
        Intent tunerIntent = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelsUriForInput(""));
        for (ResolveInfo ri : pm.queryIntentActivities(tunerIntent, 0)) {
            if (ri.activityInfo != null) {
                String pkg = ri.activityInfo.packageName;
                if (!tunerTargets.contains(pkg)) {
                    tunerNames.add(String.valueOf(ri.loadLabel(pm)));
                    tunerTargets.add(pkg);
                }
            }
        }

        if (tunerTargets.isEmpty()) {
            // Add generic tuner entry
            tunerNames.add("Default TV Tuner");
            tunerTargets.add("android.intent.action.VIEW");
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.type_tuner)
                .setItems(tunerNames.toArray(new String[0]), (dialog, which) -> {
                    sourceManager.addSource(new SourceEntry(
                            UUID.randomUUID().toString(),
                            tunerNames.get(which),
                            SourceEntry.TYPE_TUNER,
                            tunerTargets.get(which)
                    ));
                    sourceManager.save(prefs);
                    renderSources();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void pickInstalledApp() {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
        if (activities.isEmpty()) {
            activities = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        }

        List<String> names = new ArrayList<>();
        List<String> packages = new ArrayList<>();
        for (ResolveInfo ri : activities) {
            if (ri.activityInfo != null && !ri.activityInfo.packageName.equals(getPackageName())) {
                names.add(String.valueOf(ri.loadLabel(pm)));
                packages.add(ri.activityInfo.packageName);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_select_app)
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    sourceManager.addSource(new SourceEntry(
                            UUID.randomUUID().toString(),
                            names.get(which),
                            SourceEntry.TYPE_APP,
                            packages.get(which)
                    ));
                    sourceManager.save(prefs);
                    renderSources();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showRenameDialog(SourceEntry entry) {
        EditText input = new EditText(this);
        input.setText(entry.getName());
        input.setSelection(entry.getName().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_rename_title)
                .setView(input)
                .setPositiveButton(R.string.dialog_save, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        sourceManager.renameSource(entry.getId(), newName);
                        sourceManager.save(prefs);
                        renderSources();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showDeleteConfirmDialog(SourceEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_confirm)
                .setMessage(entry.getName())
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    sourceManager.removeSource(entry.getId());
                    sourceManager.save(prefs);
                    renderSources();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
