-- Purpose: Retrieve user information to authenticate it during login
\\ http://localhost:8081/login

SELECT * 
FROM user 
WHERE username = ?;


-- Purpose: Register a new user with username, password, first name and Last name
-- and inserts into user table 
\\ http://localhost:8081/register

INSERT INTO user (username, password, firstName, lastName) 
VALUES (?, ?, ?, ?);


-- Purpose: Retrieve all users in the platform so the current user can follow them
\\ http://localhost:8081/people

SELECT u.userId, u.firstName, u.lastName, 
       DATE_FORMAT(MAX(p.createdAt), '%b %d, %Y, %l:%i %p') AS lastPostDate,
       EXISTS (
           SELECT 1
           FROM follows f
           WHERE f.userId = ?
             AND f.useridFollowed = u.userId
       ) AS isFollowed
FROM user u
LEFT JOIN posts p ON p.authorId = u.userId
WHERE u.userId != ?
GROUP BY u.userId, u.firstName, u.lastName
ORDER BY lastPostDate DESC;

-- Purpose: Checks if the current user is following another user 
-- and change the follow status with respect to another user
\\ http://localhost:8081/people
\\ http://localhost:8081/people?success=User+followed

SELECT 1 
FROM follows 
WHERE userId = ? 
  AND userIdFollowed = ?;

-- Purpose: Follow another user
-- and inserts into follows table
\\ http://localhost:8081/people
\\ http://localhost:8081/people?success=User+followed

INSERT INTO follows (userId, userIdFollowed)
VALUES (?, ?);

-- Purpose: Unfollow a user
-- and removes from follows table
\\ http://localhost:8081/people
\\ http://localhost:8081/people?success=User+followed

DELETE FROM follows
WHERE userId = ? 
  AND userIdFollowed = ?;

-- Purpose: Retrieve and display posts from other users that the current user is following
-- on home page
\\ http://localhost:8081/

SELECT p.postId, p.body AS content,
  DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
  u.userId, u.firstName, u.lastName,
  (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount,
  (SELECT COUNT(*) FROM comments c WHERE c.postId = p.postId) AS commentsCount
FROM posts p
JOIN user u ON u.userId = p.authorId
JOIN follows f ON f.userIdFollowed = p.authorId
WHERE f.userId = ?
ORDER BY p.createdAt DESC;

-- Purpose: Retrieve and display posts from selected user 
-- when you click on their name
\\ http://localhost:8081/profile

SELECT p.postId, p.body AS content,
  DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
  u.userId, u.firstName, u.lastName,
  (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount,
  (SELECT COUNT(*) FROM comments c WHERE c.postId = p.postId) AS commentsCount
FROM posts p
JOIN user u ON u.userId = p.authorId
WHERE u.userId = ?
ORDER BY p.createdAt DESC;

-- Purpose: Create a new post by the current user
\\ http://localhost:8081/

INSERT INTO posts (authorId, body, createdAt)
VALUES (?, ?, NOW());

-- Purpose: Retrieve posts containing a specific hashtag
\\  http://localhost:8081/hashtagsearch?hashtags={tag}

INSERT INTO hashtags (postId, tag)
VALUES (?, ?);

-- Purpose: Expands a specific post with all its comments and displays it
\\  http://localhost:8081/profile/{postId}

SELECT p.postId, p.body AS content,
  DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate,
  u.userId, u.firstName, u.lastName,
  (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount
FROM posts p
JOIN user u ON p.authorId = u.userId
WHERE p.postId = ?;

-- Purpose: Retrieve all comments for a specific post
\\  http://localhost:8081/profile/{postId}

SELECT c.commentId, c.body AS content, c.createdAt AS commentDate,
       u.userId, u.firstName, u.lastName
FROM comments c, user u
WHERE c.postId = ?
  AND c.authorId = u.userId
ORDER BY c.createdAt ASC;

-- Purpose: Add a comment to a specific post 
-- and inserts into comments table 
\\  http://localhost:8081/profile/{postId}

INSERT INTO comments (postId, authorId, body)
VALUES (?, ?, ?);

-- Purpose: Like a specific post
-- and inserts into likes table 
\\  http://localhost:8081/profile/{postId}

INSERT INTO likes (userId, postId, createdAt)
VALUES (?, ?, NOW());

-- Purpose: Unlike a specific post
-- and removes from likes table 
\\  http://localhost:8081/profile/{postId}

DELETE FROM likes
WHERE userId = ?
  AND postId = ?;

-- Purpose: Retrieve all bookmarked posts for the current user
\\  http://localhost:8081/bookmarks

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

-- Purpose: Bookmark a specific post
-- and inserts into bookmarks table 
\\  http://localhost:8081/profile/{postId}

INSERT INTO bookmarks (userId, postId, createdAt)
VALUES (?, ?, NOW());


-- Purpose: Remove bookmark from a specific post
-- and removes from bookmarks table 
\\  http://localhost:8081/profile/{postId}

DELETE FROM bookmarks
WHERE userId = ?
  AND postId = ?;

-- Purpose: Check if the current user has liked a specific post
\\  http://localhost:8081/profile/{postId}

SELECT 1 
FROM likes 
WHERE userId = ? 
  AND postId = ? 
LIMIT 1;

-- Purpose: Check if the current user has bookmarked a specific post
\\  http://localhost:8081/profile/{postId}

SELECT 1 
FROM bookmarks 
WHERE userId = ? 
  AND postId = ? 
LIMIT 1;

-- Purpose: Check if the specified hashtag exists in the hashtags table
\\ http://localhost:8081/hashtagsearch?hashtags=

SELECT 1 
FROM hashtags 
WHERE tag = ? LIMIT 1;

-- Purpose: Retrieve posts that contain any of the specified hashtags
\\ http://localhost:8081/hashtagsearch?hashtags=

SELECT p.postId, p.body AS content, 
DATE_FORMAT(p.createdAt, '%b %d, %Y, %l:%i %p') AS postDate, 
  u.userId, u.firstName, u.lastName, 
  (SELECT COUNT(*) FROM likes l WHERE l.postId = p.postId) AS heartsCount, 
  (SELECT COUNT(*) FROM comments c WHERE c.postId = p.postId) AS commentsCount 
FROM posts p 
JOIN user u ON u.userId = p.authorId 
JOIN hashtags h ON h.postId = p.postId 
WHERE h.tag IN (" + inClause.toString() + ")
GROUP BY p.postId
ORDER BY p.createdAt DESC