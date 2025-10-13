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
        final String sql = "select * from user where userId != ?";
        
        try (
            // Connect to database
            Connection conn = dataSource.getConnection();
            // Prepare statement
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            // Replace the ? placeholder with the user id to not list.
            pstmt.setString(1, userIdToExclude);
            // The results of the query
            try (ResultSet rs = pstmt.executeQuery()) {
                // Iterate through each row, getting their info from user table, 
                // and putting placeholders for isFollowed and lastActiveDate, 
                // adding each user to output list
                while (rs.next()) {
                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");

                    FollowableUser user = new FollowableUser(userId, firstName, lastName, false, "10/09/2025");
                    output.add(user);
                }
            }
        }
        return output;
    }
}
