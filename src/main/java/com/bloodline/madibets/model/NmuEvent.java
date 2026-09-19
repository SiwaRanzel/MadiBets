package com.bloodline.madibets.model;

/**
 * Represents a single event scraped from https://events.mandela.ac.za/
 */
public class NmuEvent {

    private String name;
    private String date;
    private String url;

    public NmuEvent() {}

    public NmuEvent(String name, String date, String url) {
        this.name = name;
        this.date = date;
        this.url  = url;
    }

    public String getName()          { return name; }
    public void   setName(String n)  { this.name = n; }

    public String getDate()          { return date; }
    public void   setDate(String d)  { this.date = d; }

    public String getUrl()           { return url; }
    public void   setUrl(String u)   { this.url = u; }

    @Override
    public String toString() {
        return "NmuEvent{name='" + name + "', date='" + date + "', url='" + url + "'}";
    }
}
