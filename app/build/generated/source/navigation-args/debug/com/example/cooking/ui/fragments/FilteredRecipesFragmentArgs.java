package com.example.cooking.ui.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.navigation.NavArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class FilteredRecipesFragmentArgs implements NavArgs {
  private final HashMap arguments = new HashMap();

  private FilteredRecipesFragmentArgs() {
  }

  @SuppressWarnings("unchecked")
  private FilteredRecipesFragmentArgs(HashMap argumentsMap) {
    this.arguments.putAll(argumentsMap);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static FilteredRecipesFragmentArgs fromBundle(@NonNull Bundle bundle) {
    FilteredRecipesFragmentArgs __result = new FilteredRecipesFragmentArgs();
    bundle.setClassLoader(FilteredRecipesFragmentArgs.class.getClassLoader());
    if (bundle.containsKey("categoryName")) {
      String categoryName;
      categoryName = bundle.getString("categoryName");
      if (categoryName == null) {
        throw new IllegalArgumentException("Argument \"categoryName\" is marked as non-null but was passed a null value.");
      }
      __result.arguments.put("categoryName", categoryName);
    } else {
      throw new IllegalArgumentException("Required argument \"categoryName\" is missing and does not have an android:defaultValue");
    }
    if (bundle.containsKey("filterKey")) {
      String filterKey;
      filterKey = bundle.getString("filterKey");
      if (filterKey == null) {
        throw new IllegalArgumentException("Argument \"filterKey\" is marked as non-null but was passed a null value.");
      }
      __result.arguments.put("filterKey", filterKey);
    } else {
      throw new IllegalArgumentException("Required argument \"filterKey\" is missing and does not have an android:defaultValue");
    }
    if (bundle.containsKey("filterType")) {
      String filterType;
      filterType = bundle.getString("filterType");
      if (filterType == null) {
        throw new IllegalArgumentException("Argument \"filterType\" is marked as non-null but was passed a null value.");
      }
      __result.arguments.put("filterType", filterType);
    } else {
      throw new IllegalArgumentException("Required argument \"filterType\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static FilteredRecipesFragmentArgs fromSavedStateHandle(
      @NonNull SavedStateHandle savedStateHandle) {
    FilteredRecipesFragmentArgs __result = new FilteredRecipesFragmentArgs();
    if (savedStateHandle.contains("categoryName")) {
      String categoryName;
      categoryName = savedStateHandle.get("categoryName");
      if (categoryName == null) {
        throw new IllegalArgumentException("Argument \"categoryName\" is marked as non-null but was passed a null value.");
      }
      __result.arguments.put("categoryName", categoryName);
    } else {
      throw new IllegalArgumentException("Required argument \"categoryName\" is missing and does not have an android:defaultValue");
    }
    if (savedStateHandle.contains("filterKey")) {
      String filterKey;
      filterKey = savedStateHandle.get("filterKey");
      if (filterKey == null) {
        throw new IllegalArgumentException("Argument \"filterKey\" is marked as non-null but was passed a null value.");
      }
      __result.arguments.put("filterKey", filterKey);
    } else {
      throw new IllegalArgumentException("Required argument \"filterKey\" is missing and does not have an android:defaultValue");
    }
    if (savedStateHandle.contains("filterType")) {
      String filterType;
      filterType = savedStateHandle.get("filterType");
      if (filterType == null) {
        throw new IllegalArgumentException("Argument \"filterType\" is marked as non-null but was passed a null value.");
      }
      __result.arguments.put("filterType", filterType);
    } else {
      throw new IllegalArgumentException("Required argument \"filterType\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public String getCategoryName() {
    return (String) arguments.get("categoryName");
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public String getFilterKey() {
    return (String) arguments.get("filterKey");
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public String getFilterType() {
    return (String) arguments.get("filterType");
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public Bundle toBundle() {
    Bundle __result = new Bundle();
    if (arguments.containsKey("categoryName")) {
      String categoryName = (String) arguments.get("categoryName");
      __result.putString("categoryName", categoryName);
    }
    if (arguments.containsKey("filterKey")) {
      String filterKey = (String) arguments.get("filterKey");
      __result.putString("filterKey", filterKey);
    }
    if (arguments.containsKey("filterType")) {
      String filterType = (String) arguments.get("filterType");
      __result.putString("filterType", filterType);
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public SavedStateHandle toSavedStateHandle() {
    SavedStateHandle __result = new SavedStateHandle();
    if (arguments.containsKey("categoryName")) {
      String categoryName = (String) arguments.get("categoryName");
      __result.set("categoryName", categoryName);
    }
    if (arguments.containsKey("filterKey")) {
      String filterKey = (String) arguments.get("filterKey");
      __result.set("filterKey", filterKey);
    }
    if (arguments.containsKey("filterType")) {
      String filterType = (String) arguments.get("filterType");
      __result.set("filterType", filterType);
    }
    return __result;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
        return true;
    }
    if (object == null || getClass() != object.getClass()) {
        return false;
    }
    FilteredRecipesFragmentArgs that = (FilteredRecipesFragmentArgs) object;
    if (arguments.containsKey("categoryName") != that.arguments.containsKey("categoryName")) {
      return false;
    }
    if (getCategoryName() != null ? !getCategoryName().equals(that.getCategoryName()) : that.getCategoryName() != null) {
      return false;
    }
    if (arguments.containsKey("filterKey") != that.arguments.containsKey("filterKey")) {
      return false;
    }
    if (getFilterKey() != null ? !getFilterKey().equals(that.getFilterKey()) : that.getFilterKey() != null) {
      return false;
    }
    if (arguments.containsKey("filterType") != that.arguments.containsKey("filterType")) {
      return false;
    }
    if (getFilterType() != null ? !getFilterType().equals(that.getFilterType()) : that.getFilterType() != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + (getCategoryName() != null ? getCategoryName().hashCode() : 0);
    result = 31 * result + (getFilterKey() != null ? getFilterKey().hashCode() : 0);
    result = 31 * result + (getFilterType() != null ? getFilterType().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FilteredRecipesFragmentArgs{"
        + "categoryName=" + getCategoryName()
        + ", filterKey=" + getFilterKey()
        + ", filterType=" + getFilterType()
        + "}";
  }

  public static final class Builder {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    public Builder(@NonNull FilteredRecipesFragmentArgs original) {
      this.arguments.putAll(original.arguments);
    }

    @SuppressWarnings("unchecked")
    public Builder(@NonNull String categoryName, @NonNull String filterKey,
        @NonNull String filterType) {
      if (categoryName == null) {
        throw new IllegalArgumentException("Argument \"categoryName\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("categoryName", categoryName);
      if (filterKey == null) {
        throw new IllegalArgumentException("Argument \"filterKey\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("filterKey", filterKey);
      if (filterType == null) {
        throw new IllegalArgumentException("Argument \"filterType\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("filterType", filterType);
    }

    @NonNull
    public FilteredRecipesFragmentArgs build() {
      FilteredRecipesFragmentArgs result = new FilteredRecipesFragmentArgs(arguments);
      return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public Builder setCategoryName(@NonNull String categoryName) {
      if (categoryName == null) {
        throw new IllegalArgumentException("Argument \"categoryName\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("categoryName", categoryName);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public Builder setFilterKey(@NonNull String filterKey) {
      if (filterKey == null) {
        throw new IllegalArgumentException("Argument \"filterKey\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("filterKey", filterKey);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public Builder setFilterType(@NonNull String filterType) {
      if (filterType == null) {
        throw new IllegalArgumentException("Argument \"filterType\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("filterType", filterType);
      return this;
    }

    @SuppressWarnings({"unchecked","GetterOnBuilder"})
    @NonNull
    public String getCategoryName() {
      return (String) arguments.get("categoryName");
    }

    @SuppressWarnings({"unchecked","GetterOnBuilder"})
    @NonNull
    public String getFilterKey() {
      return (String) arguments.get("filterKey");
    }

    @SuppressWarnings({"unchecked","GetterOnBuilder"})
    @NonNull
    public String getFilterType() {
      return (String) arguments.get("filterType");
    }
  }
}
