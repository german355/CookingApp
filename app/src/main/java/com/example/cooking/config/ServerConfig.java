package com.example.cooking.config;

/**
 * Класс-конфигурация для хранения настроек сервера и API
 */
public class ServerConfig {
    
    /**
     * Базовый URL API сервера
     * Формат: http://домен:порт
     */
    public static String BASE_API_URL = "http://89.35.130.107/";
    
    /**
     * Endpoint для получения списка рецептов
     */
    public static final String ENDPOINT_RECIPES = "/recipes";
    
    /**
     * Endpoint для добавления рецепта в избранное
     */
    public static final String ENDPOINT_LIKE = "/like";
    
    /**
     * Endpoint для удаления рецепта из избранного
     */
    public static final String ENDPOINT_UNLIKE = "/unlike";
    
    /**
     * Endpoint для добавления нового рецепта
     */
    public static final String ENDPOINT_ADD_RECIPE = "/addRecipe";
    
    /**
     * Получить полный URL для указанного endpoint
     * @param endpoint endpoint API
     * @return полный URL
     */
    public static String getFullUrl(String endpoint) {
        return BASE_API_URL + endpoint;
    }
} 