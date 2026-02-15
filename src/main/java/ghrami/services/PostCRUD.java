package ghrami.services;

import ghrami.model.post;
import ghrami.services.interfaceCRUD;
import ghrami.util.myDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostCRUD implements interfaceCRUD<post> {

    Connection conn;

    public PostCRUD() {
        conn = myDB.getInstance().getConnection();
    }

    @Override
    public void ajouter(post post) throws SQLException{
        String req =
                "insert into post (content, type, createdAt, likeCount, commentCount, userId) " +
                        "values('" + post.getContent() + "','" +
                        post.getType() + "','" +
                        Timestamp.valueOf(post.getCreatedAt()) + "'," +
                        post.getLikeCount() + "," +
                        post.getCommentCount() + "," +
                        post.getUserId() + ")";

        Statement st = conn.createStatement();
        st.executeUpdate(req);

        System.out.println("Post ajouté !");
    }


    @Override
    public void modifier(post post) throws SQLException{


            String req = "update post set content=?, type=? where idPost=?";

            PreparedStatement pst = conn.prepareStatement(req);
            pst.setString(1, post.getContent());
            pst.setString(2, post.getType());
            pst.setInt(3, post.getIdPost());

            pst.executeUpdate();
            System.out.println("Post modifié !");

    }


    @Override
        public void supprimer(int id) throws SQLException{


        String req = "delete from post where idPost=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();

        System.out.println("Post supprimé !");
    }

    @Override
    public List<post> afficher() throws SQLException {

        String req = "select * from post";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        List<post> listePosts = new ArrayList<>();

        while (rs.next()) {
            post p = new post();
            p.setIdPost(rs.getInt("idPost"));
            p.setContent(rs.getString("content"));
            p.setType(rs.getString("type"));
            p.setCreatedAt(rs.getTimestamp("createdAt").toLocalDateTime());
            p.setLikeCount(rs.getInt("likeCount"));
            p.setCommentCount(rs.getInt("commentCount"));
            p.setUserId(rs.getInt("userId"));

            listePosts.add(p);
        }

        return listePosts;
    }
}
