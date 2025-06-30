package com.recipevault.model;

import java.util.Date;

public class Premium {
    private String userId;
    private boolean isPremium;
    private Date subscriptionStartDate;
    private Date subscriptionEndDate;
    private String subscriptionType; // "monthly", "yearly"
    private boolean autoRenew;
    private String paymentMethod;
    private double price;
    private String status; // "active", "expired", "cancelled"

    // Constructors
    public Premium() {}

    public Premium(String userId, boolean isPremium, String subscriptionType) {
        this.userId = userId;
        this.isPremium = isPremium;
        this.subscriptionType = subscriptionType;
        this.subscriptionStartDate = new Date();
        this.autoRenew = true;
        this.status = "active";
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public Date getSubscriptionStartDate() { return subscriptionStartDate; }
    public void setSubscriptionStartDate(Date subscriptionStartDate) { this.subscriptionStartDate = subscriptionStartDate; }

    public Date getSubscriptionEndDate() { return subscriptionEndDate; }
    public void setSubscriptionEndDate(Date subscriptionEndDate) { this.subscriptionEndDate = subscriptionEndDate; }

    public String getSubscriptionType() { return subscriptionType; }
    public void setSubscriptionType(String subscriptionType) { this.subscriptionType = subscriptionType; }

    public boolean isAutoRenew() { return autoRenew; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Helper methods
    public boolean isSubscriptionActive() {
        return isPremium && "active".equals(status) &&
                subscriptionEndDate != null && subscriptionEndDate.after(new Date());
    }

    public long getDaysRemaining() {
        if (subscriptionEndDate == null) return 0;
        long diff = subscriptionEndDate.getTime() - new Date().getTime();
        return diff / (24 * 60 * 60 * 1000);
    }
}