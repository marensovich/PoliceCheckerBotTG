package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate statistics returned for the admin dashboard.
 *
 * <p>Returned by {@code GET /api/admin/stats}. All counters reflect the current
 * live state of the database at the time the request is processed.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private long totalUsers;
    private long premiumUsers;
    private long bannedUsers;
    private long moderatorCount;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
    private long totalPosts;
    private long activePosts;
}
