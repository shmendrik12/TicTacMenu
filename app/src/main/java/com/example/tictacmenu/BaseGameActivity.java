package com.example.tictacmenu;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tictacmenu.models.TicTacToeModel;
import com.google.android.material.appbar.MaterialToolbar;

/** Shared presentation layer for the original game and the later RTDB-prep copy. */
public abstract class BaseGameActivity extends AppCompatActivity {
    protected static final int[] BUTTON_IDS = {
            R.id.button00, R.id.button01, R.id.button02,
            R.id.button10, R.id.button11, R.id.button12,
            R.id.button20, R.id.button21, R.id.button22
    };

    protected TicTacToeModel model;
    protected TextView gameStatus;
    protected boolean gameFinished;

    protected abstract String gameModeLabel();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.game_toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(view -> finish());

        ((TextView) findViewById(R.id.game_mode)).setText(gameModeLabel());
        gameStatus = findViewById(R.id.game_status);
        model = new TicTacToeModel();
        findViewById(R.id.button_new_game).setOnClickListener(view -> resetBoard());
        updateStatus();
    }

    /** Called from each grid Button through android:onClick, exactly as in the course layout. */
    public void onCellClick(View view) {
        if (gameFinished) {
            return;
        }

        Button button = (Button) view;
        String[] position = button.getTag().toString().split(",");
        int row = Integer.parseInt(position[0]);
        int col = Integer.parseInt(position[1]);
        if (!model.isLegal(row, col)) {
            return;
        }

        String player = model.getCurrentPlayer();
        model.makeMove(row, col);
        button.setText(player);
        button.setTextColor(ContextCompat.getColor(this,
                "X".equals(player) ? R.color.mark_first : R.color.mark_second));

        if (model.checkWin()) {
            gameFinished = true;
            gameStatus.setText("Player " + player + " wins!");
            Toast.makeText(this, "Player " + player + " wins!", Toast.LENGTH_SHORT).show();
        } else if (model.isTie()) {
            gameFinished = true;
            gameStatus.setText("It is a tie!");
            Toast.makeText(this, "It is a tie!", Toast.LENGTH_SHORT).show();
        } else {
            model.changePlayer();
            updateStatus();
        }
    }

    protected void resetBoard() {
        model.resetGame();
        gameFinished = false;
        for (int id : BUTTON_IDS) {
            Button button = findViewById(id);
            button.setText("");
            button.setTextColor(ContextCompat.getColor(this, R.color.mark_first));
        }
        updateStatus();
    }

    protected void updateStatus() {
        gameStatus.setText("Player " + model.getCurrentPlayer() + "'s turn");
    }
}
