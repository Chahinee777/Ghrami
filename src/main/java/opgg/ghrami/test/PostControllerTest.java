package opgg.ghrami.test;
import opgg.ghrami.controller.PostController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Post;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostControllerTest {
    
    private PostController postController;
    private UserController userController;
    
    private User testUser1;
    private User testUser2;
    private User testUser3;
    private Post testPost;
    
    @BeforeAll
    public void setUp() {
        postController = PostController.getInstance();
        userController = new UserController();
        
        // Create test users with unique identifiers
        long timestamp = System.currentTimeMillis();
        
        testUser1 = new User();
        testUser1.setUsername("postuser1_" + timestamp);
        testUser1.setEmail("postuser1_" + timestamp + "@test.com");
        testUser1.setPassword("password123");
        testUser1.setFullName("Post Test User 1");
        testUser1.setLocation("Tunis");
        testUser1.setBio("Test user for posts");
        testUser1 = userController.create(testUser1);
        
        testUser2 = new User();
        testUser2.setUsername("postuser2_" + timestamp);
        testUser2.setEmail("postuser2_" + timestamp + "@test.com");
        testUser2.setPassword("password123");
        testUser2.setFullName("Post Test User 2");
        testUser2.setLocation("Sfax");
        testUser2.setBio("Another test user");
        testUser2 = userController.create(testUser2);
        
        testUser3 = new User();
        testUser3.setUsername("postuser3_" + timestamp);
        testUser3.setEmail("postuser3_" + timestamp + "@test.com");
        testUser3.setPassword("password123");
        testUser3.setFullName("Post Test User 3");
        testUser3.setLocation("Sousse");
        testUser3.setBio("Third test user");
        testUser3 = userController.create(testUser3);
        
        assertNotNull(testUser1);
        assertNotNull(testUser2);
        assertNotNull(testUser3);
        assertNotNull(testUser1.getUserId());
        assertNotNull(testUser2.getUserId());
        assertNotNull(testUser3.getUserId());
    }
    
    @Test
    @Order(1)
    public void testCreatePost() {
        testPost = new Post(testUser1.getUserId(), "My first test post! 🎉");
        testPost = postController.create(testPost);
        
        assertNotNull(testPost);
        assertNotNull(testPost.getPostId());
        assertEquals("My first test post! 🎉", testPost.getContent());
        assertEquals(testUser1.getUserId(), testPost.getUserId());
        assertNotNull(testPost.getCreatedAt());
    }
    
    @Test
    @Order(2)
    public void testFindPostById() {
        Optional<Post> found = postController.findById(testPost.getPostId());
        
        assertTrue(found.isPresent());
        assertEquals(testPost.getPostId(), found.get().getPostId());
        assertEquals("My first test post! 🎉", found.get().getContent());
        assertEquals(testUser1.getFullName(), found.get().getAuthorName());
        assertNotNull(found.get().getAuthorProfilePicture());
    }
    
    @Test
    @Order(3)
    public void testCreatePostWithImage() {
        Post postWithImage = new Post(
            testUser1.getUserId(),
            "Check out this amazing photo!",
            "test_image.jpg"
        );
        postWithImage = postController.create(postWithImage);
        
        assertNotNull(postWithImage);
        assertNotNull(postWithImage.getPostId());
        assertEquals("test_image.jpg", postWithImage.getImageUrl());
    }
    
    @Test
    @Order(4)
    public void testFindPostsByUserId() {
        List<Post> userPosts = postController.findByUserId(testUser1.getUserId());
        
        assertNotNull(userPosts);
        assertTrue(userPosts.size() >= 2);
        
        for (Post post : userPosts) {
            assertEquals(testUser1.getUserId(), post.getUserId());
        }
    }
    
    @Test
    @Order(5)
    public void testCreatePostsForMultipleUsers() {
        Post post2 = new Post(testUser2.getUserId(), "Post from user 2");
        post2 = postController.create(post2);
        assertNotNull(post2);
        
        Post post3 = new Post(testUser3.getUserId(), "Post from user 3");
        post3 = postController.create(post3);
        assertNotNull(post3);
        
        List<Post> user2Posts = postController.findByUserId(testUser2.getUserId());
        assertEquals(1, user2Posts.size());
        
        List<Post> user3Posts = postController.findByUserId(testUser3.getUserId());
        assertEquals(1, user3Posts.size());
    }
    
    @Test
    @Order(6)
    public void testFindAllPosts() {
        List<Post> allPosts = postController.findAll();
        
        assertNotNull(allPosts);
        assertTrue(allPosts.size() >= 4);
        
        // Verify posts are ordered by created_at DESC (most recent first)
        if (allPosts.size() > 1) {
            LocalDateTime first = allPosts.get(0).getCreatedAt();
            LocalDateTime second = allPosts.get(1).getCreatedAt();
            assertTrue(first.isAfter(second) || first.isEqual(second));
        }
    }
    
    @Test
    @Order(7)
    public void testUpdatePost() {
        String updatedContent = "Updated post content! 🔥";
        testPost.setContent(updatedContent);
        testPost.setImageUrl("updated_image.jpg");
        
        Post updated = postController.update(testPost);
        
        assertNotNull(updated);
        assertEquals(updatedContent, updated.getContent());
        assertEquals("updated_image.jpg", updated.getImageUrl());
        
        // Verify from database
        Optional<Post> found = postController.findById(testPost.getPostId());
        assertTrue(found.isPresent());
        assertEquals(updatedContent, found.get().getContent());
    }
    
    @Test
    @Order(8)
    public void testCountPosts() {
        int totalCount = postController.countPosts();
        assertTrue(totalCount >= 4);
    }
    
    @Test
    @Order(9)
    public void testCountPostsByUser() {
        int user1Count = postController.countPostsByUser(testUser1.getUserId());
        assertTrue(user1Count >= 2);
        
        int user2Count = postController.countPostsByUser(testUser2.getUserId());
        assertEquals(1, user2Count);
    }
    
    @Test
    @Order(10)
    public void testCreateMultiplePostsSameUser() {
        for (int i = 1; i <= 3; i++) {
            Post post = new Post(
                testUser2.getUserId(),
                "Post number " + i + " from user 2"
            );
            Post created = postController.create(post);
            assertNotNull(created);
        }
        
        List<Post> user2Posts = postController.findByUserId(testUser2.getUserId());
        assertTrue(user2Posts.size() >= 4);
    }
    
    @Test
    @Order(11)
    public void testGetFeedForUser() {
        // Create posts from different users
        List<Post> feed = postController.getFeedForUser(testUser1.getUserId());
        
        assertNotNull(feed);
        assertTrue(feed.size() >= 2); // At least user1's posts
        
        // Verify feed contains user's own posts
        boolean hasOwnPost = feed.stream()
            .anyMatch(p -> p.getUserId().equals(testUser1.getUserId()));
        assertTrue(hasOwnPost);
    }
    
    @Test
    @Order(12)
    public void testPostWithEmptyContent() {
        Post emptyPost = new Post(testUser1.getUserId(), "");
        Post created = postController.create(emptyPost);
        
        // Should still create (database allows it)
        assertNotNull(created);
    }
    
    @Test
    @Order(13)
    public void testPostWithLongContent() {
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("Long content test. ");
        }
        
        Post longPost = new Post(testUser3.getUserId(), longContent.toString());
        Post created = postController.create(longPost);
        
        assertNotNull(created);
        assertEquals(longContent.toString(), created.getContent());
    }
    
    @Test
    @Order(14)
    public void testDeletePost() {
        Post tempPost = new Post(testUser1.getUserId(), "Temporary post for deletion");
        tempPost = postController.create(tempPost);
        assertNotNull(tempPost);
        
        Long tempPostId = tempPost.getPostId();
        boolean deleted = postController.delete(tempPostId);
        assertTrue(deleted);
        
        Optional<Post> found = postController.findById(tempPostId);
        assertFalse(found.isPresent());
    }
    
    @Test
    @Order(15)
    public void testFindNonExistentPost() {
        Optional<Post> found = postController.findById(999999999L);
        assertFalse(found.isPresent());
    }
    
    @Test
    @Order(16)
    public void testDeleteNonExistentPost() {
        boolean deleted = postController.delete(999999999L);
        assertFalse(deleted);
    }
    
    @AfterAll
    public void tearDown() {
        // Delete all test posts (cascade will handle this via user deletion)
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
