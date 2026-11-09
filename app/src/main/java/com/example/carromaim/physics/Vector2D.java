package com.example.carromaim.physics;

public class Vector2D {
    public float x;
    public float y;

    public Vector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(Vector2D other) {
        this.x = other.x;
        this.y = other.y;
    }

    public Vector2D add(Vector2D v) {
        return new Vector2D(this.x + v.x, this.y + v.y);
    }

    public Vector2D sub(Vector2D v) {
        return new Vector2D(this.x - v.x, this.y - v.y);
    }

    public Vector2D scale(float s) {
        return new Vector2D(this.x * s, this.y * s);
    }

    public float dot(Vector2D v) {
        return this.x * v.x + this.y * v.y;
    }

    public float length() {
        return (float) Math.hypot(x, y);
    }

    public Vector2D normalize() {
        float len = length();
        if (len > 0.00001f) {
            return new Vector2D(this.x / len, this.y / len);
        }
        return new Vector2D(0, 0);
    }

    public float distanceTo(Vector2D other) {
        return (float) Math.hypot(this.x - other.x, this.y - other.y);
    }
}
