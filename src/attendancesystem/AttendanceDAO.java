package attendancesystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles today's time-in / time-out records.
 * Kept simple: plain methods that open a connection through DbConnection.
 */
public class AttendanceDAO {

    /** A single student's attendance row for today. */
    public static class Today {
        public int id;
        public String timeIn;
        public String timeOut;
    }

    /**
     * Returns the student's attendance row for today, or null if the
     * student has not timed in yet.
     */
    public static Today findToday(int studentId) throws SQLException {
        String sql = "SELECT id, time_in, time_out FROM ATTENDANCE "
                + "WHERE student_id = ? AND date = CURDATE()";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Today today = new Today();
                    today.id = rs.getInt("id");
                    today.timeIn = rs.getString("time_in");
                    today.timeOut = rs.getString("time_out");
                    return today;
                }
            }
        }
        return null;
    }

    /** Records a time-in for today. time_out is left NULL until time-out. */
    public static void insertTimeIn(int studentId, String timeIn, String status) throws SQLException {
        String sql = "INSERT INTO ATTENDANCE (student_id, date, time_in, time_out, status) "
                + "VALUES (?, CURDATE(), ?, NULL, ?)";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, timeIn);
            ps.setString(3, status);
            ps.executeUpdate();
        }
    }

    /** Sets (or overwrites) the time-out of an existing attendance row. */
    public static void updateTimeOut(int attendanceId, String timeOut) throws SQLException {
        String sql = "UPDATE ATTENDANCE SET time_out = ? WHERE id = ?";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, timeOut);
            ps.setInt(2, attendanceId);
            ps.executeUpdate();
        }
    }

    /**
     * Returns all attendance rows for today as Object[]:
     * {student_id, name, time_in, time_out, status}.
     */
    public static List<Object[]> getTodayLogs() throws SQLException {
        String sql = "SELECT a.student_id, CONCAT(s.first_name, ' ', s.last_name) AS name, "
                + "a.time_in, a.time_out, a.status "
                + "FROM ATTENDANCE a JOIN STUDENT s ON s.id = a.student_id "
                + "WHERE a.date = CURDATE() ORDER BY a.time_in";

        List<Object[]> rows = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("time_in"),
                    rs.getString("time_out"),
                    rs.getString("status")
                });
            }
        }
        return rows;
    }
}
