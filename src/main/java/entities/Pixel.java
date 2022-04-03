package entities;

public class Pixel {
    private int x;
    private int y;
    private int value;

    public Pixel(int x, int y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public Pixel() {
    }

    public void lineToPixel(String text){
        String[] toPixel = text.split(",");
        this.x = Integer.parseInt(toPixel[0]);
        this.y = Integer.parseInt(toPixel[1]);
        this.value = Integer.parseInt(toPixel[2]);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

}

