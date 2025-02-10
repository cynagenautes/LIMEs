package io.github.hiro.lime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.Manifest;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.github.hiro.lime.hooks.CustomPreferences;

public class MainActivity extends Activity {
    public LimeOptions limeOptions = new LimeOptions();
    private static final int REQUEST_CODE = 100;

    @Deprecated
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                initializeApp();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE
                );
            } else {
                initializeApp();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeApp();
            } else {
                // パーミッションが拒否された場合の処理
                Toast.makeText(this, "ストレージアクセス権限が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeApp() {
        CustomPreferences customPrefs;
        try {
            customPrefs = new CustomPreferences();
        } catch (PackageManager.NameNotFoundException e) {
            showModuleNotEnabledAlert();
            return;
        }

        for (LimeOptions.Option option : limeOptions.options) {
            option.checked = Boolean.parseBoolean(customPrefs.getSetting(option.name, String.valueOf(option.checked)));
        }

        ScrollView mainScrollView = new ScrollView(this);
        mainScrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(Utils.dpToPx(20, this), Utils.dpToPx(20, this), Utils.dpToPx(20, this), Utils.dpToPx(20, this));

        Switch switchRedirectWebView = null;
        for (LimeOptions.Option option : limeOptions.options) {
            final String name = option.name;

            Switch switchView = new Switch(this);
            switchView.setText(getString(option.id));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = Utils.dpToPx(20, this);
            switchView.setLayoutParams(params);

            switchView.setChecked(option.checked);
            switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                customPrefs.saveSetting(name, String.valueOf(isChecked));
            });

            if (name.equals("redirect_webview")) {
                switchRedirectWebView = switchView;
            } else if (name.equals("open_in_browser")) {
                switchRedirectWebView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    customPrefs.saveSetting("redirect_webview", String.valueOf(isChecked));
                    switchView.setEnabled(isChecked);
                });
                switchView.setEnabled(Boolean.parseBoolean(customPrefs.getSetting("redirect_webview", "false")));
            }

            mainLayout.addView(switchView);
        }

        // Modify Request Section
        {
            LinearLayout layoutModifyRequest = new LinearLayout(this);
            layoutModifyRequest.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            layoutModifyRequest.setOrientation(LinearLayout.VERTICAL);
            layoutModifyRequest.setPadding(Utils.dpToPx(20, this), Utils.dpToPx(20, this), Utils.dpToPx(20, this), Utils.dpToPx(20, this));

            EditText editText = new EditText(this);
            editText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            editText.setTypeface(Typeface.MONOSPACE);
            editText.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                    InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
            editText.setMovementMethod(new ScrollingMovementMethod());
            editText.setTextIsSelectable(true);
            editText.setHorizontallyScrolling(true);
            editText.setVerticalScrollBarEnabled(true);
            editText.setHorizontalScrollBarEnabled(true);
            editText.setText(new String(Base64.decode(customPrefs.getSetting("encoded_js_modify_request", ""), Base64.NO_WRAP)));

            layoutModifyRequest.addView(editText);

            LinearLayout buttonLayout = new LinearLayout(this);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.topMargin = Utils.dpToPx(10, this);
            buttonLayout.setLayoutParams(buttonParams);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

            Button copyButton = new Button(this);
            copyButton.setText(R.string.button_copy);
            copyButton.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", editText.getText().toString());
                clipboard.setPrimaryClip(clip);
            });

            buttonLayout.addView(copyButton);

            Button pasteButton = new Button(this);
            pasteButton.setText(R.string.button_paste);
            pasteButton.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence pasteData = clip.getItemAt(0).getText();
                        editText.setText(pasteData);
                    }
                }
            });

            buttonLayout.addView(pasteButton);
            layoutModifyRequest.addView(buttonLayout);

            ScrollView dialogScrollView = new ScrollView(this);
            dialogScrollView.addView(layoutModifyRequest);

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.modify_request)
                    .setView(dialogScrollView)
                    .setPositiveButton(R.string.positive_button, (dialog, which) -> {
                        customPrefs.saveSetting("encoded_js_modify_request", Base64.encodeToString(editText.getText().toString().getBytes(), Base64.NO_WRAP));
                    })
                    .setNegativeButton(R.string.negative_button, null)
                    .setOnDismissListener(dialog -> {
                        editText.setText(new String(Base64.decode(customPrefs.getSetting("encoded_js_modify_request", ""), Base64.NO_WRAP)));
                    });

            AlertDialog dialog = builder.create();

            Button button = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = Utils.dpToPx(20, this);
            button.setLayoutParams(params);
            button.setText(R.string.modify_request);
            button.setOnClickListener(view -> dialog.show());

            mainLayout.addView(button);
        }

        // Modify Response Section
        {
            LinearLayout layoutModifyResponse = new LinearLayout(this);
            layoutModifyResponse.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            layoutModifyResponse.setOrientation(LinearLayout.VERTICAL);
            layoutModifyResponse.setPadding(Utils.dpToPx(20, this), Utils.dpToPx(20, this), Utils.dpToPx(20, this), Utils.dpToPx(20, this));

            EditText editText = new EditText(this);
            editText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            editText.setTypeface(Typeface.MONOSPACE);
            editText.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                    InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
            editText.setMovementMethod(new ScrollingMovementMethod());
            editText.setTextIsSelectable(true);
            editText.setHorizontallyScrolling(true);
            editText.setVerticalScrollBarEnabled(true);
            editText.setHorizontalScrollBarEnabled(true);
            editText.setText(new String(Base64.decode(customPrefs.getSetting("encoded_js_modify_response", ""), Base64.NO_WRAP)));

            layoutModifyResponse.addView(editText);

            LinearLayout buttonLayout = new LinearLayout(this);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonParams.topMargin = Utils.dpToPx(10, this);
            buttonLayout.setLayoutParams(buttonParams);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

            Button copyButton = new Button(this);
            copyButton.setText(R.string.button_copy);
            copyButton.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", editText.getText().toString());
                clipboard.setPrimaryClip(clip);
            });

            buttonLayout.addView(copyButton);

            Button pasteButton = new Button(this);
            pasteButton.setText(R.string.button_paste);
            pasteButton.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence pasteData = clip.getItemAt(0).getText();
                        editText.setText(pasteData);
                    }
                }
            });

            buttonLayout.addView(pasteButton);
            layoutModifyResponse.addView(buttonLayout);

            ScrollView dialogScrollView = new ScrollView(this);
            dialogScrollView.addView(layoutModifyResponse);

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.modify_response)
                    .setView(dialogScrollView)
                    .setPositiveButton(R.string.positive_button, (dialog, which) -> {
                        customPrefs.saveSetting("encoded_js_modify_response", Base64.encodeToString(editText.getText().toString().getBytes(), Base64.NO_WRAP));
                    })
                    .setNegativeButton(R.string.negative_button, null)
                .setOnDismissListener(dialog -> {
                    editText.setText(new String(Base64.decode(customPrefs.getSetting("encoded_js_modify_response", ""), Base64.NO_WRAP)));
                });

            AlertDialog dialog = builder.create();

            Button button = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = Utils.dpToPx(20, this);
            button.setLayoutParams(params);
            button.setText(R.string.modify_response);
            button.setOnClickListener(view -> dialog.show());

            mainLayout.addView(button);
        }

        mainScrollView.addView(mainLayout);

        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(mainScrollView);
    }

    private void showModuleNotEnabledAlert() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.module_not_enabled_title))
                .setMessage(getString(R.string.module_not_enabled_text))
                .setPositiveButton(getString(R.string.positive_button), (dialog, which) -> finishAndRemoveTask())
                .setCancelable(false)
                .show();
    }
}