package attendancesystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles saving a newly registered student and their schedule.
 * Kept simple: one method, one transaction.
 */
public class StudentDAO {

    public static int registerStudent(String firstName, String lastName, String email,
                                      String days, String timeIn, String timeOut) throws SQLException {

        String insertStudent = "INSERT INTO STUDENT (first_name, last_name, email, status) VALUES (?, ?, ?, 'a')";
        String insertSchedule = "INSERT INTO SCHEDULE (student_id, days, time_in, time_out) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);

            int studentId;
            try (PreparedStatement ps = conn.prepareStatement(insertStudent, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, (email == null || email.isEmpty()) ? null : email);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Failed to obtain the new student id.");
                    }
                    studentId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertSchedule)) {
                ps.setString(1, String.valueOf(studentId));
                ps.setString(2, days);
                ps.setString(3, timeIn);
                ps.setString(4, timeOut);
                ps.executeUpdate();
            }

            conn.commit();
            return studentId;

        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignore) {
                }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    /**
     * Finds a student by id. Returns {id, first_name, last_name, status}
     * or null when no student has that id.
     */
    public static String[] findById(int studentId) throws SQLException {
        String sql = "SELECT id, first_name, last_name, status FROM STUDENT WHERE id = ?";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("status")
                    };
                }
            }
        }
        return null;
    }

    /**
     * Returns the first schedule of a student as {days, time_in, time_out}
     * or null when the student has no schedule.
     */
    public static String[] getSchedule(int studentId) throws SQLException {
        String sql = "SELECT days, time_in, time_out FROM SCHEDULE WHERE student_id = ? LIMIT 1";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(studentId));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                        rs.getString("days"),
                        rs.getString("time_in"),
                        rs.getString("time_out")
                    };
                }
            }
        }
        return null;
    }
}