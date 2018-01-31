package ru.curs.flute.rest;

public class FluteResponse {

    private final int status;
    private String text;

    private FluteResponse(int status) {
        this.status = status;
    }

    public static FluteResponse status(int status) {
        return new FluteResponse(status);
    }

    public FluteResponse text(String text) {
        this.text = text;
        return this;
    }

    int getStatus() {
        return status;
    }

    String getText() {
        return text;
    }
}
