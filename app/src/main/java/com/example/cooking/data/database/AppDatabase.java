package com.example.cooking.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabase.JournalMode;

/**
 * Основной класс базы данных приложения, построенный на Room Persistence Library.
 * Определяет сущности базы данных, версию схемы и предоставляет доступ к Data Access Objects (DAO).
 * Реализован как Singleton для обеспечения единственного экземпляра на все приложение.
 */
@Database(entities = {RecipeEntity.class, LikedRecipeEntity.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "recipes_database";
    private static volatile AppDatabase INSTANCE;
    
    /**
     * Предоставляет Data Access Object (DAO) для работы с сущностями рецептов ({@link RecipeEntity}).
     * @return {@link RecipeDao} для операций с рецептами.
     */
    public abstract RecipeDao recipeDao();
    
    /**
     * Предоставляет Data Access Object (DAO) для работы с сущностями лайкнутых рецептов ({@link LikedRecipeEntity}).
     * @return {@link LikedRecipeDao} для операций с лайкнутыми рецептами.
     */
    public abstract LikedRecipeDao likedRecipeDao();
    
    /**
     * Возвращает единственный экземпляр {@link AppDatabase}.
     * Если экземпляр еще не создан, инициализирует его потокобезопасным способом.
     * @param context Контекст приложения (используется для получения ApplicationContext).
     * @return Синглтон-экземпляр {@link AppDatabase}.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(), // Важно использовать ApplicationContext
                            AppDatabase.class,
                            DATABASE_NAME)
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .fallbackToDestructiveMigration() 
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}