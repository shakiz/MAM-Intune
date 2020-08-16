package com.officework.intune;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MsLoginController {
    final static String[] SCOPES = {"https://graph.microsoft.com/User.Read"};
    final static String MSGRAPH_URL = "https://graph.microsoft.com/v1.0/me";

    private PublicClientApplication sampleApp;
    private IAuthenticationResult authResult;
    Context context;

    public MsLoginController(Context context) {
        this.context = context;
        sampleApp = new PublicClientApplication(context, R.raw.auth_config);
    }

    public interface onLogin {
        void onResult(boolean success, String message, String email);
    }

    public interface onLogout {
        void onResult(boolean success);
    }

    public void login(final onLogin onLogin) {
        sampleApp.acquireToken((Activity) context, SCOPES, getAuthInteractiveCallback(onLogin));
    }

    public void checkLogin(final onLogin onLogin) {
        sampleApp.getAccounts(new PublicClientApplication.AccountsLoadedCallback() {
            @Override
            public void onAccountsLoaded(final List<IAccount> accounts) {
                if (!accounts.isEmpty()) {
                    sampleApp.acquireTokenSilentAsync(SCOPES, accounts.get(0), getAuthSilentCallback(onLogin));
                } else {
                    if(onLogin != null){
                        onLogin.onResult(false, "Microsoft account not found", "");
                    }
                }
            }
        });
    }

    private AuthenticationCallback getAuthSilentCallback(final onLogin onLogin) {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d("AuthenticationCallback", "Successfully authenticated");
                authResult = authenticationResult;
                callGraphAPI(onLogin);
            }

            @Override
            public void onError(MsalException exception) {
                Log.d("AuthenticationCallback", exception.toString());
                if (exception instanceof MsalClientException) {
                    //Exception inside MSAL, more info inside MsalError.java
                } else if (exception instanceof MsalServiceException) {
                    //Exception when communicating with the STS, likely config issue
                } else if (exception instanceof MsalUiRequiredException) {
                    //Tokens expired or no session, retry with interactive
                }
                if(onLogin != null){
                    onLogin.onResult(false, "Authentication Failed", "");
                }
            }

            @Override
            public void onCancel() {
                Log.d("AuthenticationCallback", "User cancelled login.");
                if(onLogin != null){
                    onLogin.onResult(false, "User cancelled login", "");
                }
            }
        };
    }

    private AuthenticationCallback getAuthInteractiveCallback(final onLogin onLogin)
    {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d("AuthenticationCallback","Successfully authenticated");
                Log.d("AuthenticationCallback","ID Token: " + authenticationResult.getIdToken());
                authResult = authenticationResult;
                callGraphAPI(onLogin);
            }

            @Override
            public void onError(MsalException exception) {
                Log.d("AuthenticationCallback", exception.toString());
                if (exception instanceof MsalClientException) {
                    //Exception inside MSAL, more info inside MsalError.java
                } else if (exception instanceof MsalServiceException) {
                    //Exception when communicating with the STS, likely config issue
                }
                if(onLogin != null){
                    onLogin.onResult(false, "Authentication Failed","");
                }
            }

            @Override
            public void onCancel() {
                Log.d("AuthenticationCallback", "User cancelled login.");
                if(onLogin != null){
                    onLogin.onResult(false, "User cancelled login", "");
                }
            }
        };
    }

    private void callGraphAPI(final onLogin onLogin) {
        if (authResult.getAccessToken() == null) {
            if(onLogin != null){
                onLogin.onResult(false, "Authentication Failed", "");
            }
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(context);
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("key", "value");
        } catch (Exception e) {
            Log.d("GraphAPI", "Failed to put parameters: " + e.toString());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, MSGRAPH_URL,
                parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("GraphAPI", response.toString());
                getUserInfo(response, onLogin);
                Log.d("GraphAPI", "Successfully Login");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("GraphAPI", "Error: " + error.toString());
                if(onLogin != null){
                    onLogin.onResult(false, "API Authentication Failed", "");
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authResult.getAccessToken());
                return headers;
            }
        };

        Log.d("GraphAPI Request JSON", request.toString());

        request.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    private void getUserInfo(JSONObject jsonObject, onLogin onLogin){
        String token = "", mEmail = "";
        if(authResult != null) {
            token = authResult.getAccessToken();
        }
        if (jsonObject != null) {
            try {
                mEmail = jsonObject.getString("mail");
                if(onLogin != null){
                    onLogin.onResult(true, "Login Success", mEmail);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                if(onLogin != null){
                    onLogin.onResult(false, "API Response Failed", "");
                }
            }
        } else {
            if(onLogin != null){
                onLogin.onResult(false, "API Response Failed", "");
            }
        }
    }

    public void logout(final onLogout onLogout) {
        sampleApp.getAccounts(new PublicClientApplication.AccountsLoadedCallback() {
            @Override
            public void onAccountsLoaded(final List<IAccount> accounts) {
                if (accounts.isEmpty()) {
                    Log.d("No accounts to sign out!","");
                    if(onLogout != null){
                        onLogout.onResult(true);
                    }
                } else {
                    for (final IAccount account : accounts) {
                        sampleApp.removeAccount(account, new PublicClientApplication.AccountsRemovedCallback() {
                            @Override
                            public void onAccountsRemoved(Boolean isSuccess) {
                                if (isSuccess) {
                                    if(onLogout != null){
                                        onLogout.onResult(true);
                                    }
                                } else {
                                    if(onLogout != null){
                                        onLogout.onResult(false);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        });
    }
}
