package com.example.cooking.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Базовый класс ответа сервера, особенно для операций создания и обновления,
 * когда возвращается ID ресурса
 */
public class GeneralServerResponse extends BaseApiResponse {
    @SerializedName("id") // Для ответа при создании, может содержать ID нового ресурса
    private Integer id;

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
} 