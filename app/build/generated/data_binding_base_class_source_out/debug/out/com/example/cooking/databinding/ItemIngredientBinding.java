// Generated by view binder compiler. Do not edit!
package com.example.cooking.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.cooking.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ItemIngredientBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final TextView ingredientAmount;

  @NonNull
  public final TextView ingredientName;

  private ItemIngredientBinding(@NonNull LinearLayout rootView, @NonNull TextView ingredientAmount,
      @NonNull TextView ingredientName) {
    this.rootView = rootView;
    this.ingredientAmount = ingredientAmount;
    this.ingredientName = ingredientName;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemIngredientBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemIngredientBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_ingredient, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemIngredientBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.ingredient_amount;
      TextView ingredientAmount = ViewBindings.findChildViewById(rootView, id);
      if (ingredientAmount == null) {
        break missingId;
      }

      id = R.id.ingredient_name;
      TextView ingredientName = ViewBindings.findChildViewById(rootView, id);
      if (ingredientName == null) {
        break missingId;
      }

      return new ItemIngredientBinding((LinearLayout) rootView, ingredientAmount, ingredientName);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
