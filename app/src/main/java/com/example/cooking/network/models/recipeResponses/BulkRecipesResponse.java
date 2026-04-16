package com.example.cooking.network.models.recipeResponses;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.network.models.BaseApiResponse;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class BulkRecipesResponse extends BaseApiResponse {
    @SerializedName("data")
    private List<Recipe> data;

    public List<Recipe> getData() {
        return data != null ? data : Collections.emptyList();
    }

    public void setData(List<Recipe> data) {
        this.data = data;
    }
}
