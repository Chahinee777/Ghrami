package ghrami.services;
import ghrami.model.comment;
import ghrami.util.myDB;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentCRUD implements interfaceCRUD<comment> {

    Connection conn;

    public CommentCRUD() {
        conn = myDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(comment comment) throws SQLException {
        String req = "INSERT INTO comment (content, createdAt, likeCount, idPost, userId) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, comment.getContent());
        pst.setTimestamp(2, Timestamp.valueOf(comment.getCreatedAt()));
        pst.setInt(3, comment.getLikeCount());
        pst.setInt(4, comment.getIdPost());
        pst.setInt(5, comment.getUserId());

        pst.executeUpdate();
        System.out.println("Commentaire ajouté !");
    }

    @Override
    public void modifier(comment comment) throws SQLException {
        String req = "UPDATE comment SET content=? WHERE idComment=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, comment.getContent());
        pst.setInt(2, comment.getIdComment());
        pst.executeUpdate();
        System.out.println("Commentaire modifié !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM comment WHERE idComment=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
        System.out.println("Commentaire supprimé !");
    }

    @Override
    public List<comment> afficher() throws SQLException {
        List<comment> listeComments = new ArrayList<>();
        String req = "SELECT * FROM comment";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            comment c = new comment(); // constructeur vide
            c.setIdComment(rs.getInt("idComment"));
            c.setContent(rs.getString("content"));
            c.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
            c.setLikeCount(rs.getInt("likeCount"));
            c.setIdPost(rs.getInt("idPost"));
            c.setUserId(rs.getInt("userId"));

            listeComments.add(c);
        }


        return listeComments;
    }
}
