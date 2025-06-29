package com.example.cooking.network.models.recipeResponses;

import com.example.cooking.network.models.BaseApiResponse;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для представления ответа от поискового API
 */
public class SearchResponse extends BaseApiResponse {
    
    @SerializedName("data")
    private Data data;

    /**
     * Получает объект данных из ответа
     * @return объект данных
     */
    public Data getData() {
        return data;
    }

    /**
     * Устанавливает объект данных
     * @param data объект данных
     */
    public void setData(Data data) {
        this.data = data;
    }
    
    /**
     * Получает список результатов поиска
     * @return список ID рецептов
     */
    public List<String> getResults() {
        return data != null ? data.getResults() : null;
    }
    
    /**
     * Получает общее количество результатов поиска
     * @return общее количество найденных рецептов
     */
    public int getTotalResults() {
        return data != null ? data.getTotalResults() : 0;
    }

    /**
     * Внутренний класс для представления данных ответа
     */
    public static class Data {
        @SerializedName("results")
        private List<String> results;
        
        @SerializedName("total_results")
        private int totalResults;
        
        /**
         * Получает результаты поиска
         * @return список ID найденных рецептов
         */
        public List<String> getResults() {
            return results != null ? results : new ArrayList<>();
        }
        
        /**
         * Устанавливает результаты поиска
         * @param results список ID найденных рецептов
         */
        public void setResults(List<String> results) {
            this.results = results;
        }
        
        /**
         * Получает общее количество результатов
         * @return общее количество найденных рецептов
         */
        public int getTotalResults() {
            return totalResults;
        }
        
        /**
         * Устанавливает общее количество результатов
         * @param totalResults общее количество найденных рецептов
         */
        public void setTotalResults(int totalResults) {
            this.totalResults = totalResults;
        }
    }
}
