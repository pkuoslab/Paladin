package com.sei.bean.View;
/**
 * Created by vector on 16/6/7.
 */
public class Position {
    float x;
    float y;

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }


    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Position() {
    }

    @Override
    public boolean equals(Object o) {
        Position target = (Position) o;
        return this.x == target.getX() && this.y == target.getY();
    }
}
