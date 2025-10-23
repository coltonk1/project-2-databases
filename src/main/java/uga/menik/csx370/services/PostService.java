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
    /*
     *  Builds the feed for a logged‑in user 
     *  Shows posts that the given user follows on the homepage.
     */
    public List<Post> getPostsWithoutComments(String userId) throws SQLException {
        List<Post> output = new ArrayList<>();

        final String sql = """
            SELECT p.postId, p.body AS content,
                DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
                u.userId, u.firstName, u.lastName,
                (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount,
                (SELECT COUNT(*) FROM comments c WHERE c.postId = p.postId) AS commentsCount
            FROM posts p
            JOIN user u ON u.userId = p.authorId
            JOIN follows f ON f.userIdFollowed = p.authorId
            WHERE f.userId = ?
            ORDER BY p.createdAt DESC
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
                    String postDate = rs.getString("postDate");
                    String postId = rs.getString("postId");
                    String content = rs.getString("content");
                    int heartsCount = rs.getInt("heartsCount");
                    int commentsCount = rs.getInt("commentsCount");

                    User author = new User(authorId, firstName, lastName);
                    boolean isHearted = isPostLikedByUser(userId, postId);
                    boolean isBookmarked = isPostBookmarkedByUser(userId, postId);
                    Post post = new Post(postId, content, postDate, author, heartsCount, commentsCount, isHearted, isBookmarked);
                    output.add(post);
                }
            }
        }
        return output;
    }

    /*
     *  Shows all posts for a specific user (profile page).
     */
    public List<Post> getAllPostsWithoutComments(String userId, String userIdOfLoggedIn) throws SQLException {
        List<Post> output = new ArrayList<>();

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
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String authorId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    String postDate = rs.getString("postDate");
                    String postId = rs.getString("postId");
                    String content = rs.getString("content");
                    int heartsCount = rs.getInt("heartsCount");
                    int commentsCount = rs.getInt("commentsCount");

                    User author = new User(authorId, firstName, lastName);
                    boolean isHearted = isPostLikedByUser(userIdOfLoggedIn, postId);
                    boolean isBookmarked = isPostBookmarkedByUser(userIdOfLoggedIn, postId);
                    Post post = new Post(postId, content, postDate, author, heartsCount, commentsCount, isHearted, isBookmarked);
                    output.add(post);
                }
            }
        }
        return output;
    }

    public void createPost(String content, String authorId) throws SQLException {
        final String sql = """
                INSERT INTO posts (authorId, body, createdAt)
                VALUES (?, ?, NOW())
                """;
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        ) {
            pstmt.setString(1, authorId);
            pstmt.setString(2, content);
            pstmt.executeUpdate();

            int postId = -1;
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    postId = rs.getInt(1);
                }
            }

            String[] all_words = content.split("\\s+");
            Set<String> all_tags = new HashSet<>();
            final String tagSql = """
                    INSERT INTO hashtags (postId, tag) 
                    VALUES (?, ?)
                    """;

            PreparedStatement tagStmt = conn.prepareStatement(tagSql);
            for (String word : all_words) {
                if (word.startsWith("#") && word.length() >= 1) {
                    String tag = word.substring(1).toLowerCase();
                    if (!all_tags.contains(tag)) {
                        all_tags.add(tag);
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
    public List<ExpandedPost> getExpandedPostsById(String postId, String userId) throws SQLException {
        List<ExpandedPost> expandedPosts = new ArrayList<>();

        final String postSql = """
            SELECT p.postId, p.body AS content,
                DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
                u.userId, u.firstName, u.lastName,
                (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount
            FROM posts p
            JOIN user u ON p.authorId = u.userId
            WHERE p.postId = ?
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement postStmt = conn.prepareStatement(postSql)
        ) {
            postStmt.setString(1, postId);
            try (ResultSet rs = postStmt.executeQuery()) {
                if (rs.next()) {
                    String content = rs.getString("content");
                    String postDate = rs.getString("postDate");
                    String authorId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    int heartsCount = rs.getInt("heartsCount");

                    User author = new User(authorId, firstName, lastName);

                    List<Comment> comments = new ArrayList<>();
                    final String commentSql = """
                        SELECT c.commentId, c.body AS content, 
                            DATE_FORMAT(c.createdAt, '%b %d, %Y, %l:%i %p') AS commentDate,
                            u.userId, u.firstName, u.lastName
                        FROM comments c, user u
                        WHERE c.postId = ?
                        AND c.authorId = u.userId
                        ORDER BY c.createdAt ASC
                    """;

                    try (PreparedStatement commentStmt = conn.prepareStatement(commentSql)) {
                        commentStmt.setString(1, postId);
                        try (ResultSet rs2 = commentStmt.executeQuery()) {
                            while (rs2.next()) {
                                String commentId = rs2.getString("commentId");
                                String commentBody = rs2.getString("content");
                                String commentDate = rs2.getString("commentDate");
                                String commentAuthorId = rs2.getString("userId");
                                String commentFirst = rs2.getString("firstName");
                                String commentLast = rs2.getString("lastName");

                                User commentUser = new User(commentAuthorId, commentFirst, commentLast);
                                Comment comment = new Comment(commentId, commentBody, commentDate, commentUser);

                                comments.add(comment);
                            }
                        }
                    }
                    boolean isHearted = isPostLikedByUser(userId, postId);
                    boolean isBookmarked = isPostBookmarkedByUser(userId, postId);
                    ExpandedPost expandedPost = new ExpandedPost(
                        postId,
                        content,
                        postDate,
                        author,
                        heartsCount,
                        comments.size(),
                        isHearted,
                        isBookmarked,
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
        final String sql = """
            INSERT INTO comments (postId, authorId, body)
            VALUES (?, ?, ?)
        """;
        System.out.println("Adding comment to postId: " + postId + " by authorId: " + authorId);
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
            INSERT INTO likes (userId, postId, createdAt)
            VALUES (?, ?, NOW())
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
            INSERT INTO bookmarks (userId, postId, createdAt)
            VALUES (?, ?, NOW())
        """;

        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            pstmt.setString(1, userId);
            pstmt.setString(2, postId);
            pstmt.executeUpdate();
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            // when user already bookmarked post
            System.out.println(e.getMessage());
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

    public boolean isPostLikedByUser(String userId, String postId) throws SQLException {
        final String sql = "SELECT 1 FROM likes WHERE userId = ? AND postId = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean isPostBookmarkedByUser(String userId, String postId) throws SQLException {
        final String sql = "SELECT 1 FROM bookmarks WHERE userId = ? AND postId = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Returns posts bookmarked by the given user (for bookmarks page).
     */
    public List<Post> getBookmarkedPosts(String userId) throws SQLException {
        List<Post> output = new ArrayList<>();

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
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String authorId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    String postDate = rs.getString("postDate");
                    String postId = rs.getString("postId");
                    String content = rs.getString("content");
                    int heartsCount = rs.getInt("heartsCount");
                    int commentsCount = rs.getInt("commentsCount");

                    User author = new User(authorId, firstName, lastName);
                    boolean isHearted = isPostLikedByUser(userId, postId);
                    boolean isBookmarked = true; // since these are bookmarks for this user
                    Post post = new Post(postId, content, postDate, author, heartsCount, commentsCount, isHearted, isBookmarked);
                    output.add(post);
                }
            }
        }

        return output;
    }

}
