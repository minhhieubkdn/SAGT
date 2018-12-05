package imwi.sskylight;

import android.graphics.Color;

public class Plants {
    public String name;
    public int hour;
    public int minute;
    public int intervalToMeasure;
    public int waterDuration;
    public int humidity;
    public boolean isChoosing;
    public int id;
    public int minHumidity;

    public String getName() {
        return this.name;
    }

    public Plants() {
        id = 0;
        setName("Xà lách");
        setTimeToWater("07:30");
        setHumidity(0);
        setTimeToMeasure(4);  //min
        setWaterDuration(5);  //sec
        isChoosing = false;
        setMinHumidity(20);   //%
    }

    public Plants Clone() {
        Plants _pl = new Plants();
        _pl.name = this.name;
        _pl.hour = this.hour;
        _pl.minute = this.minute;
        _pl.intervalToMeasure = this.intervalToMeasure;
        _pl.waterDuration = this.waterDuration;
        _pl.humidity = this.humidity;
        _pl.id = this.id;
        _pl.minHumidity = this.minHumidity;
        return _pl;
    }


    public int getTimeToMeasure() {
        return this.intervalToMeasure;
    }

    public int getWaterDuration() {
        return this.waterDuration;
    }

    public int getHumidity() {
        return this.humidity;
    }

    public String getTimeToWater() {
        String time;
        time = Integer.toString(hour) + ":" + Integer.toString(minute);
        return time;
    }

    public void setTimeToWater(String _time) {
        String strArr[] = _time.split(":");
        setHour(Integer.parseInt(strArr[0]));
        setMinute(Integer.parseInt(strArr[1]));
    }

    public int getMinHumidity() {
        return minHumidity;
    }

    public void setMinHumidity(int minHumidity) {
        this.minHumidity = minHumidity;
    }

    public void setId(int _id) {
        this.id = _id;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setName(String _name) {
        this.name = _name;
    }

    public void setTimeToMeasure(int _time) {
        this.intervalToMeasure = _time;
    }

    public void setWaterDuration(int minutes) {
        this.waterDuration = minutes;
    }

    public void setHumidity(int _hum) {
        this.humidity = _hum;
    }
}
