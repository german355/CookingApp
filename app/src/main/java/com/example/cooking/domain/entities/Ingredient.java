package com.example.cooking.domain.entities;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.cooking.domain.units.SupportedUnit;
import com.example.cooking.domain.units.UnitNormalizer;
import com.example.cooking.domain.units.UnitResolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Представляет ингредиент в рецепте.
 * Содержит название, количество и единицу измерения.
 * Реализует Parcelable для передачи между компонентами Android.
 */
public class Ingredient implements Parcelable {
    private String name;
    private float count;
    private String type;
    /**
     * Конструктор по умолчанию.
     * Может быть необходим для некоторых библиотек или процессов десериализации.
     */
    public Ingredient() {}

    protected Ingredient(Parcel in) {
        name = in.readString();
        count = in.readFloat();
        type = in.readString();
    }

    public static final Creator<Ingredient> CREATOR = new Creator<Ingredient>() {
        @Override
        public Ingredient createFromParcel(Parcel in) {
            return new Ingredient(in);
        }

        @Override
        public Ingredient[] newArray(int size) {
            return new Ingredient[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public float getCount() {
        return count;
    }

    public void setCount(float count) {
        this.count = count;
    }
    
    /**
     * Возвращает количество ингредиента в виде float.
     * Используется для совместимости или в случаях, где требуется float.
     * @return количество ингредиента.
     */
    public float getAmount() {
        return count;
    }
    
    /**
     * Возвращает единицу измерения ингредиента.
     * Если тип не указан, возвращает пустую строку.
     * @return единица измерения или пустая строку.
     */
    public String getUnit() {
        return type != null ? type : "";
    }

    public UnitResolution resolveUnit() {
        return UnitNormalizer.resolve(type);
    }

    public SupportedUnit getSupportedUnit() {
        return resolveUnit().getSupportedUnit();
    }

    public String getNormalizedType() {
        return resolveUnit().getNormalizedValue();
    }

    public boolean hasValidUnitValue() {
        return UnitNormalizer.isAcceptableForValidation(type);
    }

    public Ingredient copyForPersistence() {
        Ingredient copy = new Ingredient();
        copy.setName(name);
        copy.setCount(count);
        copy.setType(getNormalizedType());
        return copy;
    }

    public static ArrayList<Ingredient> copiesForPersistence(List<Ingredient> ingredients) {
        ArrayList<Ingredient> normalizedIngredients = new ArrayList<>();
        if (ingredients == null) {
            return normalizedIngredients;
        }

        for (Ingredient ingredient : ingredients) {
            normalizedIngredients.add(ingredient != null ? ingredient.copyForPersistence() : null);
        }
        return normalizedIngredients;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeFloat(count);
        dest.writeString(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Float.compare(count, that.count) == 0 &&
               Objects.equals(name, that.name) &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, count, type);
    }

    // Метод toString() для удобства логирования и отладки.
    @Override
    public String toString() {
        return "Ingredient{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", type='" + type + '\'' +
                '}';
    }
}
