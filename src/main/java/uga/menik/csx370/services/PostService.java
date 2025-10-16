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

import uga.menik.csx370.models.BasicPost;
import uga.menik.csx370.models.User;

@Service
public class PostService {
    private final DataSource dataSource;

    @Autowired
    public PostService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<BasicPost> getPostsWithoutComments(String userId) throws SQLException {
        List<BasicPost> output = new ArrayList<>();

        final String sql = """
            SELECT p.postId, p.body AS content, p.createdAt AS postDate, u.userId, u.firstName, u.lastName 
            FROM posts p, user u, follows f 
            WHERE f.userId = ? 
            AND f.userIdFollowed = p.authorId 
            AND u.userId = p.authorId
            ORDER BY createdAt DESC
        """;
        
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String authorId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");

                    User author = new User(authorId, firstName, lastName);
                    BasicPost post = new BasicPost(authorId, firstName, lastName, author);
                    output.add(post);
                }
            }
        }
        return output;
    }
}
