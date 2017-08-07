package ru.curs.flute.rest;

/**
 * Created by ioann on 03.08.2017.
 */
public class FluteResponse {
  private int status;
  private String text;

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

}
