package com.bloodline.madibets.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Mirrors one row of the Event table. Owner: Kieran (B-series). */
public class Event {
    private int eventID;
    private String eventDescription;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;           // UPCOMING | LIVE | ENDED | CANCELLED

    public Event() {}

    public int getEventID()                    { return eventID; }
    public void setEventID(int v)              { this.eventID = v; }
    public String getEventDescription()        { return eventDescription; }
    public void setEventDescription(String v)  { this.eventDescription = v; }
    public LocalDate getDate()                 { return date; }
    public void setDate(LocalDate v)           { this.date = v; }
    public LocalTime getStartTime()            { return startTime; }
    public void setStartTime(LocalTime v)      { this.startTime = v; }
    public LocalTime getEndTime()              { return endTime; }
    public void setEndTime(LocalTime v)        { this.endTime = v; }
    public String getStatus()                  { return status; }
    public void setStatus(String v)            { this.status = v; }
}
