package com.example.tic_tac_toe_wifi;

import java.util.ArrayList;

public class TicTacToeBot {

    private final int[][] winPositions = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };


    private int getBestMove( int[] gameState, int aiLevel) {
        switch (aiLevel) {
            case 1: return getRandomMove(gameState);
            case 2: return getSmartRandomMove(gameState, 0.3);
            case 3: return getSmartRandomMove(gameState, 0.6);
            case 4: return 1;
            case 5: return 1;
            default:return 1;
        }
    }


    private int getRandomMove(int[] gameState) {
        ArrayList<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if(gameState[i] == 0) emptySlots.add(i);
        }
        if (emptySlots.isEmpty()) return -1;

        return emptySlots.get(new Random().nextInt(emptySlots.size()));
    }

    /**
     * Комбінований підхід: з певною ймовірністю робить логічний хід,
     * інакше робить випадковий хід.
     *
     * ЗАВДАННЯ:
     * 1. Згенеруйте випадкове число від 0.0 до 1.0.
     * 2. Якщо воно менше за probability — викличте getBestMoveSimple().
     * 3. Якщо getBestMoveSimple() повернув -1 або ймовірність не спрацювала — викличте getRandomMove().
     */
    private int getSmartRandomMove(int[] gameState, double probability) {
        if (new Random().nextDouble() < probability) {
            int best = 1; // TODO кращий хід ?
            if (best != -1) return best;
        }
        return getRandomMove(gameState);
    }

    private int getBestMoveSimple(int[] gameState) {
        for (int i = 0; i < 9; i++) {
            if (gameState[i] == 0) {
                gameState[i] = 2;
                if(isWinningState(gameState, 2)) {
                    gameState[i] = 0
                    return i;
                }
                gameState[i] = 0;
            }
        }
        return
    }


    private boolean isWinningState(int[] gameState, int player) {
        for(int[] win: winPositions) {
            if(gameState[win[0]] == player &&
                gameState[win[1]] == player &&
                gameState[win[2]] == player){
                return true;
            }
        }
    }

    /**
     * Проста евристична логіка (рівень 4).
     *
     * ЗАВДАННЯ (виконуйте строго в цьому порядку!):
     * 1. Перевірте, чи може бот (2) виграти цим ходом:
     *    - Для кожної пустої клітинки:
     *      • Тимчасово поставте gameState[i] = 2
     *      • Якщо isWinningState(gameState, 2) == true → скасуйте хід і поверніть i
     *      • Скасуйте хід (gameState[i] = 0)
     * 2. Якщо перемоги бота немає — перевірте, чи може гравець (1) виграти наступним ходом:
     *    - Для кожної пустої клітинки:
     *      • Тимчасово поставте gameState[i] = 1
     *      • Якщо isWinningState(gameState, 1) == true → скасуйте і поверніть i (блокування)
     *      • Скасуйте хід
     * 3. Якщо жодної загрози/можливості немає — поверніть getRandomMove(gameState).
     */

    /**
     * Точка входу для алгоритму Minimax (рівень 5).
     *
     * ЗАВДАННЯ:
     * 1. Ініціалізуйте bestScore = Integer.MIN_VALUE та bestMove = -1.
     * 2. Для кожної пустої клітинки:
     *    • Тимчасово поставте gameState[i] = 2
     *    • Викличте score = minimax(gameState, false)
     *    • Скасуйте хід
     *    • Якщо score > bestScore — оновіть bestScore і bestMove
     * 3. Поверніть bestMove.
     */

    /**
     * Рекурсивна функція алгоритму Minimax.
     *
     * ЗАВДАННЯ:
     * 1. Перевірте базові випадки:
     *    • Якщо isWinningState(gameState, 2) — поверніть 10
     *    • Якщо isWinningState(gameState, 1) — поверніть -10
     *    • Якщо немає вільних клітинок — поверніть 0 (нічия)
     * 2. Якщо isMaximizing == true (хід бота):
     *    • Переберіть усі пусті клітинки, поставте 2, рекурсивно викличте minimax(false),
     *      обирайте максимальне значення.
     * 3. Якщо isMaximizing == false (хід людини):
     *    • Переберіть усі пусті клітинки, поставте 1, рекурсивно викличте minimax(true),
     *      обирайте мінімальне значення.
     */

    /**
     * Допоміжний метод для перевірки перемоги.
     *
     * ЗАВДАННЯ:
     * 1. Пройдіться по всіх winPositions.
     * 2. Для кожної комбінації перевірте, чи всі три клітинки належать вказаному player.
     * 3. Якщо хоча б одна комбінація заповнена — поверніть true.
     */

}