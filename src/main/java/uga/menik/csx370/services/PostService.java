package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uga.menik.csx370.models.Comment;
import uga.menik.csx370.models.ExpandedPost;
import uga.menik.csx370.models.Post;
import uga.menik.csx370.models.User;

@Service
public class PostService {
    private final DataSource dataSource;

    @Autowired
    public PostService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Get all posts from users followed by the logged in user.
     */
    public List<Post> getPostsFromFollowedUsers(String loggedInUserId) throws SQLException {
        final String sql = """
        
            (SELECT p.postId, p.body AS content,
                DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
                u.userId, u.firstName, u.lastName,
                (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount,
                (SELECT COUNT(*) FROM comments c WHERE c.postId = p.postId) AS commentsCount
            FROM posts p
            JOIN user u ON u.userId = p.authorId
            JOIN follows f ON f.userIdFollowed = p.authorId
            WHERE f.userId = ?
            ORDER BY p.createdAt DESC)
            UNION ALL
            (SELECT 2p.postId, 2p.body AS content,
                DATE_FORMAT(2p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
                2u.userId, 2u.firstName, 2u.lastName,
                (SELECT COUNT(*) FROM likes l WHERE l.postId = 2p.postId) AS heartsCount,
                (SELECT COUNT(*) FROM comments c WHERE c.postId = 2p.postId) AS commentsCount
            FROM repost r
            JOIN posts 2p ON 2p.postId = r.originalPostId
            JOIN user 2u ON 2u.userId = 2p.authorId
            JOIN follows f ON f.userIdFollowed = r.userId
            WHERE f.userId = ?
            AND r.userId <> ?
            ORDER BY 2p.createdAt DESC)   
        """;
        
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, loggedInUserId); // reposts: follows
            pstmt.setString(3, loggedInUserId);
            return getPostsFromSet(pstmt, loggedInUserId);
        }
    }

    /**
     * Returns posts made by a specific user.
     */
    public List<Post> getPostsByUserId(String userId, String userIdOfLoggedIn) throws SQLException {
        final String sql = """
            SELECT p.postId, p.body AS content,
                DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
                u.userId, u.firstName, u.lastName,
                (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount,
                (SELECT COUNT(*) FROM comments c WHERE c.postId = p.postId) AS commentsCount
            FROM posts p
            JOIN user u ON u.userId = p.authorId
            WHERE u.userId = ?
            ORDER BY p.createdAt DESC
        """;
        
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, userId);
            return getPostsFromSet(pstmt, userIdOfLoggedIn);




        }
    }

    /**
     * Returns bookmarked posts of the logged in user.
     */
    public List<Post> getBookmarkedPosts(String loggedInUserId) throws SQLException {
        final String sql = """
            SELECT p.postId, p.body AS content,
                DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
                u.userId, u.firstName, u.lastName,
                (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount,
                (SELECT COUNT(*) FROM comments c WHERE c.postId = p.postId) AS commentsCount
            FROM posts p
            JOIN user u ON u.userId = p.authorId
            JOIN bookmarks b ON b.postId = p.postId
            WHERE b.userId = ?
            ORDER BY p.createdAt DESC
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, loggedInUserId);
            return getPostsFromSet(pstmt, loggedInUserId);
        }
    }

    /**
     * Returns posts that contain any of the given hashtags (case-insensitive).
     * If tags is empty returns an empty list.
     */
    public List<Post> getPostsByHashtags(String[] tags, String loggedInUserId) throws SQLException {
        
        // Build the "?, ?, ?" clause with the correct number of placeholders
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < tags.length; i++) {
            if (i > 0) inClause.append(",");
            inClause.append("?");
        } 

        // use string concatenation to insert the inClause into the SQL query instead
        final String sql = "SELECT p.postId, p.body AS content, "
                + "DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate, "
                + "u.userId, u.firstName, u.lastName, "
                + "(SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount, "
                + "(SELECT COUNT(*) FROM comments c WHERE c.postId = p.postId) AS commentsCount "
                + "FROM posts p "
                + "JOIN user u ON u.userId = p.authorId "
                + "JOIN hashtags h ON h.postId = p.postId "
                + "WHERE h.tag IN (" + inClause.toString() + ") "
                + "GROUP BY p.postId "
                + "ORDER BY p.createdAt DESC";

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            // set cleaned tags parameters
            for (int i = 0; i < tags.length; i++) {
                pstmt.setString(i + 1, tags[i]);
            }
            return getPostsFromSet(pstmt, loggedInUserId);
        }
    }

    /*
     * Checks if the searched hashtag exists in the hashtags table.
     */
    public boolean isHashtagInTable(String tag) throws SQLException {
        final String sql = "SELECT 1 FROM hashtags WHERE tag = ? LIMIT 1";

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, tag);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }   

    private List<Post> getPostsFromSet(PreparedStatement pstmt, String loggedInUserId) throws SQLException {
        List<Post> output = new ArrayList<>();
        
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                // Extracting data from the result set.
                String authorId = rs.getString("userId");
                String firstName = rs.getString("firstName");
                String lastName = rs.getString("lastName");
                String postDate = rs.getString("postDate");
                String postId = rs.getString("postId");
                String content = rs.getString("content");
                int heartsCount = rs.getInt("heartsCount");
                int commentsCount = rs.getInt("commentsCount");

                // Create User object of author.
                User author = new User(authorId, firstName, lastName);

                // Check if the logged in user has hearted or bookmarked this post.
                boolean isHearted = isPostLikedByUser(loggedInUserId, postId);
                boolean isBookmarked = isPostBookmarkedByUser(loggedInUserId, postId);
                boolean isReposted = isPostRepostedByUser(loggedInUserId, postId);

                // Create Post object and add to output list.
                Post post = new Post(postId, content, postDate, author, heartsCount, commentsCount, isHearted, isBookmarked, isReposted );
                output.add(post);
            }
        }

        return output;
    }

    public void createPost(String content, String authorId) throws SQLException {
        final String insertPostSql = """
                INSERT INTO posts (authorId, body)
                VALUES (?, ?)
                """;

        final String insertTagSql = """
                INSERT INTO hashtags (postId, tag)
                VALUES (?, ?)
                """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(insertPostSql, PreparedStatement.RETURN_GENERATED_KEYS);
        ) {
            // Insert the post into the posts table.
            pstmt.setString(1, authorId);
            pstmt.setString(2, content);
            pstmt.executeUpdate();

            // Retrieve the generated postId.
            int postId = -1;
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    postId = rs.getInt(1);
                }
            }

            // Parse hashtags from content and insert into hashtags table.
            String[] all_words = content.split("\\s+");
            Set<String> all_tags = new HashSet<>();

            // Insert each unique hashtag.
            PreparedStatement tagStmt = conn.prepareStatement(insertTagSql);
            for (String word : all_words) {
                if (word.startsWith("#") && word.length() > 1) {
                    String tag = word.substring(1).toLowerCase();
                    if (all_tags.add(tag)) {
                        tagStmt.setInt(1, postId);
                        tagStmt.setString(2, tag);
                        tagStmt.executeUpdate();
                    }
                }
            }
        }
    }

    /*
     *  Shows a single post with all its comments.
     */
    public List<ExpandedPost> getExpandedPostsById(String postId, String loggedInUserId) throws SQLException {
        final String postSql = """
            SELECT p.postId, p.body AS content,
                DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
                u.userId, u.firstName, u.lastName,
                (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount
            FROM posts p
            JOIN user u ON p.authorId = u.userId
            WHERE p.postId = ?
        """;

        final String commentSql = """
            SELECT c.commentId, c.body AS content, 
                DATE_FORMAT(c.createdAt, '%b %d, %Y, %l:%i %p') AS commentDate,
                u.userId, u.firstName, u.lastName
            FROM comments c, user u
            WHERE c.postId = ?
            AND c.authorId = u.userId
            ORDER BY c.createdAt ASC
        """;

        // The list of expanded posts to return (should be one or zero).
        List<ExpandedPost> expandedPosts = new ArrayList<>();

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement postStmt = conn.prepareStatement(postSql)
        ) {
            // Get the post details.
            postStmt.setString(1, postId);
            try (ResultSet rs = postStmt.executeQuery()) {
                if (rs.next()) {
                    // Extracting data from the result set.
                    String content = rs.getString("content");
                    String postDate = rs.getString("postDate");
                    int heartsCount = rs.getInt("heartsCount");

                    String authorId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");

                    // Create User object of author.
                    User author = new User(authorId, firstName, lastName);

                    // Get comments for the post.
                    List<Comment> comments = new ArrayList<>();

                    try (PreparedStatement commentStmt = conn.prepareStatement(commentSql)) {
                        // Get the comments.
                        commentStmt.setString(1, postId);

                        try (ResultSet rs2 = commentStmt.executeQuery()) {
                            while (rs2.next()) {
                                // Extracting data from the result set.
                                String commentAuthorId = rs2.getString("userId");
                                String commentFirst = rs2.getString("firstName");
                                String commentLast = rs2.getString("lastName");

                                // Create User object of comment author.
                                User commentAuthor = new User(commentAuthorId, commentFirst, commentLast);

                                String commentId = rs2.getString("commentId");
                                String commentBody = rs2.getString("content");
                                String commentDate = rs2.getString("commentDate");

                                // Create Comment object and add to comments list.
                                Comment comment = new Comment(commentId, commentBody, commentDate, commentAuthor);
                                comments.add(comment);
                            }
                        }
                    }

                    // Check if the logged in user has hearted or bookmarked this post.
                    boolean isHearted = isPostLikedByUser(loggedInUserId, postId);
                    boolean isBookmarked = isPostBookmarkedByUser(loggedInUserId, postId);
                    boolean isReposted = isPostRepostedByUser(loggedInUserId, postId); // new

                    // Create ExpandedPost object and add to output list.
                    ExpandedPost expandedPost = new ExpandedPost(
                        postId,
                        content,
                        postDate,
                        author,
                        heartsCount,
                        comments.size(),
                        isHearted,
                        isBookmarked,
                        isReposted, // new
                        comments

                    );
                    expandedPosts.add(expandedPost);
                }
            }
        }

        return expandedPosts;
    }

    /**
     * Adds a comment to a post.
     */
    public void addComment(String postId, String authorId, String body) throws SQLException {
        System.out.println("Adding comment to postId: " + postId + " by authorId: " + authorId);
        
        final String sql = """
            INSERT INTO comments (postId, authorId, body)
            VALUES (?, ?, ?)
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            pstmt.setString(1, postId);
            pstmt.setString(2, authorId);
            pstmt.setString(3, body);
            pstmt.executeUpdate();
        }
    }

    
    /**
     * Adds a like (heart) for a post by a user. Returns true if inserted, false if it already existed.
     */
    public void addLike(String userId, String postId) throws SQLException {
        final String sql = """
            INSERT INTO likes (userId, postId)
            VALUES (?, ?)
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            pstmt.setString(1, userId);
            pstmt.setString(2, postId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Removes a like (heart) for a post by a user. Returns true if a row was deleted.
     */
    public void removeLike(String userId, String postId) throws SQLException {
        final String sql = """
            DELETE FROM likes
            WHERE userId = ? AND postId = ?
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            pstmt.setString(1, userId);
            pstmt.setString(2, postId);
            pstmt.executeUpdate();
        } 
    }

    /**
     * Adds a bookmark for a post by a user.
     */
    public void addBookmark(String userId, String postId) throws SQLException {
        final String sql = """
            INSERT INTO bookmarks (userId, postId)
            VALUES (?, ?)
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            pstmt.setString(1, userId);
            pstmt.setString(2, postId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Removes a bookmark for a post by a user.
     */
    public void removeBookmark(String userId, String postId) throws SQLException {
        final String sql = """
            DELETE FROM bookmarks
            WHERE userId = ? AND postId = ?
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            pstmt.setString(1, userId);
            pstmt.setString(2, postId);
            pstmt.executeUpdate();
        }
    }

    public boolean isPostLikedByUser(String loggedInUserId, String postId) throws SQLException {
        final String sql = """
            SELECT 1 FROM likes 
            WHERE userId = ? AND postId = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean isPostBookmarkedByUser(String loggedInUserId, String postId) throws SQLException {
        final String sql = """
            SELECT 1 FROM bookmarks 
            WHERE userId = ? AND postId = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, loggedInUserId);
            pstmt.setString(2, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

           // Repost service methods can be added here.
            public boolean isPostRepostedByUser(String userId, String postId) throws SQLException {
            final String sql = "SELECT 1 FROM repost WHERE userId = ? AND originalPostId = ? ";
            try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                ps.setString(2, postId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }

        public void addRepost(String userId, String postId) throws SQLException {
            final String sql = """
                INSERT INTO repost (userId, originalPostId, createdAt)
                VALUES (?, ?, NOW())
            """;
            try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                ps.setString(2, postId);
                ps.executeUpdate();
            }
        }
        public void removeRepost(String userId, String postId) throws SQLException {
            final String sql = """
                DELETE FROM repost
                WHERE userId = ? AND originalPostId = ?
            """;
            try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId);
                ps.setString(2, postId);
                ps.executeUpdate();
            }
        }   

}