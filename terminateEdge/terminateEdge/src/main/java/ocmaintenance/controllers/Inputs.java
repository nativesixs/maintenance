package ocmaintenance.controllers;


import java.io.File;
import java.util.ArrayList;

public class Inputs {

    private String ckodField;
    private String simckodField;
    private String dpuckodField;
    private String inventoryField;
    private int mode;
    private String lvmField;
    private String platoField;
    private String placeckodField;
    private String taskField;
    private String lvmField2;
    private String lvmField3;
    private String zdrojField;
    private String ahsField;

    private int device1;
    private int device2;
    private int logset;
    private int delset;
    private String delField;
    private String modIDField;
    private int modField;
    private int sizeField;
    private boolean autoscroll;
    private String vyvodField;
    private String vyvod2Field;
    private String vyvod3Field;
    private String ckodField2;
    private String vazba;
    private String ckodtyp;
    private String ckodtyp2;
    private String lvmVyvodCombo1;
    private String lvmVyvodCombo2;
    private String lvmVyvodCombo3;
    private String lvmVyvodCombo4;
    private String lvmVyvodCombo5;
    private String lvmVyvodCombo6;
    private String lvmVyvodCombo7;
    private String lvmVyvodCombo8;
    private String lvmVyvodCombo9;
    private String lvmVyvodCombo10;

    public String getLvmVyvodCombo1() {
        return lvmVyvodCombo1;
    }

    public void setLvmVyvodCombo1(String lvmVyvodCombo1) {
        this.lvmVyvodCombo1 = lvmVyvodCombo1;
    }

    public String getLvmVyvodCombo2() {
        return lvmVyvodCombo2;
    }

    public void setLvmVyvodCombo2(String lvmVyvodCombo2) {
        this.lvmVyvodCombo2 = lvmVyvodCombo2;
    }

    public String getLvmVyvodCombo3() {
        return lvmVyvodCombo3;
    }

    public void setLvmVyvodCombo3(String lvmVyvodCombo3) {
        this.lvmVyvodCombo3 = lvmVyvodCombo3;
    }

    public String getLvmVyvodCombo4() {
        return lvmVyvodCombo4;
    }

    public void setLvmVyvodCombo4(String lvmVyvodCombo4) {
        this.lvmVyvodCombo4 = lvmVyvodCombo4;
    }

    public String getLvmVyvodCombo5() {
        return lvmVyvodCombo5;
    }

    public void setLvmVyvodCombo5(String lvmVyvodCombo5) {
        this.lvmVyvodCombo5 = lvmVyvodCombo5;
    }

    public String getLvmVyvodCombo6() {
        return lvmVyvodCombo6;
    }

    public void setLvmVyvodCombo6(String lvmVyvodCombo6) {
        this.lvmVyvodCombo6 = lvmVyvodCombo6;
    }

    public String getLvmVyvodCombo7() {
        return lvmVyvodCombo7;
    }

    public void setLvmVyvodCombo7(String lvmVyvodCombo7) {
        this.lvmVyvodCombo7 = lvmVyvodCombo7;
    }

    public String getLvmVyvodCombo8() {
        return lvmVyvodCombo8;
    }

    public void setLvmVyvodCombo8(String lvmVyvodCombo8) {
        this.lvmVyvodCombo8 = lvmVyvodCombo8;
    }

    public String getLvmVyvodCombo9() {
        return lvmVyvodCombo9;
    }

    public void setLvmVyvodCombo9(String lvmVyvodCombo9) {
        this.lvmVyvodCombo9 = lvmVyvodCombo9;
    }

    public String getLvmVyvodCombo10() {
        return lvmVyvodCombo10;
    }

    public void setLvmVyvodCombo10(String lvmVyvodCombo10) {
        this.lvmVyvodCombo10 = lvmVyvodCombo10;
    }

    public String getCkodtyp2() {
        return ckodtyp2;
    }

    public void setCkodtyp2(String ckodtyp2) {
        this.ckodtyp2 = ckodtyp2;
    }

