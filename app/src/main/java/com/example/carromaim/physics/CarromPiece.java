package com.example.carromaim.physics;

public class CarromPiece {
    public int id;
    public Vector2D position;
    public float radius;
    public int color;
    public boolean isStriker;

    public CarromPiece(int id, float x, float y, float radius, int color, boolean isStriker) {
        this.id = id;
        this.position = new Vector2D(x, y);
        this.radius = radius;
        this.color = color;
        this.isStriker = isStriker;
    }

    public CarromPiece copy() {
        return new CarromPiece(id, position.x, position.y, radius, color, isStriker);
    }
}
