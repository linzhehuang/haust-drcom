package io.linzhehuang;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import io.linzhehuang.drcomutil.DRCOMUtil;
import io.linzhehuang.drcomutil.DRCOMUtil.DRCOMState;
import io.linzhehuang.drcomutil.HostConf;

public class MainActivity extends Activity {
    private final static String INFO_FILE_NAME = "info";
    private final static String INFO_ACCOUNT = "account";
    private final static String INFO_PASSWORD = "password";

    private boolean logged = false;

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
        DRCOMUtil.init();


        final String account = accountEdit.getText().toString();
        final String password = passwordEdit.getText().toString();
        final String macAddress = wifiUtil.getMacAddress();
        final HostConf conf = new HostConf(account, password, macAddress);

        new Thread(new Runnable() {
            public void run() {
                DRCOMState state = DRCOMUtil.login(conf);
                if (state == DRCOMState.LOGGED) {
                    logged = true;
                    setLoggedStatus();
                }
            }
        }).start();
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
                final String account = accountEdit.getText().toString();
                final String password = passwordEdit.getText().toString();
                final String macAddress = wifiUtil.getMacAddress();
                final HostConf conf = new HostConf(account, password, macAddress);

                if (logged == false) {
                    tip(R.string.login_info);
                    new Thread(new Runnable() {
                        public void run() {
                            DRCOMState state = DRCOMUtil.login(conf);
                            if (state == DRCOMState.LOGGED) {
                                logged = true;
                                saveInfo(account, password);
                                innerTip(R.string.login_success);
                                setLoggedStatus();
                            } else if (state == DRCOMState.AUTH_ERROR) {
                                innerTip(R.string.info_error);
                            } else {
                                innerTip(R.string.unknown_error);
                            }
                        }
                    }).start();
                } else {
                    tip(R.string.logout_info);
                    new Thread(new Runnable() {
                        public void run() {
                            if (DRCOMUtil.logout(conf)) {
                                logged = false;
                                innerTip(R.string.logout_success);
                                setUnloggedStatus();
                            } else {
                                innerTip(R.string.logout_failed);
                            }
                        }
                    }).start();
                }

            }
        });
    }

    private void setLoggedStatus() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable(){
            public void run(){
                accountEdit.setEnabled(false);
                passwordEdit.setEnabled(false);
                loginButton.setText(R.string.logged_text);
            }
        });
    }

    private void setUnloggedStatus() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable(){
            public void run(){
                accountEdit.setEnabled(true);
                passwordEdit.setEnabled(true);
                loginButton.setText(R.string.login_text);
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
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    private void tip(int rid) {
        tip(getString(rid));
    }

    private void innerTip(String msg) {
        Handler handler = new Handler(Looper.getMainLooper());
        final String m = msg;
        handler.post(new Runnable(){
            public void run(){
                tip(m);
            }
        });
    }

    private void innerTip(int rid) {
        innerTip(getString(rid));
    }
}
