package com.example.cooking.ui.fragments;

import android.os.Bundle;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import com.example.cooking.R;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class CatalogFragmentDirections {
  private CatalogFragmentDirections() {
  }

  @CheckResult
  @NonNull
  public static ActionCatalogToFilteredRecipes actionCatalogToFilteredRecipes(
      @NonNull String categoryName, @NonNull String filterKey, @NonNull String filterType) {
    return new ActionCatalogToFilteredRecipes(categoryName, filterKey, filterType);
  }

  public static class ActionCatalogToFilteredRecipes implements NavDirections {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    private ActionCatalogToFilteredRecipes(@NonNull String categoryName, @NonNull String filterKey,
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
    @SuppressWarnings("unchecked")
    public ActionCatalogToFilteredRecipes setCategoryName(@NonNull String categoryName) {
      if (categoryName == null) {
        throw new IllegalArgumentException("Argument \"categoryName\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("categoryName", categoryName);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionCatalogToFilteredRecipes setFilterKey(@NonNull String filterKey) {
      if (filterKey == null) {
        throw new IllegalArgumentException("Argument \"filterKey\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("filterKey", filterKey);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionCatalogToFilteredRecipes setFilterType(@NonNull String filterType) {
      if (filterType == null) {
        throw new IllegalArgumentException("Argument \"filterType\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("filterType", filterType);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle getArguments() {
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

    @Override
    public int getActionId() {
      return R.id.action_catalog_to_filteredRecipes;
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

    @Override
    public boolean equals(Object object) {
      if (this == object) {
          return true;
      }
      if (object == null || getClass() != object.getClass()) {
          return false;
      }
      ActionCatalogToFilteredRecipes that = (ActionCatalogToFilteredRecipes) object;
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
      if (getActionId() != that.getActionId()) {
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
      result = 31 * result + getActionId();
      return result;
    }

    @Override
    public String toString() {
      return "ActionCatalogToFilteredRecipes(actionId=" + getActionId() + "){"
          + "categoryName=" + getCategoryName()
          + ", filterKey=" + getFilterKey()
          + ", filterType=" + getFilterType()
          + "}";
    }
  }
}
