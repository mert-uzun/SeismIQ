package com.seismiq.app.auth;

import android.se.omapi.Session;
import android.util.Log;

import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.core.Amplify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuthService {
    private static final String TAG = "AuthService";

    public CompletableFuture<Boolean> registerUser(String username, String password, String email,
                                                   String name, String address,
                                                   boolean isVolunteer, boolean isSocialWorker) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        List<AuthUserAttribute> attributes = new ArrayList<>();
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.email(), email));
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.name(), name));
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.address(), address));
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.custom("custom:isVolunteer"), 
            String.valueOf(isVolunteer)));
        attributes.add(new AuthUserAttribute(AuthUserAttributeKey.custom("custom:isSocialWorker"), 
            String.valueOf(isSocialWorker)));


        AuthSignUpOptions options = AuthSignUpOptions.builder()
                .userAttributes(attributes)
                .build();

        Amplify.Auth.signUp(username, password, options,
                result -> {
                    future.complete(result.isSignUpComplete());
                    Log.i(TAG, "Sign up result: " + result.isSignUpComplete());
                },
                error -> future.completeExceptionally(error)
        );

        return future;
    }

    public CompletableFuture<Boolean> confirmSignUp(String username, String code) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Amplify.Auth.confirmSignUp(username, code,
                result -> future.complete(result.isSignUpComplete()),
                error -> future.completeExceptionally(error)
        );

        return future;
    }

    public CompletableFuture<Boolean> login(String username, String password) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Amplify.Auth.signIn(username, password,
                result -> future.complete(result.isSignedIn()),
                error -> future.completeExceptionally(error)
        );

        return future;
    }

    public CompletableFuture<Boolean> logout() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        AuthSignOutOptions options = AuthSignOutOptions.builder()
                .globalSignOut(true)
                .build();

        Amplify.Auth.signOut(options, signOutResult -> {
            if (signOutResult instanceof AWSCognitoAuthSignOutResult.CompleteSignOut) {
                Log.i(TAG, "successfully signed out");
            } else if (signOutResult instanceof AWSCognitoAuthSignOutResult.PartialSignOut) {
                Log.i(TAG, "Partially signed out");
            } else if (signOutResult instanceof AWSCognitoAuthSignOutResult.FailedSignOut) {
                Log.i(TAG, "Failed to sign out");
            }
        });
        return future;
    }

    public CompletableFuture<String> getIdToken() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Amplify.Auth.fetchAuthSession(
                result -> {
                    if (result instanceof AWSCognitoAuthSession) {
                        AWSCognitoAuthSession session = (AWSCognitoAuthSession) result;

                        if (session.isSignedIn()) {
                            try {
                                String token = session.getUserPoolTokensResult().getValue().getIdToken();
                                future.complete(token);
                                Log.i(TAG, "Token retrieved");
                            } catch (Exception e) {
                                future.completeExceptionally(new Exception("Failed to get IdToken", e));
                            }
                        } else {
                            future.completeExceptionally(new Exception("User not signed in"));
                        }
                    } else {
                        future.completeExceptionally(new Exception("Session is not AWSCognitoAuthSession"));
                    }
                },
                error -> future.completeExceptionally(error)
        );

        return future;
    }


    public CompletableFuture<Boolean> isLoggedIn() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Amplify.Auth.fetchAuthSession(
                result -> future.complete(result.isSignedIn()),
                error -> future.completeExceptionally(error)
        );

        return future;
    }

}
