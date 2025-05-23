package com.example.cooking.auth;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.cooking.data.database.LikedRecipeDao;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.Locale;

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

    // Интерфейсы обратных вызовов
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String message);
    }

    public interface UpdateProfileCallback {
        void onSuccess();
        void onError(String message);
    }

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
        Log.d(TAG, "Initializing Google Sign In with client ID: " + defaultWebClientId);
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(defaultWebClientId)
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(context, gso);
            
            // Проверка, что клиент успешно инициализирован
            if (googleSignInClient != null) {
                Log.d(TAG, "Google Sign In client initialized successfully");
            } else {
                Log.e(TAG, "Google Sign In client is null after initialization");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Google Sign In client", e);
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
     * Регистрация нового пользователя по email и паролю
     * @param email email для регистрации
     * @param password пароль для регистрации
     * @param authCallback интерфейс для обработки результата
     */
    public void registerWithEmailAndPassword(String email, String password, AuthCallback authCallback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        authCallback.onSuccess(user);

                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Неизвестная ошибка при регистрации";
                        authCallback.onError(errorMessage);
                    }
                });
    }

    /**
     * Вход с использованием email и пароля
     * @param email email для входа
     * @param password пароль для входа
     * @param authCallback интерфейс для обработки результата
     */
    public void signInWithEmailAndPassword(String email, String password, AuthCallback authCallback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        authCallback.onSuccess(user);
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Неизвестная ошибка при входе";
                        authCallback.onError(errorMessage);
                    }
                });
    }

    /**
     * Обновляет имя пользователя в профиле Firebase
     * @param user текущий пользователь Firebase
     * @param displayName новое имя пользователя
     * @param callback интерфейс для обработки результата
     */
    public void updateUserDisplayName(FirebaseUser user, String displayName, UpdateProfileCallback callback) {
        if (user == null) {
            callback.onError("Пользователь не авторизован");
            return;
        }

        user.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Неизвестная ошибка при обновлении профиля";
                        callback.onError(errorMessage);
                    }
                });
    }

    /**
     * Обновляет пароль пользователя в Firebase
     * @param user текущий пользователь Firebase
     * @param newPassword новый пароль
     * @param callback интерфейс для обработки результата
     */
    public void updateUserPassword(FirebaseUser user, String newPassword, UpdateProfileCallback callback) {
        if (user == null) {
            callback.onError("Пользователь не авторизован");
            return;
        }

        user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Неизвестная ошибка при обновлении пароля";
                        callback.onError(errorMessage);
                    }
                });
    }

    /**
     * Повторная аутентификация пользователя (необходима для операций, требующих недавней аутентификации)
     * @param user текущий пользователь Firebase
     * @param email email пользователя
     * @param password текущий пароль пользователя
     * @param callback интерфейс для обработки результата
     */
    public void reauthenticate(FirebaseUser user, String email, String password, AuthCallback callback) {
        if (user == null) {
            callback.onError("Пользователь не авторизован");
            return;
        }

        AuthCredential credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(user);
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Неизвестная ошибка при повторной аутентификации";
                        callback.onError(errorMessage);
                    }
                });
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
        
        try {
            Log.d(TAG, "Starting Google Sign In flow");
            Intent signInIntent = googleSignInClient.getSignInIntent();
            if (signInIntent != null) {
                Log.d(TAG, "Got sign in intent, starting activity for result with RC_SIGN_IN=" + RC_SIGN_IN);
                activity.startActivityForResult(signInIntent, RC_SIGN_IN);
            } else {
                Log.e(TAG, "Sign in intent is null");
                Toast.makeText(activity, "Ошибка запуска Google Sign In", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting Google Sign In flow", e);
            Toast.makeText(activity, "Ошибка запуска Google Sign In: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Обработка результата входа через Google аккаунт
     * @param data данные из onActivityResult
     * @param authCallback интерфейс для обработки результата
     */
    public void handleGoogleSignInResult(Intent data, AuthCallback authCallback) {
        try {
            Log.d(TAG, "Handling Google Sign In result");
            
            if (data == null) {
                Log.e(TAG, "Google Sign In data is null");
                authCallback.onError("Данные для Google Sign In отсутствуют");
                return;
            }
            
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);
            
            if (account == null || account.getIdToken() == null) {
                Log.e(TAG, "ID token is null, cannot authenticate with Firebase");
                authCallback.onError("ID токен отсутствует");
                return;
            }
            
            // Аутентификация с Firebase
            firebaseAuthWithGoogle(account.getIdToken(), authCallback);
            
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign In failed: " + e.getStatusCode(), e);
            authCallback.onError("Ошибка входа через Google: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during Google Sign In", e);
            authCallback.onError("Непредвиденная ошибка: " + e.getMessage());
        }
    }

    /**
     * Аутентификация с Firebase через Google ID токен
     * @param idToken ID токен Google
     * @param authCallback интерфейс для обработки результата
     */
    private void firebaseAuthWithGoogle(String idToken, AuthCallback authCallback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        authCallback.onSuccess(user);
                    } else {
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Неизвестная ошибка при аутентификации с Firebase";
                        authCallback.onError(errorMessage);
                    }
                });
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