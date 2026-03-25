package com.example.tic_tac_toe_wifi;

import java.util.ArrayList;
import java.util.Random;

/**
 * Клас, що відповідає за логіку штучного інтелекту (бота) для гри в Хрестики-Нулики.
 * Інкапсулює алгоритми обчислення наступного ходу залежно від обраного рівня складності.
 * * Індексація ігрового поля (масив з 9 елементів):
 * 0 | 1 | 2
 * ---------
 * 3 | 4 | 5
 * ---------
 * 6 | 7 | 8
 */
public class TicTacToeBot {

    /**
     * Двовимірний масив, що містить усі можливі виграшні комбінації (індекси клітинок).
     * Включає 3 горизонталі, 3 вертикалі та 2 діагоналі.
     */
    private final int[][] winPositions = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // Горизонталі
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // Вертикалі
            {0, 4, 8}, {2, 4, 6}             // Діагоналі
    };

    /**
     * Головний метод для отримання найкращого ходу бота.
     * Маршрутизує виклик до відповідного алгоритму на основі рівня складності.
     *
     * @param gameState Поточний стан дошки, де: 0 - пусто, 1 - гравець (людина), 2 - бот.
     * @param aiLevel   Рівень складності:
     * 1 - Повністю випадкові ходи.
     * 2 - Здебільшого випадкові (30% шансу на обдуманий хід).
     * 3 - Збалансований (60% шансу на обдуманий хід).
     * 4 - Уважний (завжди блокує загрозу та забирає перемогу, але без стратегії наперед).
     * 5 - Непереможний (використовує алгоритм Minimax).
     * @return Індекс клітинки (0-8) для наступного ходу, або -1, якщо вільних клітинок немає.
     */
    public int getBestMove(int[] gameState, int aiLevel) {
        switch (aiLevel) {
            case 1: return getRandomMove(gameState);
            case 2: return getSmartRandomMove(gameState, 0.3);
            case 3: return getSmartRandomMove(gameState, 0.6);
            case 4: return getBestMoveSimple(gameState);
            case 5: return getBestMoveMinimax(gameState);
            default: return getRandomMove(gameState);
        }
    }

    /**
     * Робить повністю випадковий хід у будь-яку доступну вільну клітинку.
     *
     * @param gameState Поточний стан дошки.
     * @return Випадковий індекс вільної клітинки або -1.
     */
    private int getRandomMove(int[] gameState) {
        ArrayList<Integer> emptySpots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (gameState[i] == 0) emptySpots.add(i);
        }
        if (emptySpots.isEmpty()) return -1;
        return emptySpots.get(new Random().nextInt(emptySpots.size()));
    }

    /**
     * Комбінований підхід: з певною ймовірністю робить логічний хід (перемога/блокування),
     * інакше робить випадковий хід. Додає ефект "людської помилки".
     *
     * @param gameState   Поточний стан дошки.
     * @param probability Ймовірність (від 0.0 до 1.0) зробити "розумний" хід.
     * @return Індекс клітинки для ходу.
     */
    private int getSmartRandomMove(int[] gameState, double probability) {
        if (new Random().nextDouble() < probability) {
            int best = getBestMoveSimple(gameState);
            if (best != -1) return best;
        }
        return getRandomMove(gameState);
    }

    /**
     * Проста евристична логіка (Рівень 4).
     * 1. Перевіряє, чи може бот виграти цим ходом. Якщо так - робить хід.
     * 2. Перевіряє, чи може гравець виграти наступним ходом. Якщо так - блокує його.
     * 3. Якщо прямих загроз або можливостей немає - робить випадковий хід.
     *
     * @param gameState Поточний стан дошки.
     * @return Індекс клітинки для ходу.
     */
    private int getBestMoveSimple(int[] gameState) {
        // 1. Перевіряємо, чи може бот (2) виграти наступним ходом
        for (int i = 0; i < 9; i++) {
            if (gameState[i] == 0) {
                gameState[i] = 2; // Робимо пробний хід
                if (isWinningState(gameState, 2)) {
                    gameState[i] = 0; // Скасовуємо пробний хід
                    return i;
                }
                gameState[i] = 0;
            }
        }

        // 2. Перевіряємо, чи може гравець (1) виграти наступним ходом, і блокуємо його
        for (int i = 0; i < 9; i++) {
            if (gameState[i] == 0) {
                gameState[i] = 1; // Симулюємо хід гравця
                if (isWinningState(gameState, 1)) {
                    gameState[i] = 0; // Скасовуємо симуляцію
                    return i; // Повертаємо цю клітинку, щоб заблокувати
                }
                gameState[i] = 0;
            }
        }

        // 3. Якщо ніхто не виграє відразу, робимо випадковий хід
        return getRandomMove(gameState);
    }

    /**
     * Точка входу для алгоритму Minimax (Рівень 5).
     * Прораховує всі можливі гілки розвитку гри і знаходить математично ідеальний хід,
     * який гарантує перемогу або принаймні нічию.
     *
     * @param gameState Поточний стан дошки.
     * @return Оптимальний індекс клітинки для ходу.
     */
    private int getBestMoveMinimax(int[] gameState) {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;

        for (int i = 0; i < 9; i++) {
            if (gameState[i] == 0) {
                gameState[i] = 2; // Робимо пробний хід за бота
                // Викликаємо minimax для оцінки цього ходу (наступним ходить гравець - false)
                int score = minimax(gameState, false);
                gameState[i] = 0; // Скасовуємо хід

                // Обираємо хід, який дає найвищу оцінку
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = i;
                }
            }
        }
        return bestMove;
    }

    /**
     * Рекурсивна функція алгоритму Minimax.
     * Симулює гру до самого кінця (перемога, поразка, нічия) для кожної можливої гілки подій.
     *
     * @param gameState    Поточний стан дошки в симуляції.
     * @param isMaximizing true - хід бота (максимізує оцінку), false - хід людини (мінімізує оцінку).
     * @return Оцінка стану дошки: +10 (перемога бота), -10 (перемога людини), 0 (нічия).
     */
    private int minimax(int[] gameState, boolean isMaximizing) {
        // Базові випадки (термінальні стани)
        if (isWinningState(gameState, 2)) return 10;  // Бот виграв
        if (isWinningState(gameState, 1)) return -10; // Гравець виграв

        // Перевірка на нічию (немає вільних клітинок)
        boolean isDraw = true;
        for (int state : gameState) {
            if (state == 0) { isDraw = false; break; }
        }
        if (isDraw) return 0;

        // Рекурсивний розрахунок
        if (isMaximizing) {
            // Симулюємо найкращий хід для бота
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 9; i++) {
                if (gameState[i] == 0) {
                    gameState[i] = 2;
                    int score = minimax(gameState, false);
                    gameState[i] = 0;
                    bestScore = Math.max(score, bestScore);
                }
            }
            return bestScore;
        } else {
            // Симулюємо найкращий хід для гравця (людина намагається зробити рахунок найменшим)
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < 9; i++) {
                if (gameState[i] == 0) {
                    gameState[i] = 1;
                    int score = minimax(gameState, true);
                    gameState[i] = 0;
                    bestScore = Math.min(score, bestScore);
                }
            }
            return bestScore;
        }
    }

    /**
     * Допоміжний метод для перевірки, чи зібрав вказаний гравець виграшну комбінацію.
     *
     * @param gameState Поточний масив стану дошки.
     * @param player    Ідентифікатор гравця (1 - людина, 2 - бот).
     * @return true, якщо гравець переміг, інакше false.
     */
    private boolean isWinningState(int[] gameState, int player) {
        for (int[] win : winPositions) {
            if (gameState[win[0]] == player && gameState[win[1]] == player && gameState[win[2]] == player) {
                return true;
            }
        }
        return false;
    }
}