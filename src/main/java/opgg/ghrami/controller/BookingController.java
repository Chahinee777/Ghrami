package opgg.ghrami.controller;

import opgg.ghrami.model.Booking;
import opgg.ghrami.model.BookingStatus;
import opgg.ghrami.model.PaymentStatus;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for managing bookings
 */
public class BookingController {
    private static BookingController instance;
    
    private BookingController() {
    }
    
    public static BookingController getInstance() {
        if (instance == null) {
            instance = new BookingController();
        }
        return instance;
    }
    
    /**
     * Create a new booking
     */
    public boolean create(Booking booking) {
        // First check if class has available spots
        if (!checkAvailability(booking.getClassId())) {
            System.err.println("Class is full, cannot create booking");
            return false;
        }
        
        String sql = "INSERT INTO bookings (class_id, user_id, booking_date, status, payment_status, total_amount) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, booking.getClassId());
            stmt.setLong(2, booking.getUserId());
            stmt.setTimestamp(3, Timestamp.valueOf(booking.getBookingDate()));
            stmt.setString(4, booking.getStatus().getValue());
            stmt.setString(5, booking.getPaymentStatus().getValue());
            stmt.setDouble(6, booking.getTotalAmount());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        booking.setBookingId(generatedKeys.getLong(1));
                    }
                }
                System.out.println("Booking created successfully: " + booking.getBookingId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating booking: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get booking by ID with full details
     */
    public Booking getById(Long bookingId) {
        String sql = "SELECT b.*, " +
                    "  c.title as class_title, c.category as class_category, c.duration as class_duration, c.provider_id, " +
                    "  u.username, u.full_name as user_full_name, u.email as user_email, " +
                    "  p_user.full_name as provider_name " +
                    "FROM bookings b " +
                    "JOIN classes c ON b.class_id = c.class_id " +
                    "JOIN users u ON b.user_id = u.user_id " +
                    "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
                    "JOIN users p_user ON cp.user_id = p_user.user_id " +
                    "WHERE b.booking_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, bookingId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToBooking(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching booking: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get all bookings for a user
     */
    public List<Booking> getByUserId(Long userId, BookingStatus status) {
        List<Booking> bookings = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT b.*, " +
            "  c.title as class_title, c.category as class_category, c.duration as class_duration, c.provider_id, " +
            "  u.username, u.full_name as user_full_name, u.email as user_email, " +
            "  p_user.full_name as provider_name " +
            "FROM bookings b " +
            "JOIN classes c ON b.class_id = c.class_id " +
            "JOIN users u ON b.user_id = u.user_id " +
            "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
            "JOIN users p_user ON cp.user_id = p_user.user_id " +
            "WHERE b.user_id = ?"
        );
        
        if (status != null) {
            sql.append(" AND b.status = '").append(status.getValue()).append("'");
        }
        
        sql.append(" ORDER BY b.booking_date DESC");
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(mapResultSetToBooking(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching user bookings: " + e.getMessage());
            e.printStackTrace();
        }
        return bookings;
    }
    
    /**
     * Get all bookings for a class
     */
    public List<Booking> getByClassId(Long classId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT b.*, " +
                    "  c.title as class_title, c.category as class_category, c.duration as class_duration, c.provider_id, " +
                    "  u.username, u.full_name as user_full_name, u.email as user_email, " +
                    "  p_user.full_name as provider_name " +
                    "FROM bookings b " +
                    "JOIN classes c ON b.class_id = c.class_id " +
                    "JOIN users u ON b.user_id = u.user_id " +
                    "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
                    "JOIN users p_user ON cp.user_id = p_user.user_id " +
                    "WHERE b.class_id = ? " +
                    "ORDER BY b.booking_date DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, classId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(mapResultSetToBooking(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching class bookings: " + e.getMessage());
            e.printStackTrace();
        }
        return bookings;
    }
    
    /**
     * Get all bookings for a provider
     */
    public List<Booking> getByProviderId(Long providerId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT b.*, " +
                    "  c.title as class_title, c.category as class_category, c.duration as class_duration, c.provider_id, " +
                    "  u.username, u.full_name as user_full_name, u.email as user_email, " +
                    "  p_user.full_name as provider_name " +
                    "FROM bookings b " +
                    "JOIN classes c ON b.class_id = c.class_id " +
                    "JOIN users u ON b.user_id = u.user_id " +
                    "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
                    "JOIN users p_user ON cp.user_id = p_user.user_id " +
                    "WHERE c.provider_id = ? " +
                    "ORDER BY b.booking_date DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, providerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                bookings.add(mapResultSetToBooking(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching provider bookings: " + e.getMessage());
            e.printStackTrace();
        }
        return bookings;
    }
    
    /**
     * Update booking status
     */
    public boolean updateStatus(Long bookingId, BookingStatus status) {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.getValue());
            stmt.setLong(2, bookingId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Booking status updated: " + bookingId + " -> " + status);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating booking status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Update payment status
     */
    public boolean updatePaymentStatus(Long bookingId, PaymentStatus paymentStatus) {
        String sql = "UPDATE bookings SET payment_status = ? WHERE booking_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, paymentStatus.getValue());
            stmt.setLong(2, bookingId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Payment status updated: " + bookingId + " -> " + paymentStatus);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating payment status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Cancel a booking
     */
    public boolean cancel(Long bookingId) {
        return updateStatus(bookingId, BookingStatus.CANCELLED);
    }
    
    /**
     * Complete a booking
     */
    public boolean complete(Long bookingId) {
        return updateStatus(bookingId, BookingStatus.COMPLETED);
    }
    
    /**
     * Check if class has available spots
     */
    public boolean checkAvailability(Long classId) {
        String sql = "SELECT c.max_participants, " +
                    "  (SELECT COUNT(*) FROM bookings WHERE class_id = ? AND status != 'cancelled') as current_enrollment " +
                    "FROM classes c WHERE c.class_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, classId);
            stmt.setLong(2, classId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int maxParticipants = rs.getInt("max_participants");
                int currentEnrollment = rs.getInt("current_enrollment");
                return currentEnrollment < maxParticipants;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking availability: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Check if user already booked a class
     */
    public boolean hasUserBooked(Long userId, Long classId) {
        String sql = "SELECT COUNT(*) FROM bookings " +
                    "WHERE user_id = ? AND class_id = ? AND status != 'cancelled'";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, classId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking user booking: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get total revenue for a provider
     */
    public double getProviderRevenue(Long providerId, BookingStatus status) {
        StringBuilder sql = new StringBuilder(
            "SELECT SUM(b.total_amount) as total_revenue " +
            "FROM bookings b " +
            "JOIN classes c ON b.class_id = c.class_id " +
            "WHERE c.provider_id = ? AND b.payment_status = 'paid'"
        );
        
        if (status != null) {
            sql.append(" AND b.status = '").append(status.getValue()).append("'");
        }
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            stmt.setLong(1, providerId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("total_revenue");
            }
            
        } catch (SQLException e) {
            System.err.println("Error calculating provider revenue: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }
    
    /**
     * Get total revenue for a provider by payment status
     */
    public double getProviderRevenueByPaymentStatus(Long providerId, PaymentStatus paymentStatus) {
        String sql = "SELECT SUM(b.total_amount) as total_revenue " +
                    "FROM bookings b " +
                    "JOIN classes c ON b.class_id = c.class_id " +
                    "WHERE c.provider_id = ? AND b.payment_status = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, providerId);
            stmt.setString(2, paymentStatus.getValue());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("total_revenue");
            }
            
        } catch (SQLException e) {
            System.err.println("Error calculating provider revenue by payment status: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }
    
    /**
     * Delete a booking
     */
    public boolean delete(Long bookingId) {
        String sql = "DELETE FROM bookings WHERE booking_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, bookingId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Booking deleted successfully: " + bookingId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error deleting booking: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Map ResultSet to Booking object
     */
    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        Booking booking = new Booking();
        booking.setBookingId(rs.getLong("booking_id"));
        booking.setClassId(rs.getLong("class_id"));
        booking.setUserId(rs.getLong("user_id"));
        booking.setBookingDate(rs.getTimestamp("booking_date").toLocalDateTime());
        booking.setStatus(BookingStatus.fromString(rs.getString("status")));
        booking.setPaymentStatus(PaymentStatus.fromString(rs.getString("payment_status")));
        booking.setTotalAmount(rs.getDouble("total_amount"));
        
        // Class details
        booking.setClassTitle(rs.getString("class_title"));
        booking.setClassCategory(rs.getString("class_category"));
        booking.setClassDuration(rs.getInt("class_duration"));
        
        // User details
        booking.setUsername(rs.getString("username"));
        booking.setUserFullName(rs.getString("user_full_name"));
        booking.setUserEmail(rs.getString("user_email"));
        
        // Provider details
        booking.setProviderId(rs.getLong("provider_id"));
        booking.setProviderName(rs.getString("provider_name"));
        
        return booking;
    }
}
