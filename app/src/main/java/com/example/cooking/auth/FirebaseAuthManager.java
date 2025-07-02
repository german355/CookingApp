package com.example.cooking.auth;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.lang.ref.WeakReference;
import java.util.Locale;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Completable;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.EmailAuthProvider;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * Основной класс для работы с Firebase Authentication
 */
public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private static FirebaseAuthManager instance;
    private final FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private Context context;
    
    // Константа для запроса авторизации через Google
    public static final int RC_SIGN_IN = 9001;

    /**
     * Конструктор по умолчанию
     */
    public FirebaseAuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        // Устанавливаем язык X-Firebase-Locale в формате ISO 639
        firebaseAuth.setLanguageCode(Locale.getDefault().getLanguage());
    }

    /**
     * Конструктор с контекстом приложения
     */
    public FirebaseAuthManager(Application application) {
        this();
        this.context = application.getApplicationContext();
    }

    /**
     * Получение экземпляра класса (Singleton)
     */
    public static synchronized FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    /**
     * Инициализирует клиент для входа через Google
     * @param context контекст приложения
     * @param defaultWebClientId строковый идентификатор из google-services.json
     */
    public void initGoogleSignIn(Context context, String defaultWebClientId) {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(defaultWebClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(context, gso);

        } catch (Exception e) {
            Toast.makeText(context, "Ошибка настройки Google Sign In", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Проверяет, авторизован ли пользователь в данный момент
     * @return true, если пользователь авторизован, иначе false
     */
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Возвращает текущего пользователя Firebase
     * @return объект FirebaseUser или null, если пользователь не авторизован
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * RxJava Single для входа с помощью email/пароля
     */
    public Single<FirebaseUser> signInWithEmailAndPasswordSingle(String email, String password) {
        return Single.<FirebaseUser>create(emitter -> {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        emitter.onSuccess(firebaseAuth.getCurrentUser());
                    } else {
                        Throwable e = task.getException() != null ? task.getException() : new Exception("Unknown error signing in");
                        emitter.onError(e);
                    }
                });
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * RxJava Single для регистрации нового пользователя по email/паролю
     */
    public Single<FirebaseUser> registerWithEmailAndPasswordSingle(String email, String password) {
        return Single.<FirebaseUser>create(emitter -> {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        emitter.onSuccess(firebaseAuth.getCurrentUser());
                    } else {
                        Throwable e = task.getException() != null ? task.getException() : new Exception("Unknown error registering");
                        emitter.onError(e);
                    }
                });
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * RxJava Single для получения ID токена текущего пользователя
     */
    public Single<String> getIdTokenSingle(boolean forceRefresh) {
        return Single.<String>create(emitter -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                emitter.onError(new NullPointerException("Current user is null"));
                return;
            }
            user.getIdToken(forceRefresh).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    emitter.onSuccess(task.getResult().getToken());
                } else {
                    Throwable e = task.getException() != null ? task.getException() : new Exception("Unknown error getting token");
                    emitter.onError(e);
                }
            });
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * RxJava Completable для обновления displayName
     */
    public Completable updateUserDisplayNameCompletable(FirebaseUser user, String displayName) {
        return Completable.create(emitter -> {
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(displayName).build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        emitter.onComplete();
                    } else {
                        emitter.onError(task.getException() != null ? task.getException() : new Exception("Unknown error updating display name"));
                    }
                });
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * RxJava Completable для обновления пароля
     */
    public Completable updateUserPasswordCompletable(FirebaseUser user, String newPassword) {
        return Completable.create(emitter -> {
            user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        emitter.onComplete();
                    } else {
                        emitter.onError(task.getException() != null ? task.getException() : new Exception("Unknown error updating password"));
                    }
                });
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * RxJava Single для повторной аутентификации
     */
    public Single<FirebaseUser> reauthenticateSingle(FirebaseUser user, String email, String password) {
        return Single.<FirebaseUser>create(emitter -> {
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    emitter.onSuccess(firebaseAuth.getCurrentUser());
                } else {
                    emitter.onError(task.getException() != null ? task.getException() : new Exception("Unknown error reauthenticating"));
                }
            });
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * RxJava Single для аутентификации через Google ID токен
     */
    public Single<FirebaseUser> firebaseAuthWithGoogleSingle(String idToken) {
        return Single.<FirebaseUser>create(emitter -> {
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        emitter.onSuccess(firebaseAuth.getCurrentUser());
                    } else {
                        emitter.onError(task.getException() != null ? task.getException() : new Exception("Unknown error signing in with Google"));
                    }
                });
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * RxJava Single для обработки результата Google Sign-In Intent
     */
    public Single<FirebaseUser> handleGoogleSignInResultSingle(Intent data) {
        return Single.defer(() -> {
            if (data == null) {
                Log.e(TAG, "Google Sign-In data is null");
                return Single.error(new IllegalArgumentException("Google Sign-In data is null"));
            }
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account == null || account.getIdToken() == null) {
                    Log.e(TAG, "Google Sign-In account or ID token is null");
                    return Single.error(new Exception("ID token is null"));
                }
                Log.d(TAG, "Successfully got Google Sign-In account, proceeding to Firebase auth");
                return firebaseAuthWithGoogleSingle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Google Sign-In ApiException: " + e.getStatusCode());
                if (e.getStatusCode() == 10) {
                    Log.e(TAG, "DEVELOPER_ERROR (10): Check SHA1 fingerprints in Firebase Console and verify google-services.json is up to date");
                    return Single.error(new Exception("Ошибка конфигурации Google Sign-In. Проверьте настройки проекта в Firebase Console."));
                }
                return Single.error(e);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Начинает процесс входа через Google аккаунт
     * @param activity активность для запуска Intent
     */
    public void signInWithGoogle(Activity activity) {
        if (googleSignInClient == null) {
            Log.e(TAG, "Google Sign In client not initialized");
            Toast.makeText(activity, "Ошибка инициализации Google Sign In", Toast.LENGTH_SHORT).show();
            throw new IllegalStateException("Google Sign In was not initialized. Call initGoogleSignIn() first.");
        }
        
        if (activity == null) {
            Log.e(TAG, "Activity is null, cannot start Google Sign In");
            return;
        }

        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "Activity is finishing or destroyed, cannot start Google Sign In");
            return;
        }
        
        try {
            Log.d(TAG, "Starting Google Sign In flow");
            // Используем WeakReference для избежания утечек памяти и race condition
            WeakReference<Activity> activityRef = new WeakReference<>(activity);
            
            // Очищаем предыдущий аккаунт для избежания кеширования
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Activity activityFromRef = activityRef.get();
                
                // Проверяем валидность Activity после асинхронной операции
                if (activityFromRef == null) {
                    Log.w(TAG, "Activity reference is null after signOut, cannot proceed with Google Sign In");
                    return;
                }
                
                if (activityFromRef.isFinishing() || activityFromRef.isDestroyed()) {
                    Log.w(TAG, "Activity is finishing or destroyed after signOut, cannot proceed with Google Sign In");
                    return;
                }
                
                // Проверяем результат операции signOut
                if (!task.isSuccessful()) {
                    Log.w(TAG, "SignOut failed, but continuing with sign in", task.getException());
                }
                
                try {
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    if (signInIntent != null) {
                        Log.d(TAG, "Got sign in intent, starting activity for result with RC_SIGN_IN=" + RC_SIGN_IN);
                        activityFromRef.startActivityForResult(signInIntent, RC_SIGN_IN);
                    } else {
                        Log.e(TAG, "Sign in intent is null");
                        Toast.makeText(activityFromRef, "Ошибка запуска Google Sign In", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting sign in intent or starting activity", e);
                    Toast.makeText(activityFromRef, "Ошибка запуска Google Sign In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting Google Sign In flow", e);
            Toast.makeText(activity, "Ошибка запуска Google Sign In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Выход из аккаунта
     */
    public void signOut() {
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }

        firebaseAuth.signOut();
    }
}