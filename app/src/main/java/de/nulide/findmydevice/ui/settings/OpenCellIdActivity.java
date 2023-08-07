package de.nulide.findmydevice.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;


public class OpenCellIdActivity extends AppCompatActivity implements TextWatcher {

    private Settings Settings;

    private EditText editTextOpenCellIdKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cell_id);

        Settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));

        editTextOpenCellIdKey = findViewById(R.id.editTextOpenCellIDAPIKey);
        editTextOpenCellIdKey.setText((String) Settings.get(Settings.SET_OPENCELLID_API_KEY));
        editTextOpenCellIdKey.addTextChangedListener(this);

        Button buttonPaste = findViewById(R.id.buttonPaste);
        buttonPaste.setOnClickListener(this::onPasteClicked);

        Button buttonOpenOpenCellIdWebsite = findViewById(R.id.buttonOpenOpenCellIdWebsite);
        buttonOpenOpenCellIdWebsite.setOnClickListener(this::onOpenWebsiteClicked);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable edited) {
        if (edited == editTextOpenCellIdKey.getText()) {
            Settings.set(Settings.SET_OPENCELLID_API_KEY, edited.toString());
        }
    }

    private void onPasteClicked(View view) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
        CharSequence pasteData = item.getText();
        if (pasteData != null) {
            editTextOpenCellIdKey.setText(pasteData);
        }
    }

    private void onOpenWebsiteClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://opencellid.org/"));
        startActivity(intent);
    }
}