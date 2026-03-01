package opgg.ghrami.util;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Analytics service – executes SQL aggregation queries for the Admin analytics tab.
 */
public class AnalyticsService {

    private static AnalyticsService instance;

    public static AnalyticsService getInstance() {
        if (instance == null) instance = new AnalyticsService();
        return instance;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long count(String sql) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private double sum(String sql) {
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private Map<String, Long> groupBy(String sql) {
        Map<String, Long> map = new LinkedHashMap<>();
        try (Connection c = DatabaseConnection.getInstance().getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getLong(2));
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    // ── Users ────────────────────────────────────────────────────────────────

    public long getTotalUsers()    { return count("SELECT COUNT(*) FROM users"); }
    public long getBannedUsers()   { return count("SELECT COUNT(*) FROM users WHERE is_banned = 1"); }
    public long getOnlineUsers()   { return count("SELECT COUNT(*) FROM users WHERE is_online = 1"); }
    public long getLocalUsers()    { return count("SELECT COUNT(*) FROM users WHERE auth_provider = 'local'"); }
    public long getGoogleUsers()   { return count("SELECT COUNT(*) FROM users WHERE auth_provider = 'google'"); }

    // ── Social ───────────────────────────────────────────────────────────────

    public long getTotalPosts()        { return count("SELECT COUNT(*) FROM posts"); }
    public long getTotalComments()     { return count("SELECT COUNT(*) FROM comments"); }
    public long getTotalLikes()        { return count("SELECT COUNT(*) FROM post_likes"); }
    public long getTotalStories()      { return count("SELECT COUNT(*) FROM stories"); }
    public long getAcceptedFriendships() {
        return count("SELECT COUNT(*) FROM friendships WHERE status = 'ACCEPTED'");
    }
    public long getPendingFriendships() {
        return count("SELECT COUNT(*) FROM friendships WHERE status = 'PENDING'");
    }

    // ── Badges ───────────────────────────────────────────────────────────────

    public long getTotalBadges()   { return count("SELECT COUNT(*) FROM badges"); }
    public long getUniqueBadgeTypes() {
        return count("SELECT COUNT(DISTINCT name) FROM badges");
    }

    // ── Hobbies ──────────────────────────────────────────────────────────────

    public long getTotalHobbies()         { return count("SELECT COUNT(*) FROM hobbies"); }
    public long getTotalMilestones()      { return count("SELECT COUNT(*) FROM milestones"); }
    public long getAchievedMilestones()   { return count("SELECT COUNT(*) FROM milestones WHERE is_achieved = 1"); }
    public double getTotalHobbyHours()    { return sum("SELECT COALESCE(SUM(hours_spent),0) FROM progress"); }

    // ── Classes & Bookings ───────────────────────────────────────────────────

    public long getTotalClasses()       { return count("SELECT COUNT(*) FROM classes"); }
    public long getCompletedBookings()  { return count("SELECT COUNT(*) FROM bookings WHERE status = 'completed'"); }
    public long getScheduledBookings()  { return count("SELECT COUNT(*) FROM bookings WHERE status = 'scheduled'"); }
    public long getPendingBookings()    { return count("SELECT COUNT(*) FROM bookings WHERE status = 'pending'"); }
    public long getCancelledBookings()  { return count("SELECT COUNT(*) FROM bookings WHERE status = 'cancelled'"); }
    public long getTotalBookings()      { return count("SELECT COUNT(*) FROM bookings"); }
    public double getTotalRevenue() {
        return sum("SELECT COALESCE(SUM(total_amount),0) FROM bookings WHERE payment_status = 'paid'");
    }

    // ── Meetings ─────────────────────────────────────────────────────────────

    public long getTotalMeetings()      { return count("SELECT COUNT(*) FROM meetings"); }
    public long getCompletedMeetings()  { return count("SELECT COUNT(*) FROM meetings WHERE status = 'completed'"); }
    public long getScheduledMeetings()  { return count("SELECT COUNT(*) FROM meetings WHERE status = 'scheduled'"); }
    public long getCancelledMeetings()  { return count("SELECT COUNT(*) FROM meetings WHERE status = 'cancelled'"); }

    // ── Grouped / Chart Data ─────────────────────────────────────────────────

    public Map<String, Long> getHobbyByCategory() {
        return groupBy(
            "SELECT COALESCE(category,'Other'), COUNT(*) FROM hobbies " +
            "GROUP BY category ORDER BY COUNT(*) DESC LIMIT 8");
    }

    public Map<String, Long> getBookingsByStatus() {
        return groupBy("SELECT COALESCE(status,'unknown'), COUNT(*) FROM bookings GROUP BY status");
    }

    public Map<String, Long> getMeetingsByStatus() {
        return groupBy("SELECT COALESCE(status,'unknown'), COUNT(*) FROM meetings GROUP BY status");
    }

    public Map<String, Long> getTopBadges() {
        return groupBy(
            "SELECT name, COUNT(*) FROM badges GROUP BY name ORDER BY COUNT(*) DESC LIMIT 8");
    }

    public Map<String, Long> getFriendshipsByStatus() {
        return groupBy("SELECT COALESCE(status,'unknown'), COUNT(*) FROM friendships GROUP BY status");
    }
}
