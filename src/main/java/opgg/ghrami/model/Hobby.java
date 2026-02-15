package opgg.ghrami.model;

public class Hobby {
    private Long hobbyId;
    private Long userId;
    private String name;
    private String category;
    private String description;

    // Constructors
    public Hobby() {
    }

    public Hobby(Long userId, String name, String category) {
        this.userId = userId;
        this.name = name;
        this.category = category;
    }

    public Hobby(Long userId, String name, String category, String description) {
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    // Getters and Setters
    public Long getHobbyId() {
        return hobbyId;
    }

    public void setHobbyId(Long hobbyId) {
        this.hobbyId = hobbyId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Hobby{" +
                "hobbyId=" + hobbyId +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
