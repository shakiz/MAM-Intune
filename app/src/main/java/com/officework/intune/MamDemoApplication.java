/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.officework.intune;

import android.app.Application;
import android.util.Log;

import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.Logger;
import com.microsoft.intune.mam.client.app.MAMComponents;
import com.microsoft.intune.mam.client.notification.MAMNotificationReceiverRegistry;
import com.microsoft.intune.mam.policy.MAMEnrollmentManager;
import com.microsoft.intune.mam.policy.notification.MAMEnrollmentNotification;
import com.microsoft.intune.mam.policy.notification.MAMNotificationType;
import com.officework.intune.auth.AuthManager;

/**
 * Specifies what happens when the app is launched and terminated.
 *
 * Registers an authentication callback for MAM.
 */
public class MamDemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Registers a MAMAuthenticationCallback, which will try to acquire access tokens for MAM
        MAMEnrollmentManager mgr = MAMComponents.get(MAMEnrollmentManager.class);
        AuthenticationContext authContext =
                new AuthenticationContext(getApplicationContext(), AuthManager.AUTHORITY, true);
        mgr.registerAuthenticationCallback(
                (final String upn, final String aadId, final String resourceId) ->
                        AuthManager.getAccessTokenForMAM(authContext, this, upn, aadId, resourceId));

        /* This section shows how to register a MAMNotificationReceiver, so you can perform custom
         * actions if MAM requests certain things. More information is available here:
         * https://docs.microsoft.com/en-us/intune/app-sdk-android#types-of-notifications */
        MAMComponents.get(MAMNotificationReceiverRegistry.class).registerReceiver(notification -> {
            if (notification instanceof MAMEnrollmentNotification) {
                MAMEnrollmentManager.Result result =
                        ((MAMEnrollmentNotification) notification).getEnrollmentResult();
                switch (result) {
                    case AUTHORIZATION_NEEDED:
                    case NOT_LICENSED:
                    case ENROLLMENT_SUCCEEDED:
                    case ENROLLMENT_FAILED:
                    case WRONG_USER:
                    case UNENROLLMENT_SUCCEEDED:
                    case UNENROLLMENT_FAILED:
                    case PENDING:
                    case COMPANY_PORTAL_REQUIRED:
                    default:
                        Log.d("Enrollment Receiver", result.name());
                        break;
                }
            } else {
                Log.d("Enrollment Receiver", "Unexpected notification type received");
            }
            return true;
        }, MAMNotificationType.MAM_ENROLLMENT_RESULT);

        /* ADAL logging is enabled in the app by default for troubleshooting purposes.
         * More information is available here:
         * https://github.com/AzureAD/azure-activedirectory-library-for-android/#logs */
        Logger.getInstance().setAndroidLogEnabled(true);
    }
}
