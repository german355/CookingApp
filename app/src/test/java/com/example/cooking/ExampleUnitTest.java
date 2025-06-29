package com.example.cooking;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
    
    @Test
    public void moderation_error_parsing_test() {
        // Тест парсинга ошибок модерации
        String moderationError = "Модерация: Рецепт содержит неподходящий контент";
        
        // Проверяем что строка начинается с префикса модерации
        assertTrue("Ошибка должна начинаться с 'Модерация:'", 
                   moderationError.startsWith("Модерация:"));
        
        // Извлекаем сообщение модерации
        String message = moderationError.substring("Модерация:".length()).trim();
        assertEquals("Рецепт содержит неподходящий контент", message);
        
        // Проверяем что сообщение не пустое
        assertFalse("Сообщение модерации не должно быть пустым", message.isEmpty());
    }
    
    @Test
    public void moderation_error_empty_message_test() {
        // Тест обработки пустого сообщения модерации
        String moderationError = "Модерация: ";
        
        String message = moderationError.substring("Модерация:".length()).trim();
        assertTrue("Пустое сообщение модерации должно быть обработано", message.isEmpty());
    }
}