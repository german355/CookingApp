package com.example.cooking.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Класс, представляющий рецепт.
 * Содержит всю информацию о рецепте, необходимую для отображения и взаимодействия.
 * Реализует Parcelable для передачи между компонентами Android.
 */
public class Recipe implements Parcelable {
    private int id;
    private String title;
    private String created_at;
    private String userId;
    private boolean isLiked;

    @SerializedName("ingredients")
    @JsonAdapter(IngredientsAdapter.class)
    private ArrayList<Ingredient> ingredients = new ArrayList<>();

    @SerializedName("instructions")
    @JsonAdapter(StepsAdapter.class)
    private ArrayList<Step> steps = new ArrayList<>();
    
    @SerializedName("meal_type")
    private String mealType;
    
    @SerializedName("food_type")
    private String foodType;
    
    @SerializedName("photo")
    private String photo_url;
    private static <T> ArrayList<T> handleParseError(String context, Exception e) {
        android.util.Log.w("Recipe", "Failed to parse " + context + ": " + e.getMessage());
        return new ArrayList<>();
    }

    public static class IngredientsAdapter extends TypeAdapter<ArrayList<Ingredient>> {
        private static final Gson GSON_INSTANCE = new Gson();
        private static final Type INGREDIENT_LIST_TYPE = new TypeToken<ArrayList<Ingredient>>() {}.getType();

        @Override
        public void write(JsonWriter out, ArrayList<Ingredient> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            GSON_INSTANCE.toJson(value, INGREDIENT_LIST_TYPE, out);
        }

        @Override
        public ArrayList<Ingredient> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return new ArrayList<>();
            }

            if (in.peek() == JsonToken.STRING) {
                String jsonString = in.nextString();
                if (jsonString.isEmpty()) {
                    return new ArrayList<>();
                }
                try {
                    ArrayList<Ingredient> parsedIngredients = GSON_INSTANCE.fromJson(jsonString, INGREDIENT_LIST_TYPE);
                    return parsedIngredients != null ? parsedIngredients : new ArrayList<>();
                } catch (Exception e) {
                    return handleParseError("ingredients string", e);
                }
            } else if (in.peek() == JsonToken.BEGIN_ARRAY) {
                try {
                    ArrayList<Ingredient> parsedIngredients = GSON_INSTANCE.fromJson(in, INGREDIENT_LIST_TYPE);
                    return parsedIngredients != null ? parsedIngredients : new ArrayList<>();
                } catch (Exception e) {
                    return handleParseError("ingredients array", e);
                }
            } else {
                android.util.Log.w("Recipe", "Unexpected token for ingredients, skipping");
                in.skipValue();
                return new ArrayList<>();
            }
        }
    }

    public static class StepsAdapter extends TypeAdapter<ArrayList<Step>> {
        private static final Gson GSON_INSTANCE = new Gson();
        private static final Type STEP_LIST_TYPE = new TypeToken<ArrayList<Step>>() {}.getType();

        @Override
        public void write(JsonWriter out, ArrayList<Step> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            GSON_INSTANCE.toJson(value, STEP_LIST_TYPE, out);
        }

        @Override
        public ArrayList<Step> read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return new ArrayList<>();
            }

            if (in.peek() == JsonToken.STRING) {
                String jsonString = in.nextString();
                if (jsonString.isEmpty()) {
                    return new ArrayList<>();
                }
                try {
                    ArrayList<Step> parsedSteps = GSON_INSTANCE.fromJson(jsonString, STEP_LIST_TYPE);
                    return parsedSteps != null ? parsedSteps : new ArrayList<>();
                } catch (Exception e) {
                    return handleParseError("steps string", e);
                }
            } else if (in.peek() == JsonToken.BEGIN_ARRAY) {
                try {
                    ArrayList<Step> parsedSteps = GSON_INSTANCE.fromJson(in, STEP_LIST_TYPE);
                    return parsedSteps != null ? parsedSteps : new ArrayList<>();
                } catch (Exception e) {
                    return handleParseError("steps array", e);
                }
            } else {
                android.util.Log.w("Recipe", "Unexpected token for steps, skipping");
                in.skipValue();
                return new ArrayList<>();
            }
        }
    }
    
    /**
     * Конструктор по умолчанию. 
     * Необходим для некоторых процессов, например, Firebase десериализации или создания объекта с последующей установкой полей через сеттеры.
     */
    public Recipe() {
        // Пустой конструктор
    }

    public ArrayList<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(ArrayList<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public ArrayList<Step> getSteps() {
        return steps;
    }

    public void setSteps(ArrayList<Step> steps) {
        this.steps = steps;
    }

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }
    
    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Recipe{id=").append(id);
        sb.append(", title='").append(title).append('\'');
        sb.append(", created_at='").append(created_at).append('\'');
        sb.append(", userId='").append(userId).append('\'');
        sb.append(", isLiked=").append(isLiked);
        sb.append(", ingredients=").append(ingredients != null ? ingredients.size() : 0);
        sb.append(", steps=").append(steps != null ? steps.size() : 0);
        sb.append(", mealType='").append(mealType).append('\'');
        sb.append(", foodType='").append(foodType).append('\'');
        sb.append(", photo_url='").append(photo_url).append('\'');
        sb.append('}');
        return sb.toString();
    }


    public static final Parcelable.Creator<Recipe> CREATOR = new Parcelable.Creator<Recipe>() {
        @Override
        public Recipe createFromParcel(Parcel in) {
            return new Recipe(in);
        }

        @Override
        public Recipe[] newArray(int size) {
            return new Recipe[size];
        }
    };

    protected Recipe(Parcel in) {
        id = in.readInt();
        title = in.readString();
        created_at = in.readString();
        userId = in.readString();
        isLiked = in.readByte() != 0;
        ingredients = new ArrayList<>();
        in.readList(ingredients, Ingredient.class.getClassLoader());
        steps = new ArrayList<>();
        in.readList(steps, Step.class.getClassLoader());
        mealType = in.readString();
        foodType = in.readString();
        photo_url = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(created_at);
        dest.writeString(userId);
        dest.writeByte((byte) (isLiked ? 1 : 0));
        dest.writeList(ingredients);
        dest.writeList(steps);
        dest.writeString(mealType);
        dest.writeString(foodType);
        dest.writeString(photo_url);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        return id == recipe.id &&
               isLiked == recipe.isLiked &&
               Objects.equals(title, recipe.title) &&
               Objects.equals(created_at, recipe.created_at) &&
               Objects.equals(userId, recipe.userId) &&
               Objects.equals(ingredients, recipe.ingredients) &&
               Objects.equals(steps, recipe.steps) &&
               Objects.equals(mealType, recipe.mealType) &&
               Objects.equals(foodType, recipe.foodType) &&
               Objects.equals(photo_url, recipe.photo_url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, created_at, userId, isLiked, ingredients, steps, mealType, foodType, photo_url);
    }
}