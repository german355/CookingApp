package com.example.cooking.domain.entities;

import java.util.Objects;

/**
 * Представляет элемент категории для отображения в каталоге.
 * Содержит информацию, необходимую для идентификации, фильтрации и отображения категории.
 */
public class CategoryItem {
    private String name;
    private String filterKey; // Ключ, используемый для фильтрации рецептов по этой категории
    private String filterType; // Тип фильтра (например, "meal_type" или "food_type")
    private String description;
    private String imageUrl;

    public CategoryItem(String name, String filterKey, String filterType, String description, String imageUrl) {
        this.name = name;
        this.filterKey = filterKey;
        this.filterType = filterType;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilterKey() {
        return filterKey;
    }


    public String getFilterType() {
        return filterType;
    }



    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryItem that = (CategoryItem) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(filterKey, that.filterKey) &&
               Objects.equals(filterType, that.filterType) &&
               Objects.equals(description, that.description) &&
               Objects.equals(imageUrl, that.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, filterKey, filterType, description, imageUrl);
    }

    @Override
    public String toString() {
        return "CategoryItem{" +
               "name='" + name + '\'' +
               ", filterKey='" + filterKey + '\'' +
               ", filterType='" + filterType + '\'' +
               ", description='" + description + '\'' +
               ", imageUrl='" + imageUrl + '\'' +
               '}';
    }
} 