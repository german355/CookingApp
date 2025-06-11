package com.example.cooking.ui.fragments;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.example.cooking.R;

public class SharedProfileFragmentDirections {
  private SharedProfileFragmentDirections() {
  }

  @CheckResult
  @NonNull
  public static NavDirections actionSharedProfileToProfile() {
    return new ActionOnlyNavDirections(R.id.action_sharedProfile_to_profile);
  }

  @CheckResult
  @NonNull
  public static NavDirections actionSharedProfileToAuth() {
    return new ActionOnlyNavDirections(R.id.action_sharedProfile_to_auth);
  }

  @CheckResult
  @NonNull
  public static NavDirections actionSharedProfileToSettings() {
    return new ActionOnlyNavDirections(R.id.action_sharedProfile_to_settings);
  }
}
