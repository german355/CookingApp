package com.example.cooking.ui.fragments;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.example.cooking.R;

public class ProfileFragmentDirections {
  private ProfileFragmentDirections() {
  }

  @CheckResult
  @NonNull
  public static NavDirections actionProfileToSettings() {
    return new ActionOnlyNavDirections(R.id.action_profile_to_settings);
  }
}
