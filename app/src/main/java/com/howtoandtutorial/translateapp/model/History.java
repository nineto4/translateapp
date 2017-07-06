package com.howtoandtutorial.translateapp.model;

/**
 * Created by Dao on 5/25/2017.
 * History:
 * This is History model
 */

public class History {

    private int id;
    private String textKey;
    private String textResult;
    private String lang;

    public History(int id, String textKey, String textResult, String lang) {
        this.id = id;
        this.textKey = textKey;
        this.textResult = textResult;
        this.lang = lang;
    }

    public History() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTextKey() {
        return textKey;
    }

    public void setTextKey(String textKey) {
        this.textKey = textKey;
    }

    public String getTextResult() {
        return textResult;
    }

    public void setTextResult(String textResult) {
        this.textResult = textResult;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