    public String getCkodtyp() {
        return ckodtyp;
    }

    public void setCkodtyp(String ckodtyp) {
        this.ckodtyp = ckodtyp;
    }

    public String getVazba() {
        return vazba;
    }

    public void setVazba(String vazba) {
        this.vazba = vazba;
    }

    public String getCkodField2() {
        return ckodField2;
    }

    public void setCkodField2(String ckodField2) {
        this.ckodField2 = ckodField2;
    }

    public String getVyvod2Field() {
        return vyvod2Field;
    }

    public void setVyvod2Field(String vyvod2Field) {
        this.vyvod2Field = vyvod2Field;
    }

    public String getVyvod3Field() {
        return vyvod3Field;
    }

    public void setVyvod3Field(String vyvod3Field) {
        this.vyvod3Field = vyvod3Field;
    }

    public String getVyvodField() {
        return vyvodField;
    }

    public void setVyvodField(String vyvodField) {
        this.vyvodField = vyvodField;
    }

    public boolean isAutoscroll() {
        return autoscroll;
    }

    public void setAutoscroll(boolean autoscroll) {
        this.autoscroll = autoscroll;
    }

    public int getSizeField() {
        return sizeField;
    }

    public void setSizeField(int sizeField) {
        this.sizeField = sizeField;
    }


    public String getModIDField() {
        return modIDField;
    }

    public void setModIDField(String modIDField) {
        this.modIDField = modIDField;
    }

    public int getModField() {
        return modField;
    }

    public void setModField(int modField) {
        this.modField = modField;
    }

    public String getDelField() {
        return delField;
    }

    public void setDelField(String delField) {
        this.delField = delField;
    }

    public int getDelset() {
        return delset;
    }

    public void setDelset(int delset) {
        this.delset = delset;
    }

    public int getLogset() {
        return logset;
    }

    public void setLogset(int logset) {
        this.logset = logset;
    }

    public int getDevice1() {
        return device1;
    }

    public void setDevice1(int device1) {
        this.device1 = device1;
    }

    public int getDevice2() {
        return device2;
    }

    public void setDevice2(int device2) {
        this.device2 = device2;
    }

    public String getZdrojField() {
        return zdrojField;
    }

    public void setZdrojField(String zdrojField) {
        this.zdrojField = zdrojField;
    }

    public String getAhsField() {
        return ahsField;
    }

    public void setAhsField(String ahsField) {
        this.ahsField = ahsField;
    }

    public String getLvmField2() {
        return lvmField2;
    }

    public void setLvmField2(String lvmField2) {
        this.lvmField2 = lvmField2;
    }

    public String getLvmField3() {
        return lvmField3;
    }

    public void setLvmField3(String lvmField3) {
        this.lvmField3 = lvmField3;
    }

    public String getTaskField() {
        return taskField;
    }

    public void setTaskField(String taskField) {
        this.taskField = taskField;
    }

    public String getPlaceckodField() {
        return placeckodField;
    }

    public void setPlaceckodField(String placeckodField) {
        this.placeckodField = placeckodField;
    }

    public String getLvmField() {
        return lvmField;
    }

    public void setLvmField(String lvmField) {
        this.lvmField = lvmField;
    }

    public String getPlatoField() {
        return platoField;
    }

    public void setPlatoField(String platoField) {
        this.platoField = platoField;
    }

    public String getSimckodField() {
        return simckodField;
    }

    public void setSimckodField(String simckodField) {
        this.simckodField = simckodField;
    }

    public String getDpuckodField() {
        return dpuckodField;
    }

    public void setDpuckodField(String dpuckodField) {
        this.dpuckodField = dpuckodField;
    }

    public String getCkodField() {
        return ckodField;
    }

    public void setCkodField(String ckodField) {
        this.ckodField = ckodField;
    }

    public String getInventoryField() {
        return inventoryField;
    }

    public void setInventoryField(String inventoryField) {
        this.inventoryField = inventoryField;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
