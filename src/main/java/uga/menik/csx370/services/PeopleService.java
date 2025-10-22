/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uga.menik.csx370.models.FollowableUser;

/**
 * This service contains people related functions.
 */
@Service
public class PeopleService {
    private final DataSource dataSource;

    @Autowired
    public PeopleService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * This function should query and return all users that 
     * are followable. The list should not contain the user 
     * with id userIdToExclude.
     */
    public List<FollowableUser> getFollowableUsers(String userIdToExclude) throws SQLException {
        // The output list of people.
        List<FollowableUser> output = new ArrayList<>();
        // Note the ? placeholder, filled in later, used to avoid problems such as SQL injection.
        final String sql = """
            SELECT u.userId, u.firstName, u.lastName, 
                DATE_FORMAT(MAX(p.createdAt), '%b %d, %Y, %l:%i %p') AS lastPostDate,
                EXISTS (
                    SELECT 1
                    FROM follows f
                    WHERE f.userId = ?
                    AND f.useridFollowed = u.userId
                ) as isFollowed
            FROM user u
            LEFT JOIN posts p ON p.authorId = u.userId
            WHERE u.userId != ?
            GROUP BY u.userId, u.firstName, u.lastName
            ORDER BY lastPostDate DESC
        """;
        
        try (
            // Connect to database
            Connection conn = dataSource.getConnection();
            // Prepare statement
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, userIdToExclude);
            pstmt.setString(2, userIdToExclude);
            // The results of the query
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    String lastPostDate = rs.getString("lastPostDate");
                    Boolean isFollowed = rs.getBoolean("isFollowed");

                    if (lastPostDate == null) {
                        lastPostDate = ": Never";
                    }

                    FollowableUser user = new FollowableUser(userId, firstName, lastName, isFollowed, lastPostDate);
                    output.add(user);
                }
            }
        }
        return output;
    }

    public boolean toggleFollow(String currentUserId, String targetUserId) throws SQLException {
        final String checkSql = """
            SELECT 1 FROM follows
            WHERE userId = ? AND userIdFollowed = ?
        """;

        final String insertSql = """
            INSERT INTO follows (userId, userIdFollowed)
            VALUES (?, ?)
        """;

        final String deleteSql = """
            DELETE FROM follows
            WHERE userId = ? AND userIdFollowed = ?
        """;

        try (Connection conn = dataSource.getConnection()) {
            // Check if user is already followed
            boolean isFollowed = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, currentUserId);
                checkStmt.setString(2, targetUserId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    isFollowed = rs.next();
                }
            }

            if (isFollowed) {
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, currentUserId);
                    deleteStmt.setString(2, targetUserId);
                    deleteStmt.executeUpdate();
                }
                return false; // now unfollowed
            } else {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, currentUserId);
                    insertStmt.setString(2, targetUserId);
                    insertStmt.executeUpdate();
                }
                return true; // now followed
            }
        }
    }

}
