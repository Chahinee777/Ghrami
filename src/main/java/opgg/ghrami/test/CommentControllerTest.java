package opgg.ghrami.test;

import opgg.ghrami.controller.CommentController;
import opgg.ghrami.controller.PostController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Comment;
import opgg.ghrami.model.Post;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommentControllerTest {
    
    private CommentController commentController;
    private PostController postController;
    private UserController userController;
    
    private User testUser1;
    private User testUser2;
    private User testUser3;
    private Post testPost1;
    private Post testPost2;
    private Comment testComment;
    
    @BeforeAll
    public void setUp() {
        commentController = CommentController.getInstance();
        postController = PostController.getInstance();
        userController = new UserController();
        
        // Create test users
        long timestamp = System.currentTimeMillis();
        
        testUser1 = new User();
        testUser1.setUsername("commuser1_" + timestamp);
        testUser1.setEmail("commuser1_" + timestamp + "@test.com");
        testUser1.setPassword("password123");
        testUser1.setFullName("Comment Test User 1");
        testUser1.setLocation("Tunis");
        testUser1 = userController.create(testUser1);
        
        testUser2 = new User();
        testUser2.setUsername("commuser2_" + timestamp);
        testUser2.setEmail("commuser2_" + timestamp + "@test.com");
        testUser2.setPassword("password123");
        testUser2.setFullName("Comment Test User 2");
        testUser2.setLocation("Sfax");
        testUser2 = userController.create(testUser2);
        
        testUser3 = new User();
        testUser3.setUsername("commuser3_" + timestamp);
        testUser3.setEmail("commuser3_" + timestamp + "@test.com");
        testUser3.setPassword("password123");
        testUser3.setFullName("Comment Test User 3");
        testUser3.setLocation("Sousse");
        testUser3 = userController.create(testUser3);
        
        assertNotNull(testUser1);
        assertNotNull(testUser2);
        assertNotNull(testUser3);
        
        // Create test posts
        testPost1 = new Post(testUser1.getUserId(), "Test post for comments");
        testPost1 = postController.create(testPost1);
        
        testPost2 = new Post(testUser2.getUserId(), "Another test post");
        testPost2 = postController.create(testPost2);
        
        assertNotNull(testPost1);
        assertNotNull(testPost2);
    }
    
    @Test
    @Order(1)
    public void testCreateComment() {
        testComment = new Comment(
            testPost1.getPostId(),
            testUser2.getUserId(),
            "Great post! 👍"
        );
        testComment = commentController.create(testComment);
        
        assertNotNull(testComment);
        assertNotNull(testComment.getCommentId());
        assertEquals("Great post! 👍", testComment.getContent());
        assertEquals(testPost1.getPostId(), testComment.getPostId());
        assertEquals(testUser2.getUserId(), testComment.getUserId());
        assertNotNull(testComment.getCreatedAt());
    }
    
    @Test
    @Order(2)
    public void testFindCommentById() {
        Optional<Comment> found = commentController.findById(testComment.getCommentId());
        
        assertTrue(found.isPresent());
        assertEquals(testComment.getCommentId(), found.get().getCommentId());
        assertEquals("Great post! 👍", found.get().getContent());
        assertEquals(testUser2.getFullName(), found.get().getAuthorName());
        assertNotNull(found.get().getAuthorProfilePicture());
    }
    
    @Test
    @Order(3)
    public void testAddMultipleComments() {
        Comment comment2 = new Comment(
            testPost1.getPostId(),
            testUser3.getUserId(),
            "I agree! Very interesting 😊"
        );
        comment2 = commentController.create(comment2);
        assertNotNull(comment2);
        
        Comment comment3 = new Comment(
            testPost1.getPostId(),
            testUser1.getUserId(),
            "Thanks everyone! 🙏"
        );
        comment3 = commentController.create(comment3);
        assertNotNull(comment3);
    }
    
    @Test
    @Order(4)
    public void testFindCommentsByPostId() {
        List<Comment> comments = commentController.findByPostId(testPost1.getPostId());
        
        assertNotNull(comments);
        assertTrue(comments.size() >= 3);
        
        // Verify all comments belong to the post
        for (Comment comment : comments) {
            assertEquals(testPost1.getPostId(), comment.getPostId());
        }
        
        // Verify chronological order (ASC)
        if (comments.size() > 1) {
            assertTrue(
                comments.get(0).getCreatedAt().isBefore(comments.get(1).getCreatedAt()) ||
                comments.get(0).getCreatedAt().isEqual(comments.get(1).getCreatedAt())
            );
        }
    }
    
    @Test
    @Order(5)
    public void testFindCommentsByUserId() {
        List<Comment> user2Comments = commentController.findByUserId(testUser2.getUserId());
        
        assertNotNull(user2Comments);
        assertTrue(user2Comments.size() >= 1);
        
        for (Comment comment : user2Comments) {
            assertEquals(testUser2.getUserId(), comment.getUserId());
        }
    }
    
    @Test
    @Order(6)
    public void testCommentsOnDifferentPosts() {
        Comment commentOnPost2 = new Comment(
            testPost2.getPostId(),
            testUser1.getUserId(),
            "Nice work on this post!"
        );
        commentOnPost2 = commentController.create(commentOnPost2);
        assertNotNull(commentOnPost2);
        
        Comment anotherComment = new Comment(
            testPost2.getPostId(),
            testUser3.getUserId(),
            "Very informative 📚"
        );
        anotherComment = commentController.create(anotherComment);
        assertNotNull(anotherComment);
        
        List<Comment> post2Comments = commentController.findByPostId(testPost2.getPostId());
        assertTrue(post2Comments.size() >= 2);
    }
    
    @Test
    @Order(7)
    public void testCountCommentsByPostId() {
        int count1 = commentController.countByPostId(testPost1.getPostId());
        assertTrue(count1 >= 3);
        
        int count2 = commentController.countByPostId(testPost2.getPostId());
        assertTrue(count2 >= 2);
    }
    
    @Test
    @Order(8)
    public void testUpdateComment() {
        String updatedContent = "Updated comment text! ✏️";
        testComment.setContent(updatedContent);
        
        Comment updated = commentController.update(testComment);
        
        assertNotNull(updated);
        assertEquals(updatedContent, updated.getContent());
        
        // Verify from database
        Optional<Comment> found = commentController.findById(testComment.getCommentId());
        assertTrue(found.isPresent());
        assertEquals(updatedContent, found.get().getContent());
    }
    
    @Test
    @Order(9)
    public void testCountTotalComments() {
        int totalCount = commentController.countComments();
        assertTrue(totalCount >= 5);
    }
    
    @Test
    @Order(10)
    public void testCommentWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longContent.append("This is a very long comment. ");
        }
        
        Comment longComment = new Comment(
            testPost1.getPostId(),
            testUser2.getUserId(),
            longContent.toString()
        );
        longComment = commentController.create(longComment);
        
        assertNotNull(longComment);
        assertEquals(longContent.toString(), longComment.getContent());
    }
    
    @Test
    @Order(11)
    public void testCommentWithEmojis() {
        Comment emojiComment = new Comment(
            testPost1.getPostId(),
            testUser3.getUserId(),
            "😀😁😂🤣😃😄😅😆😉😊😋😎😍😘🥰"
        );
        emojiComment = commentController.create(emojiComment);
        
        assertNotNull(emojiComment);
        assertTrue(emojiComment.getContent().contains("😀"));
    }
    
    @Test
    @Order(12)
    public void testMultipleCommentsFromSameUser() {
        for (int i = 1; i <= 3; i++) {
            Comment comment = new Comment(
                testPost2.getPostId(),
                testUser1.getUserId(),
                "Comment number " + i
            );
            Comment created = commentController.create(comment);
            assertNotNull(created);
        }
        
        List<Comment> user1Comments = commentController.findByUserId(testUser1.getUserId());
        assertTrue(user1Comments.size() >= 4);
    }
    
    @Test
    @Order(13)
    public void testDeleteComment() {
        Comment tempComment = new Comment(
            testPost1.getPostId(),
            testUser1.getUserId(),
            "Temporary comment for deletion"
        );
        tempComment = commentController.create(tempComment);
        assertNotNull(tempComment);
        
        Long tempCommentId = tempComment.getCommentId();
        boolean deleted = commentController.delete(tempCommentId);
        assertTrue(deleted);
        
        Optional<Comment> found = commentController.findById(tempCommentId);
        assertFalse(found.isPresent());
    }
    
    @Test
    @Order(14)
    public void testFindNonExistentComment() {
        Optional<Comment> found = commentController.findById(999999999L);
        assertFalse(found.isPresent());
    }
    
    @Test
    @Order(15)
    public void testDeleteNonExistentComment() {
        boolean deleted = commentController.delete(999999999L);
        assertFalse(deleted);
    }
    
    @Test
    @Order(16)
    public void testCommentsDeletedWhenPostDeleted() {
        // Create a new post with comments
        Post tempPost = new Post(testUser1.getUserId(), "Temporary post");
        tempPost = postController.create(tempPost);
        
        Comment comment1 = new Comment(tempPost.getPostId(), testUser2.getUserId(), "Comment 1");
        comment1 = commentController.create(comment1);
        
        Comment comment2 = new Comment(tempPost.getPostId(), testUser3.getUserId(), "Comment 2");
        comment2 = commentController.create(comment2);
        
        int commentsBefore = commentController.countByPostId(tempPost.getPostId());
        assertEquals(2, commentsBefore);
        
        // Delete post (should cascade delete comments)
        postController.delete(tempPost.getPostId());
        
        // Verify comments are deleted
        Optional<Comment> found1 = commentController.findById(comment1.getCommentId());
        Optional<Comment> found2 = commentController.findById(comment2.getCommentId());
        assertFalse(found1.isPresent());
        assertFalse(found2.isPresent());
    }
    
    @AfterAll
    public void tearDown() {
        // Delete test users (cascade will handle posts and comments)
        if (testUser1 != null && testUser1.getUserId() != null) {
            userController.delete(testUser1.getUserId());
        }
        if (testUser2 != null && testUser2.getUserId() != null) {
            userController.delete(testUser2.getUserId());
        }
        if (testUser3 != null && testUser3.getUserId() != null) {
            userController.delete(testUser3.getUserId());
        }
    }
}
