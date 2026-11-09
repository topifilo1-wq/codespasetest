package com.example.carromaim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.carromaim.physics.CarromPiece;
import com.example.carromaim.physics.PhysicsEngine;
import com.example.carromaim.physics.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class CarromBoardView extends View {

    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aimDirectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aimTargetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aimChainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint aimDeflectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostBallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pocketGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF boardBounds = new RectF();
    private final List<CarromPiece> pieces = new ArrayList<>();
    private CarromPiece striker;
    private final List<Vector2D> pockets = new ArrayList<>();
    private float pocketRadius = 38f;

    private Vector2D aimDirection = new Vector2D(0, -1);
    private PhysicsEngine.SimulationResult currentSim;

    private CarromPiece draggedPiece = null;
    private boolean isAiming = false;
    private boolean chainEnabled = true;

    public interface OnAimListener {
        void onPocketTarget(boolean isTargetingPocket);
    }
    private OnAimListener aimListener;

    public CarromBoardView(Context context) {
        super(context);
        init();
    }

    public CarromBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boardPaint.setColor(0xFFF0D5A3); // Natural carrom wood color
        boardPaint.setStyle(Paint.Style.FILL);

        framePaint.setColor(0xFF4A2511); // Dark rosewood frame
        framePaint.setStyle(Paint.Style.STROKE);

        pocketPaint.setColor(0xFF1A1A1A);
        pocketPaint.setStyle(Paint.Style.FILL);

        pocketGlowPaint.setColor(0x884CAF50);
        pocketGlowPaint.setStyle(Paint.Style.STROKE);
        pocketGlowPaint.setStrokeWidth(8f);

        linePaint.setColor(0xFF8B4513);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);

        piecePaint.setStyle(Paint.Style.FILL);
        pieceBorderPaint.setStyle(Paint.Style.STROKE);
        pieceBorderPaint.setColor(0xFF222222);
        pieceBorderPaint.setStrokeWidth(2f);

        // Trajectory paints
        aimDirectPaint.setColor(0xFFFFFFFF);
        aimDirectPaint.setStyle(Paint.Style.STROKE);
        aimDirectPaint.setStrokeWidth(5f);

        aimTargetPaint.setColor(0xFF00E676); // Bright green for direct target
        aimTargetPaint.setStyle(Paint.Style.STROKE);
        aimTargetPaint.setStrokeWidth(5f);

        aimChainPaint.setColor(0xFFFFEB3B); // Yellow for chain combo
        aimChainPaint.setStyle(Paint.Style.STROKE);
        aimChainPaint.setStrokeWidth(5f);
        aimChainPaint.setPathEffect(new DashPathEffect(new float[]{15f, 10f}, 0));

        aimDeflectPaint.setColor(0xFF00B0FF); // Cyan for striker deflection
        aimDeflectPaint.setStyle(Paint.Style.STROKE);
        aimDeflectPaint.setStrokeWidth(4f);
        aimDeflectPaint.setPathEffect(new DashPathEffect(new float[]{10f, 8f}, 0));

        ghostBallPaint.setStyle(Paint.Style.STROKE);
        ghostBallPaint.setStrokeWidth(3f);
    }

    public void setOnAimListener(OnAimListener listener) {
        this.aimListener = listener;
    }

    public void setChainEnabled(boolean enabled) {
        this.chainEnabled = enabled;
        recalculateTrajectory();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float size = Math.min(w, h);
        float left = (w - size) / 2f + 25f;
        float top = (h - size) / 2f + 25f;
        float right = left + size - 50f;
        float bottom = top + size - 50f;

        boardBounds.set(left, top, right, bottom);
        framePaint.setStrokeWidth(25f);

        pocketRadius = size * 0.045f;
        pockets.clear();
        pockets.add(new Vector2D(boardBounds.left + pocketRadius, boardBounds.top + pocketRadius));
        pockets.add(new Vector2D(boardBounds.right - pocketRadius, boardBounds.top + pocketRadius));
        pockets.add(new Vector2D(boardBounds.left + pocketRadius, boardBounds.bottom - pocketRadius));
        pockets.add(new Vector2D(boardBounds.right - pocketRadius, boardBounds.bottom - pocketRadius));

        resetBoard();
    }

    public void resetBoard() {
        pieces.clear();
        float coinRadius = (boardBounds.width()) * 0.032f;
        float strikerRadius = coinRadius * 1.35f;

        // Striker positioned at bottom baseline
        float strikerY = boardBounds.bottom - (boardBounds.height() * 0.18f);
        float strikerX = boardBounds.centerX();
        striker = new CarromPiece(0, strikerX, strikerY, strikerRadius, 0xFFFFF9C4, true);

        // Center coins
        float cx = boardBounds.centerX();
        float cy = boardBounds.centerY();

        // Queen (Red) in center
        pieces.add(new CarromPiece(1, cx, cy, coinRadius, 0xFFD32F2F, false));

        // White and Black carrom men around Queen
        float dist = coinRadius * 2.05f;
        pieces.add(new CarromPiece(2, cx - dist, cy - 20, coinRadius, 0xFFFFFFFF, false));
        pieces.add(new CarromPiece(3, cx + dist * 0.5f, cy - dist * 0.866f, coinRadius, 0xFF212121, false));
        pieces.add(new CarromPiece(4, cx + dist * 0.8f, cy + dist * 0.6f, coinRadius, 0xFFFFFFFF, false));
        pieces.add(new CarromPiece(5, cx - dist * 0.6f, cy + dist * 1.2f, coinRadius, 0xFF212121, false));
        pieces.add(new CarromPiece(6, cx + 40, cy - dist * 1.5f, coinRadius, 0xFFFFFFFF, false));

        aimDirection = new Vector2D(0, -1);
        recalculateTrajectory();
        invalidate();
    }

    public void addPiece(int color) {
        float coinRadius = (boardBounds.width()) * 0.032f;
        int nextId = pieces.size() + 10;
        float rx = boardBounds.centerX() + (float)(Math.random() * 200 - 100);
        float ry = boardBounds.centerY() + (float)(Math.random() * 200 - 100);
        pieces.add(new CarromPiece(nextId, rx, ry, coinRadius, color, false));
        recalculateTrajectory();
        invalidate();
    }

    private void recalculateTrajectory() {
        if (striker == null) return;
        currentSim = PhysicsEngine.simulate(striker, aimDirection, pieces, boardBounds, pockets, pocketRadius);
        if (aimListener != null) {
            aimListener.onPocketTarget(currentSim != null && currentSim.pocketTargetDetected);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw carrom board surface
        canvas.drawRect(boardBounds, boardPaint);

        // 2. Draw border frame
        canvas.drawRect(boardBounds, framePaint);

        // 3. Draw board patterns (Center circle & baselines)
        float cx = boardBounds.centerX();
        float cy = boardBounds.centerY();
        canvas.drawCircle(cx, cy, boardBounds.width() * 0.12f, linePaint);
        canvas.drawCircle(cx, cy, boardBounds.width() * 0.04f, linePaint);

        // Bottom baseline
        float baselineY = boardBounds.bottom - (boardBounds.height() * 0.18f);
        float baselineMargin = boardBounds.width() * 0.15f;
        canvas.drawLine(boardBounds.left + baselineMargin, baselineY, boardBounds.right - baselineMargin, baselineY, linePaint);
        canvas.drawCircle(boardBounds.left + baselineMargin, baselineY, 14f, linePaint);
        canvas.drawCircle(boardBounds.right - baselineMargin, baselineY, 14f, linePaint);

        // 4. Draw pockets
        for (Vector2D pocket : pockets) {
            canvas.drawCircle(pocket.x, pocket.y, pocketRadius, pocketPaint);
            if (currentSim != null && currentSim.pocketTargetDetected) {
                canvas.drawCircle(pocket.x, pocket.y, pocketRadius + 4f, pocketGlowPaint);
            }
        }

        // 5. Draw trajectory lines
        if (currentSim != null) {
            for (PhysicsEngine.TrajectoryLine line : currentSim.lines) {
                Paint p;
                if (line.lineType == 0) p = aimDirectPaint;
                else if (line.lineType == 1) p = aimTargetPaint;
                else if (line.lineType == 2) {
                    if (!chainEnabled) continue;
                    p = aimChainPaint;
                } else if (line.lineType == 3) p = aimDeflectPaint;
                else p = aimTargetPaint;

                canvas.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, p);
            }

            // Draw ghost balls
            for (PhysicsEngine.GhostBall gb : currentSim.ghostBalls) {
                ghostBallPaint.setColor(gb.color);
                canvas.drawCircle(gb.position.x, gb.position.y, gb.radius, ghostBallPaint);
            }
        }

        // 6. Draw coins
        for (CarromPiece piece : pieces) {
            piecePaint.setColor(piece.color);
            canvas.drawCircle(piece.position.x, piece.position.y, piece.radius, piecePaint);
            canvas.drawCircle(piece.position.x, piece.position.y, piece.radius, pieceBorderPaint);
        }

        // 7. Draw striker
        if (striker != null) {
            piecePaint.setColor(striker.color);
            canvas.drawCircle(striker.position.x, striker.position.y, striker.radius, piecePaint);
            canvas.drawCircle(striker.position.x, striker.position.y, striker.radius, pieceBorderPaint);

            // Draw striker center inner ring
            canvas.drawCircle(striker.position.x, striker.position.y, striker.radius * 0.45f, pieceBorderPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if user touched striker
                if (striker != null && Math.hypot(x - striker.position.x, y - striker.position.y) < striker.radius * 1.5f) {
                    draggedPiece = striker;
                    return true;
                }
                // Check if user touched another piece to reposition
                for (CarromPiece p : pieces) {
                    if (Math.hypot(x - p.position.x, y - p.position.y) < p.radius * 1.5f) {
                        draggedPiece = p;
                        return true;
                    }
                }
                // Otherwise user is aiming
                isAiming = true;
                updateAim(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (draggedPiece != null) {
                    if (draggedPiece.isStriker) {
                        // Keep striker along baseline
                        float baselineMargin = boardBounds.width() * 0.15f;
                        float minX = boardBounds.left + baselineMargin;
                        float maxX = boardBounds.right - baselineMargin;
                        draggedPiece.position.x = Math.max(minX, Math.min(maxX, x));
                    } else {
                        // Free move piece inside board
                        draggedPiece.position.x = Math.max(boardBounds.left + draggedPiece.radius, Math.min(boardBounds.right - draggedPiece.radius, x));
                        draggedPiece.position.y = Math.max(boardBounds.top + draggedPiece.radius, Math.min(boardBounds.bottom - draggedPiece.radius, y));
                    }
                    recalculateTrajectory();
                    invalidate();
                } else if (isAiming) {
                    updateAim(x, y);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggedPiece = null;
                isAiming = false;
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void updateAim(float touchX, float touchY) {
        if (striker == null) return;
        Vector2D dir = new Vector2D(touchX - striker.position.x, touchY - striker.position.y);
        if (dir.length() > 5f) {
            aimDirection = dir.normalize();
            recalculateTrajectory();
            invalidate();
        }
    }
}
