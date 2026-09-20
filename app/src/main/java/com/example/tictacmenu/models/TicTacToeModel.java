package com.example.tictacmenu.models;

/** Keeps the game rules independent from any Activity or view. */
public final class TicTacToeModel {
    private final String[][] board = new String[3][3];
    private String currentPlayer;

    public TicTacToeModel() {
        resetGame();
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public String getCell(int row, int col) {
        return board[row][col];
    }

    public boolean isLegal(int row, int col) {
        return row >= 0 && row < 3 && col >= 0 && col < 3 && board[row][col].isEmpty();
    }

    public boolean makeMove(int row, int col) {
        if (!isLegal(row, col)) {
            return false;
        }
        board[row][col] = currentPlayer;
        return true;
    }

    /** Applies a remote move while preserving the normal legal-move guard. */
    public boolean setMove(int row, int col, String player) {
        if (!isLegal(row, col) || (!"X".equals(player) && !"O".equals(player))) {
            return false;
        }
        board[row][col] = player;
        return true;
    }

    public void changePlayer() {
        currentPlayer = "X".equals(currentPlayer) ? "O" : "X";
    }

    public boolean checkWin() {
        for (int index = 0; index < 3; index++) {
            if (sameMark(board[index][0], board[index][1], board[index][2])
                    || sameMark(board[0][index], board[1][index], board[2][index])) {
                return true;
            }
        }
        return sameMark(board[0][0], board[1][1], board[2][2])
                || sameMark(board[0][2], board[1][1], board[2][0]);
    }

    public boolean isTie() {
        if (checkWin()) {
            return false;
        }
        for (String[] row : board) {
            for (String cell : row) {
                if (cell.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void resetGame() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                board[row][col] = "";
            }
        }
        currentPlayer = "X";
    }

    private boolean sameMark(String first, String second, String third) {
        return !first.isEmpty() && first.equals(second) && second.equals(third);
    }
}
