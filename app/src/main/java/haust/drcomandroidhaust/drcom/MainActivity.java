package haust.drcomandroidhaust.drcom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import haust.drcomandroidhaust.R;
import haust.drcomandroidhaust.drcom.drcom.DrcomService;
import haust.drcomandroidhaust.drcom.drcom.HostInfo;
import haust.drcomandroidhaust.drcom.util.WifiUtil;

public class MainActivity extends Activity {

    private final static String INFO_FILE_NAME = "info";
    private final static String INFO_ACCOUNT = "account";
    private final static String INFO_PASSWORD = "password";

    private EditText accountEdit, passwordEdit;
    private Button loginButton;

    private WifiUtil wifiUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accountEdit = (EditText)findViewById(R.id.accountEdit);
        passwordEdit = (EditText)findViewById(R.id.passwordEdit);
        loginButton = (Button)findViewById(R.id.loginButton);
        wifiUtil = new WifiUtil(getApplicationContext());

        loadInfo();
        checkoutEditTexts();
        bindLoginButtonEvent();
        bindEditTextEvent();
    }

    private void checkoutEditTexts() {
        loginButton.setEnabled(!(accountEdit.getText().toString().equals("") ||
                passwordEdit.getText().toString().equals("")));
    }

    private void loadInfo() {
        SharedPreferences sharedPreferences = getSharedPreferences(INFO_FILE_NAME, Context.MODE_PRIVATE);
        accountEdit.setText(sharedPreferences.getString(INFO_ACCOUNT, ""));
        passwordEdit.setText(sharedPreferences.getString(INFO_PASSWORD, ""));
    }

    private void saveInfo(String account, String password) {
        SharedPreferences.Editor editor = getSharedPreferences(INFO_FILE_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(INFO_ACCOUNT, account);
        editor.putString(INFO_PASSWORD, password);
        editor.apply();
    }

    private void bindLoginButtonEvent() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String account = accountEdit.getText().toString();
                String password = passwordEdit.getText().toString();
                String macAddress = wifiUtil.getMacAddress();
                HostInfo hostInfo = new HostInfo(account, password, macAddress);

                tip("登录中");

                Intent service = new Intent(MainActivity.this, DrcomService.class);
                service.putExtra("info", hostInfo);
                startService(service);

                saveInfo(account, password);
            }
        });
    }

    private void bindEditTextEvent() {
        accountEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                checkoutEditTexts();
            }
        });
        passwordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                checkoutEditTexts();
            }
        });
    }

    private void tip(String m) {
        final String msg = new StringBuilder(getString(R.string.app_name)).append("：").append(m).toString();
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
}

