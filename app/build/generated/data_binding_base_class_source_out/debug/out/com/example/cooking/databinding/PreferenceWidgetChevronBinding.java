// Generated by view binder compiler. Do not edit!
package com.example.cooking.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import com.example.cooking.R;
import java.lang.NullPointerException;
import java.lang.Override;

public final class PreferenceWidgetChevronBinding implements ViewBinding {
  @NonNull
  private final ImageView rootView;

  private PreferenceWidgetChevronBinding(@NonNull ImageView rootView) {
    this.rootView = rootView;
  }

  @Override
  @NonNull
  public ImageView getRoot() {
    return rootView;
  }

  @NonNull
  public static PreferenceWidgetChevronBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static PreferenceWidgetChevronBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.preference_widget_chevron, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static PreferenceWidgetChevronBinding bind(@NonNull View rootView) {
    if (rootView == null) {
      throw new NullPointerException("rootView");
    }

    return new PreferenceWidgetChevronBinding((ImageView) rootView);
  }
}
