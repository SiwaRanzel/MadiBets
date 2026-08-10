package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Event;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

/** Owner: Kieran (B-series). Copy the PreparedStatement pattern from UserDAO. */
public class EventDAO {

    /** Used by B200 to validate the event a proposal points at. Returns null if absent. */
    public Event findById(int eventID) throws SQLException {
        String sql = "SELECT eventID, eventDescription, `date`, startTime, endTime, status "
                   + "FROM Event WHERE eventID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, eventID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private Event map(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setEventID(rs.getInt("eventID"));
        e.setEventDescription(rs.getString("eventDescription"));
        e.setDate(rs.getObject("date", LocalDate.class));
        e.setStartTime(rs.getObject("startTime", LocalTime.class));
        e.setEndTime(rs.getObject("endTime", LocalTime.class));
        e.setStatus(rs.getString("status"));
        return e;
    }
}
