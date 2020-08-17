package com.officework.intune;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.PromptBehavior;
import com.officework.intune.auth.AuthListener;
import com.officework.intune.auth.AuthManager;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignInActivity extends AppCompatActivity implements AuthListener {
    private Handler mHandler;
    private AuthenticationContext mAuthContext;
    private SweetAlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //region get sha code
        try {
            Log.v("SHA 1", SHA1("A0:50:88:29:FB:76:E3:DD:F4:6E:1F:9B:EA:CF:85:59:9C:63:F8:EC"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //endregion

        /* If the app has already started, the user has signed in, and the activity was just restarted,
         * skip the rest of this initialization and open the main UI */
        if (savedInstanceState != null && AuthManager.shouldRestoreSignIn(savedInstanceState)) {
            onSignedIn();
            return;
        }

        // region Start by making a sign in window to show instead of the main view
        openSignInView();
        //endregion

        //region // Will make sign in attempts that are allowed to access/modify the UI (prompt)
        mAuthContext = new AuthenticationContext(this, AuthManager.AUTHORITY, true);
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(final Message msg) {
                if (msg.what == AuthManager.MSG_PROMPT_AUTO) {
                    AuthManager.signInWithPrompt(mAuthContext, SignInActivity.this,
                            SignInActivity.this, PromptBehavior.Auto, mHandler);
                } else if (msg.what == AuthManager.MSG_PROMPT_ALWAYS) {
                    AuthManager.signInWithPrompt(mAuthContext, SignInActivity.this,
                            SignInActivity.this, PromptBehavior.Always, mHandler);
                }
            }
        };
        //endregion

        /* We only need to change/set the view and sign in if this is the first time the app
         * has opened, which is when savedInstanceState is null */
        if (savedInstanceState == null) {
            AuthManager.signInSilent(mAuthContext, this, mHandler);
        }

        //region with our vms like login controller code
//        alertDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
//        alertDialog.setCancelable(false);
//        alertDialog.setCanceledOnTouchOutside(false);
//        alertDialog.getProgressHelper().setBarColor(Color.parseColor("#137EF0"));
//        alertDialog.setTitleText("Login ongoing");
//
//        MsLoginController msLoginController = new MsLoginController(SignInActivity.this);
//
//
//        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                alertDialog.show();
//                msLoginController.login(new MsLoginController.onLogin() {
//                    @Override
//                    public void onResult(boolean success, String message, String email) {
//                        alertDialog.dismiss();
//                        if(success){
//                            Toast.makeText(getApplicationContext(),"Login Done",Toast.LENGTH_LONG).show();
//                        } else {
//                            Toast.makeText(getApplicationContext(),"Login Not Done",Toast.LENGTH_LONG).show();
//                        }
//                    }
//                });
//            }
//        });
        //endregion
    }

    public String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String result;
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        result = Base64.encodeToString(sha1hash, Base64.DEFAULT);
        result = result.substring(0, result.length()-1);
        return result;
    }

    private void openMainView() {
        setContentView(R.layout.activity_main);
    }

    private void openSignInView() {
        setContentView(R.layout.activity_sign_in);
        findViewById(R.id.sign_in_button).setOnClickListener(signInListener);
    }

    private final View.OnClickListener signInListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mHandler.sendEmptyMessage(AuthManager.MSG_PROMPT_ALWAYS);
        }
    };

    @Override
    public void onSignedIn() {
        // Must be run on the UI thread because it is modifying the UI
        runOnUiThread(this::openMainView);
    }

    @Override
    public void onSignedOut() {
        Toast.makeText(this, getString(R.string.auth_out_success), Toast.LENGTH_SHORT).show();
        runOnUiThread(this::openSignInView);
    }

    @Override
    public void onError(Exception e) {
        Toast.makeText(this, getString(R.string.err_auth, e.getLocalizedMessage()),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Required by ADAL
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        AuthManager.onSaveInstanceState(outState);
    }
}