package opgg.ghrami.model;

import java.time.LocalDateTime;

/**
 * Model representing a class booking
 */
public class Booking {
    private Long bookingId;
    private Long classId;
    private Long userId;
    private LocalDateTime bookingDate;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private double totalAmount;
    
    // Class details (joined from classes table)
    private String classTitle;
    private String classCategory;
    private int classDuration;
    
    // User details (joined from users table)
    private String username;
    private String userFullName;
    private String userEmail;
    
    // Provider details
    private Long providerId;
    private String providerName;
    
    public Booking() {
    }
    
    public Booking(Long bookingId, Long classId, Long userId, LocalDateTime bookingDate,
                  BookingStatus status, PaymentStatus paymentStatus, double totalAmount) {
        this.bookingId = bookingId;
        this.classId = classId;
        this.userId = userId;
        this.bookingDate = bookingDate;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.totalAmount = totalAmount;
    }
    
    // Getters and Setters
    public Long getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
    
    public Long getClassId() {
        return classId;
    }
    
    public void setClassId(Long classId) {
        this.classId = classId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getBookingDate() {
        return bookingDate;
    }
    
    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }
    
    public BookingStatus getStatus() {
        return status;
    }
    
    public void setStatus(BookingStatus status) {
        this.status = status;
    }
    
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getClassTitle() {
        return classTitle;
    }
    
    public void setClassTitle(String classTitle) {
        this.classTitle = classTitle;
    }
    
    public String getClassCategory() {
        return classCategory;
    }
    
    public void setClassCategory(String classCategory) {
        this.classCategory = classCategory;
    }
    
    public int getClassDuration() {
        return classDuration;
    }
    
    public void setClassDuration(int classDuration) {
        this.classDuration = classDuration;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getUserFullName() {
        return userFullName;
    }
    
    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public Long getProviderId() {
        return providerId;
    }
    
    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId +
                ", classId=" + classId +
                ", userId=" + userId +
                ", bookingDate=" + bookingDate +
                ", status=" + status +
                ", paymentStatus=" + paymentStatus +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
