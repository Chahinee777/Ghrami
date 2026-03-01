package opgg.ghrami;

import opgg.ghrami.controller.BadgeController;
import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Badge;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.FriendshipStatus;
import opgg.ghrami.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("🚀 Ghrami User Management Demo");
        System.out.println("========================================\n");

        // Initialize Controllers
        UserController userController = new UserController();
        FriendshipController friendshipController = new FriendshipController();
        BadgeController badgeController = new BadgeController();

        // ===== USER OPERATIONS =====
        System.out.println("\n📋 USER OPERATIONS");
        System.out.println("-------------------");

        // Create a new user with profile picture
        System.out.println("➕ Creating new user with profile picture...");
        final User newUser = new User(
                "farah_khelifi",
                "farah.khelifi@ghrami.tn",
                "securePassword123",
                "biggie.png",  // Profile picture from resources/images/profile_pictures/
                "Développeuse passionnée par la tech et l'innovation",
                "Nabeul, Tunisie"
        );
        newUser.setFullName("Farah Khelifi");
        User createdUser = userController.create(newUser);

        // Find user by ID
        if (createdUser != null && createdUser.getUserId() != null) {
            System.out.println("\n🔍 Finding user by ID: " + createdUser.getUserId());
            Optional<User> foundUser = userController.findById(createdUser.getUserId());
            foundUser.ifPresent(u -> {
                System.out.println("   Found: " + u.getUsername());
                System.out.println("   Full Name: " + (u.getFullName() != null ? u.getFullName() : "N/A"));
                System.out.println("   Profile Picture: " + (u.getProfilePicture() != null ? u.getProfilePicture() : "N/A"));
            });
        } else {
            System.out.println("\n⚠️ User creation failed or user already exists");
        }

        // Find user by email
        System.out.println("\n🔍 Finding user by email: farah.khelifi@ghrami.tn");
        Optional<User> userByEmail = userController.findById(1L);
        userByEmail.ifPresent(u -> System.out.println("   Found: " + u.getUsername()));

        // Find user by ID again
        System.out.println("\n🔍 Finding user again...");
        Optional<User> foundAgain = userController.findById(newUser != null ? newUser.getUserId() : 1L);
        if (foundAgain.isPresent()) {
            System.out.println("   User found!");
            System.out.println("   User: " + foundAgain.get().getUsername());
        }

        // Update user
        if (newUser != null) {
            System.out.println("\n✏️ Updating user bio...");
            newUser.setBio("Bio mise à jour: Full-stack developer passionnée par l'IA et l'entrepreneuriat social");
            userController.update(newUser);
        }

        // Update online status
        if (newUser != null) {
            System.out.println("\n🟢 Setting user online...");
            newUser.setOnline(true);
            userController.update(newUser);
        }

        // List all users
        System.out.println("\n👥 All registered users:");
        List<User> allUsers = userController.findAll();
        allUsers.forEach(u -> System.out.println("   - " + u.getUsername() + 
            " (" + u.getEmail() + ") | " + 
            (u.getFullName() != null ? u.getFullName() : "No full name") + " | " +
            (u.isOnline() ? "🟢 Online" : "🔴 Offline")));

        // Display user count
        System.out.println("\n📊 Total users in system: " + allUsers.size());

        // ===== FRIENDSHIP OPERATIONS =====
        System.out.println("\n\n📋 FRIENDSHIP OPERATIONS");
        System.out.println("------------------------");

        // Send friend request (assuming users with IDs 1 and newUser exist)
        if (newUser != null && allUsers.size() > 1) {
            Long user1Id = allUsers.get(0).getUserId();
            Long user2Id = newUser.getUserId();

            System.out.println("\n📨 Sending friend request from User " + user1Id + " to User " + user2Id);
            Friendship friendRequest = new Friendship(user1Id, user2Id);
            friendRequest = friendshipController.create(friendRequest);

            // Update friendship status
            if (friendRequest != null) {
                System.out.println("\nAccepting friend request...");
                friendRequest.setStatus(FriendshipStatus.ACCEPTED);
                friendRequest.setAcceptedDate(java.time.LocalDateTime.now());
                friendshipController.update(friendRequest);

                // List all friendships
                System.out.println("\n👥 All friendships:");
                List<Friendship> friends = friendshipController.findAll();
                friends.forEach(fr -> {
                    String statusEmoji = fr.getStatus() == FriendshipStatus.ACCEPTED ? "✅" : 
                                       fr.getStatus() == FriendshipStatus.PENDING ? "⏳" : 
                                       fr.getStatus() == FriendshipStatus.REJECTED ? "❌" : "🚫";
                    System.out.println("   " + statusEmoji + " User " + fr.getUser1Id() + " <-> User " + fr.getUser2Id() + 
                                     " (Status: " + fr.getStatus() + ")");
                });
                
                System.out.println("\n📊 Total friendships: " + friends.size());
            }
        }

        // ===== BADGE OPERATIONS =====
        System.out.println("\n\n📋 BADGE OPERATIONS");
        System.out.println("-------------------");

        // Award badges to user
        if (newUser != null) {
            System.out.println("\n🏆 Awarding badges to " + newUser.getUsername() + ":");

            Badge badge1 = new Badge(
                    newUser.getUserId(),
                    "Premiers Pas",
                    "Profil complété avec succès"
            );
            badgeController.create(badge1);

            Badge badge2 = new Badge(
                    newUser.getUserId(),
                    "Connecteur Social",
                    "Première connexion établie"
            );
            badgeController.create(badge2);

            Badge badge3 = new Badge(
                    newUser.getUserId(),
                    "Profil Complet",
                    "Toutes les informations renseignées"
            );
            badgeController.create(badge3);

            // Display user's badges
            System.out.println("\n🏆 Badges for " + newUser.getUsername() + ":");
            List<Badge> userBadges = badgeController.findAll();
            userBadges.stream()
                .filter(b -> b.getUserId().equals(newUser.getUserId()))
                .forEach(b -> {
                    String earnedDate = b.getEarnedDate() != null ? 
                        b.getEarnedDate().toLocalDate().toString() : "N/A";
                    System.out.println("   🎖️  " + b.getName() + " - " + b.getDescription() + 
                                     " (Earned: " + earnedDate + ")");
                });

            // Count badges
            long badgeCount = userBadges.stream().filter(b -> b.getUserId().equals(newUser.getUserId())).count();
            System.out.println("\n📊 Total badges earned: " + badgeCount);
        }

        // Display all badges in system
        System.out.println("\n🏆 All badges in system:");
        List<Badge> allBadges = badgeController.findAll();
        System.out.println("   Total badges awarded: " + allBadges.size());
        allBadges.forEach(b -> System.out.println("   User " + b.getUserId() + ": " + b.getName()));

        // ===== CLEANUP (Optional - uncomment to test delete operations) =====
        /*
        System.out.println("\n\n📋 CLEANUP OPERATIONS");
        System.out.println("---------------------");
        
        if (newUser != null) {
            System.out.println("\n🗑️ Deleting badges for user...");
            badgeController.findAll().stream()
                .filter(b -> b.getUserId().equals(newUser.getUserId()))
                .forEach(b -> badgeController.delete(b.getBadgeId()));
            
            System.out.println("\n🗑️ Deleting user account...");
            userController.delete(newUser.getUserId());
        }
        */

        System.out.println("\n========================================");
        System.out.println("Demo completed successfully!");
        System.out.println("========================================\n");
    }
}
