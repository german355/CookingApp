package com.example.cooking.Recipe;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/**
 * Представляет ингредиент в рецепте.
 * Содержит название, количество и единицу измерения.
 * Реализует Parcelable для передачи между компонентами Android.
 */
public class Ingredient implements Parcelable {
    private String name; // Название ингредиента
    private int count;   // Количество ингредиента (целочисленное)
    private String type; // Единица измерения (например, "г", "мл", "шт")

    /**
     * Конструктор по умолчанию.
     * Может быть необходим для некоторых библиотек или процессов десериализации.
     */
    public Ingredient() {}

    protected Ingredient(Parcel in) {
        name = in.readString();
        count = in.readInt();
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
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
     * @return единица измерения или пустая строка.
     */
    public String getUnit() {
        return type != null ? type : "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(count);
        dest.writeString(type);
    }

    // Методы equals() и hashCode() для корректного сравнения и использования в коллекциях.
    // Учитывают поля, важные для идентификации объекта (например, для DiffUtil).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return count == that.count &&
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
