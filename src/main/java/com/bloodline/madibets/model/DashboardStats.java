package com.bloodline.madibets.model;

public class DashboardStats {
    private int betsProposedToday;
    private int betsPlacedToday;
    private int betsPendingReview;
    private int upcomingEvents;

    private int totalUsers;
    private int usersJoinedToday;
    private int openSupportQueries;
    private int usersRewardedToday;

    public DashboardStats() {}

    public int getBetsProposedToday() { return betsProposedToday; }
    public void setBetsProposedToday(int betsProposedToday) { this.betsProposedToday = betsProposedToday; }

    public int getBetsPlacedToday() { return betsPlacedToday; }
    public void setBetsPlacedToday(int betsPlacedToday) { this.betsPlacedToday = betsPlacedToday; }

    public int getBetsPendingReview() { return betsPendingReview; }
    public void setBetsPendingReview(int betsPendingReview) { this.betsPendingReview = betsPendingReview; }

    public int getUpcomingEvents() { return upcomingEvents; }
    public void setUpcomingEvents(int upcomingEvents) { this.upcomingEvents = upcomingEvents; }

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public int getUsersJoinedToday() { return usersJoinedToday; }
    public void setUsersJoinedToday(int usersJoinedToday) { this.usersJoinedToday = usersJoinedToday; }

    public int getOpenSupportQueries() { return openSupportQueries; }
    public void setOpenSupportQueries(int openSupportQueries) { this.openSupportQueries = openSupportQueries; }

    public int getUsersRewardedToday() { return usersRewardedToday; }
    public void setUsersRewardedToday(int usersRewardedToday) { this.usersRewardedToday = usersRewardedToday; }
}
