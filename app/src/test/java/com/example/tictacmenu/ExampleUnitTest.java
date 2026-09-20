package com.example.tictacmenu;

import com.example.tictacmenu.models.TicTacToeModel;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Core rules tests run on the development machine without an Android device.
 */
public class ExampleUnitTest {
    @Test
    public void detectsAWinningRow() {
        TicTacToeModel model = new TicTacToeModel();

        model.makeMove(0, 0); // X
        model.changePlayer();
        model.makeMove(1, 0); // O
        model.changePlayer();
        model.makeMove(0, 1); // X
        model.changePlayer();
        model.makeMove(1, 1); // O
        model.changePlayer();
        model.makeMove(0, 2); // X

        assertTrue(model.checkWin());
        assertFalse(model.isTie());
    }

    @Test
    public void detectsATieWithoutAWin() {
        TicTacToeModel model = new TicTacToeModel();
        String[][] moves = {
                {"X", "O", "X"},
                {"X", "O", "O"},
                {"O", "X", "X"}
        };

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                assertTrue(model.setMove(row, col, moves[row][col]));
            }
        }

        assertFalse(model.checkWin());
        assertTrue(model.isTie());
    }
}
