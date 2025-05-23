package com.example.cooking.ui.fragments;

import android.os.Bundle;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.cooking.R;
import com.example.cooking.ui.viewmodels.MainViewModel;
import com.example.cooking.auth.FirebaseAuthManager;
import com.google.firebase.auth.FirebaseUser;

public class SharedProfileFragment extends Fragment {

    TextView profileName;
    TextView profile_description;
    ConstraintLayout profile;
    ConstraintLayout settings;
    private MainViewModel activityViewModel;
    private NavController navController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_shared_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        profileName = view.findViewById(R.id.profile_name);
        profile_description = view.findViewById(R.id.profile_description);
        profile = view.findViewById(R.id.profile_container);
        settings = view.findViewById(R.id.settings_container);

        // Получаем NavController
        navController = Navigation.findNavController(view);

        profile.setOnClickListener(view1 -> {
            Boolean isLoggedIn = activityViewModel.getIsUserLoggedIn().getValue();

            if (isLoggedIn != null && isLoggedIn) {
                // Используем NavController для навигации к профилю
                navController.navigate(R.id.action_sharedProfile_to_profile);
            } else {
                // Используем NavController для навигации к авторизации
                navController.navigate(R.id.action_sharedProfile_to_auth);
            }
        });

        activityViewModel.getIsUserLoggedIn().getValue();
        activityViewModel.getIsUserLoggedIn().observe(getViewLifecycleOwner(), isLoggedIn -> {
            if (isLoggedIn != null && isLoggedIn) {
                FirebaseUser currentUser = FirebaseAuthManager.getInstance().getCurrentUser();

                if (currentUser != null) {
                    String displayName = currentUser.getDisplayName();
                    profileName.setText((displayName != null && !displayName.isEmpty()) ? displayName : "Профиль");
                    profile_description.setText("Настройки профиля");
                } else {
                    profileName.setText("Ошибка");
                    profile_description.setText("Профиль (ошибка данных)");
                }
            } else {
                profileName.setText("Гость");
                profile_description.setText("Войти в профиль");
            }
        });

        settings.setOnClickListener(view1 -> {
            // Используем NavController для навигации к настройкам
            navController.navigate(R.id.action_sharedProfile_to_settings);
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }
}