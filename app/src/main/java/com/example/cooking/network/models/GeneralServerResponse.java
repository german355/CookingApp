package com.example.cooking.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Базовый класс ответа сервера, особенно для операций создания и обновления,
 * когда возвращается ID ресурса
 */
public class GeneralServerResponse extends BaseApiResponse {
    @SerializedName(value = "recipeId", alternate = {"id"}) // Поддерживает как "recipeId", так и "id"
    private Integer id;
    
    @SerializedName(value = "photo_url", alternate = {"photoUrl"}) // Поддерживает "photo_url" и "photoUrl"
    private String photoUrl;

    /**
     * Получает ID созданного или обновленного ресурса
     * @return ID ресурса или null, если не применимо
     */
    public Integer getId() {
        return id;
    }

    /**
     * Устанавливает ID ресурса
     * @param id ID ресурса
     */
    public void setId(Integer id) {
        this.id = id;
    }
    
    /**
     * Получает URL загруженного изображения
     * @return URL изображения или null, если изображение не было загружено
     */
    public String getPhotoUrl() {
        return photoUrl;
    }
    
    /**
     * Устанавливает URL загруженного изображения
     * @param photoUrl URL изображения
     */
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}