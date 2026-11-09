package com.example.carromaim;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private CarromBoardView carromBoardView;
    private TextView tvPocketStatus;
    private SwitchMaterial switchChain;
    private Button btnReset;
    private Button btnAddCoin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        carromBoardView = findViewById(R.id.carromBoardView);
        tvPocketStatus = findViewById(R.id.tvPocketStatus);
        switchChain = findViewById(R.id.switchChain);
        btnReset = findViewById(R.id.btnReset);
        btnAddCoin = findViewById(R.id.btnAddCoin);

        // Listener for real-time pocket targeting status
        carromBoardView.setOnAimListener(new CarromBoardView.OnAimListener() {
            @Override
            public void onPocketTarget(boolean isTargetingPocket) {
                if (isTargetingPocket) {
                    tvPocketStatus.setText("POCKET IN-LINE ★");
                    tvPocketStatus.setBackgroundColor(Color.parseColor("#2E7D32")); // Green
                } else {
                    tvPocketStatus.setText("AIMING");
                    tvPocketStatus.setBackgroundColor(Color.parseColor("#455A64")); // Blue Grey
                }
            }
        });

        // Toggle Chain / Multi-ball simulation
        switchChain.setOnCheckedChangeListener((buttonView, isChecked) -> {
            carromBoardView.setChainEnabled(isChecked);
        });

        // Reset board pieces to initial standard positions
        btnReset.setOnClickListener(v -> {
            carromBoardView.resetBoard();
        });

        // Add additional coins dynamically for testing combo angles
        btnAddCoin.setOnClickListener(v -> {
            int randomColor = (Math.random() > 0.5) ? 0xFFFFFFFF : 0xFF212121;
            carromBoardView.addPiece(randomColor);
        });
    }
}
