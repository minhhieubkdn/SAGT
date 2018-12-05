package imwi.sskylight;

import android.graphics.Color;

public class TreePositions {
    public Plants plant;
    public int position;
    public boolean isChoosing;
    public Color textColor;
    public int hum;
    TreePositions() {
        plant = new Plants();
        position = 0;
        isChoosing = false;
        hum = 0;
    }
    public int getPosition() {
        return position;
    }
    public Plants getPlant() {
        return this.plant;
    }

    public void setPlant(Plants plant) {
        this.plant = plant;
    }
    public void setPosition(int _pos) {
        this.position = _pos;
    }
}
