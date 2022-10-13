package domain;

import java.io.Serializable;

public class Power implements Serializable {
    private double totalpower;
    private int unixtimestamp;

    public Power() {
    }

    public Power(double totalpower, int unixtimestamp) {
        this.totalpower = totalpower;
        this.unixtimestamp = unixtimestamp;
    }

    public double getTotalpower() {
        return totalpower;
    }

    public void setTotalpower(double totalpower) {
        this.totalpower = totalpower;
    }

    public int getUnixtimestamp() {
        return unixtimestamp;
    }

    public void setUnixtimestamp(int unixtimestamp) {
        this.unixtimestamp = unixtimestamp;
    }

    @Override
    public String toString() {
        return "Power{" +
                "totalpower=" + totalpower +
                ", unixtimestamp=" + unixtimestamp +
                '}';
    }
}
