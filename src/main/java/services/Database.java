package services;

import domain.Power;

import java.io.*;
import java.util.ArrayList;

public class Database {
    private String dbFile = "database.txt";

    public Database(String dbFileName) {
        String ressourcePath = String.valueOf(this.getClass().getClassLoader().getResource("")).substring(6);
        this.dbFile = ressourcePath + dbFileName;
    }

    public void delPower(Power power) {
        ArrayList<Power> powerList = new ArrayList<>();
        for (Power p : read()) {
            if(!(p.getUnixtimestamp()==power.getUnixtimestamp() && p.getTotalpower()==power.getTotalpower())) {
                powerList.add(p);
            }
        }
        write(powerList);
    }

    public void addPower(Power power) {
        ArrayList<Power> powerList = read();
        powerList.add(power);
        write(powerList);
    }
    public void write(ArrayList<Power> powers) {
        try {
            FileOutputStream fop = new FileOutputStream(this.dbFile, false);
            ObjectOutputStream oos = new ObjectOutputStream(fop);
            oos.writeObject(powers);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<Power> read() {
        try {
            FileInputStream fis = new FileInputStream(this.dbFile);
            if (fis.available() > 0) {
                ObjectInputStream ois = new ObjectInputStream(fis);
                return (ArrayList<Power>) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            return new ArrayList<Power>();
        }
        return new ArrayList<Power>();
    }
}
