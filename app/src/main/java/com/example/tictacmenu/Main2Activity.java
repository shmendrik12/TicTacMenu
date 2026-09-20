package com.example.tictacmenu;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

/** A two-device Tic-Tac-Toe board synchronized through Firebase Realtime Database. */
public class Main2Activity extends BaseGameActivity {
    private static final String DATABASE_URL =
            "https://tictacmenu-78a8b-default-rtdb.firebaseio.com/";
    private static final String GAME_PATH = "tictactoe";

    private DatabaseReference gameRef;
    private ValueEventListener gameListener;
    private boolean gameReady;

    @Override
    protected String gameModeLabel() {
        return "RTDB game · synced via 'tictactoe'";
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameRef = FirebaseDatabase.getInstance(DATABASE_URL).getReference(GAME_PATH);
        findViewById(R.id.button_new_game).setOnClickListener(view -> resetFirebaseState());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            gameStatus.setText("Sign in before opening a shared game.");
            Toast.makeText(this, "Firebase sign-in is required for the shared game.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        setupFirebaseListener();
    }

    private void setupFirebaseListener() {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    gameReady = false;
                    gameStatus.setText("Preparing shared game…");
                    ensureGameExists();
                    return;
                }

                gameReady = true;

                String firebasePlayer = snapshot.child("currentPlayer").getValue(String.class);
                Boolean firebaseFinished = snapshot.child("gameFinished").getValue(Boolean.class);

                model.resetGame();
                for (int id : BUTTON_IDS) {
                    Button button = findViewById(id);
                    String[] position = button.getTag().toString().split(",");
                    int r = Integer.parseInt(position[0]);
                    int c = Integer.parseInt(position[1]);

                    String cellVal = snapshot.child("board").child(r + "_" + c).getValue(String.class);
                    if (cellVal != null && !cellVal.isEmpty()) {
                        model.setMove(r, c, cellVal);
                        button.setText(cellVal);
                        button.setTextColor(ContextCompat.getColor(Main2Activity.this,
                                "X".equals(cellVal) ? R.color.mark_first : R.color.mark_second));
                    } else {
                        button.setText("");
                    }
                }

                if ("O".equals(firebasePlayer)) {
                    model.changePlayer();
                }
                gameFinished = Boolean.TRUE.equals(firebaseFinished);

                if (model.checkWin()) {
                    gameFinished = true;
                    // Find out who won by checking the board marks
                    String winner = findWinner();
                    gameStatus.setText("Player " + winner + " wins!");
                } else if (model.isTie()) {
                    gameFinished = true;
                    gameStatus.setText("It is a tie!");
                } else {
                    updateStatus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                gameReady = false;
                gameStatus.setText("Firebase access was denied.");
                Toast.makeText(Main2Activity.this, permissionMessage(error), Toast.LENGTH_LONG).show();
            }
        };
        gameRef.addValueEventListener(gameListener);
    }

    private String findWinner() {
        if (checkLine(0, 0, 0, 1, 0, 2)) return model.getCell(0, 0);
        if (checkLine(1, 0, 1, 1, 1, 2)) return model.getCell(1, 0);
        if (checkLine(2, 0, 2, 1, 2, 2)) return model.getCell(2, 0);
        if (checkLine(0, 0, 1, 0, 2, 0)) return model.getCell(0, 0);
        if (checkLine(0, 1, 1, 1, 2, 1)) return model.getCell(0, 1);
        if (checkLine(0, 2, 1, 2, 2, 2)) return model.getCell(0, 2);
        if (checkLine(0, 0, 1, 1, 2, 2)) return model.getCell(0, 0);
        if (checkLine(0, 2, 1, 1, 2, 0)) return model.getCell(0, 2);
        return "";
    }

    private boolean checkLine(int r1, int c1, int r2, int c2, int r3, int c3) {
        String first = model.getCell(r1, c1);
        return !first.isEmpty() && first.equals(model.getCell(r2, c2)) && first.equals(model.getCell(r3, c3));
    }

    @Override
    public void onCellClick(View view) {
        if (gameFinished) {
            return;
        }
        if (!gameReady) {
            Toast.makeText(this, "Waiting for the shared game to load.", Toast.LENGTH_SHORT).show();
            return;
        }

        Button button = (Button) view;
        String[] position = button.getTag().toString().split(",");
        int row = Integer.parseInt(position[0]);
        int col = Integer.parseInt(position[1]);
        if (!model.isLegal(row, col)) {
            return;
        }

        submitMove(row, col);
    }

    private void resetFirebaseState() {
        if (!gameReady) {
            Toast.makeText(this, "The shared game is not available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        gameRef.setValue(newGameState()).addOnFailureListener(error ->
                showSyncError("Could not reset the game", error));
    }

    /** Creates the shared board once, without overwriting a game another player already started. */
    private void ensureGameExists() {
        gameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                if (currentData.getValue() == null) {
                    currentData.setValue(newGameState());
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (error != null) {
                    showSyncError("Could not create the shared game", error.toException());
                }
            }
        });
    }

    /**
     * A transaction prevents two devices from taking the same square or both moving for one turn.
     * The listener redraws only after Firebase commits the winning state.
     */
    private void submitMove(int row, int col) {
        gameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Object rawGame = currentData.getValue();
                if (!(rawGame instanceof Map)) {
                    return Transaction.abort();
                }

                Map<String, Object> game = new HashMap<>((Map<String, Object>) rawGame);
                if (Boolean.TRUE.equals(game.get("gameFinished"))) {
                    return Transaction.abort();
                }

                Map<String, Object> board = boardFrom(game);
                String key = cellKey(row, col);
                String existingMark = stringValue(board.get(key));
                if (!existingMark.isEmpty()) {
                    return Transaction.abort();
                }

                String player = "O".equals(game.get("currentPlayer")) ? "O" : "X";
                board.put(key, player);
                game.put("board", board);

                if (hasWinner(board, player)) {
                    game.put("gameFinished", true);
                    game.put("winner", player);
                } else if (isTie(board)) {
                    game.put("gameFinished", true);
                    game.put("winner", "");
                } else {
                    game.put("currentPlayer", "X".equals(player) ? "O" : "X");
                }
                currentData.setValue(game);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (error != null) {
                    showSyncError("Move was not saved", error.toException());
                } else if (!committed) {
                    Toast.makeText(Main2Activity.this, "That square is no longer available.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private Map<String, Object> newGameState() {
        Map<String, Object> board = new HashMap<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                board.put(cellKey(row, col), "");
            }
        }
        Map<String, Object> game = new HashMap<>();
        game.put("board", board);
        game.put("currentPlayer", "X");
        game.put("gameFinished", false);
        game.put("winner", "");
        return game;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> boardFrom(Map<String, Object> game) {
        Object rawBoard = game.get("board");
        return rawBoard instanceof Map
                ? new HashMap<>((Map<String, Object>) rawBoard)
                : new HashMap<>();
    }

    private boolean hasWinner(Map<String, Object> board, String player) {
        return hasLine(board, player, 0, 0, 0, 1, 0, 2)
                || hasLine(board, player, 1, 0, 1, 1, 1, 2)
                || hasLine(board, player, 2, 0, 2, 1, 2, 2)
                || hasLine(board, player, 0, 0, 1, 0, 2, 0)
                || hasLine(board, player, 0, 1, 1, 1, 2, 1)
                || hasLine(board, player, 0, 2, 1, 2, 2, 2)
                || hasLine(board, player, 0, 0, 1, 1, 2, 2)
                || hasLine(board, player, 0, 2, 1, 1, 2, 0);
    }

    private boolean hasLine(Map<String, Object> board, String player, int r1, int c1,
                            int r2, int c2, int r3, int c3) {
        return player.equals(stringValue(board.get(cellKey(r1, c1))))
                && player.equals(stringValue(board.get(cellKey(r2, c2))))
                && player.equals(stringValue(board.get(cellKey(r3, c3))));
    }

    private boolean isTie(Map<String, Object> board) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (stringValue(board.get(cellKey(row, col))).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String cellKey(int row, int col) {
        return row + "_" + col;
    }

    private String stringValue(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private void showSyncError(String prefix, Exception error) {
        String message = error.getLocalizedMessage();
        Toast.makeText(this, prefix + (message == null ? "." : ": " + message),
                Toast.LENGTH_LONG).show();
    }

    private String permissionMessage(DatabaseError error) {
        if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
            return "Firebase denied access. Allow authenticated read and write at /tictactoe in RTDB Rules.";
        }
        return "Sync error: " + error.getMessage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameRef != null && gameListener != null) {
            gameRef.removeEventListener(gameListener);
        }
    }
}
