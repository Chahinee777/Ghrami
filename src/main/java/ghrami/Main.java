package ghrami;

import ghrami.model.post;
import ghrami.model.comment;
import ghrami.services.PostCRUD;
import ghrami.services.CommentCRUD;
import ghrami.util.myDB;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        System.out.printf("Hello and welcome!");

        myDB.getInstance().getConnection();

                try {
                    PostCRUD postCRUD = new PostCRUD();

                    // 1️⃣ Ajouter un post
                    post nouveauPost = new post();
                    nouveauPost.setContent("Mon premier post !");
                    nouveauPost.setType("reflection");
                    nouveauPost.setCreatedAt(LocalDateTime.now());
                    nouveauPost.setLikeCount(0);
                    nouveauPost.setCommentCount(0);
                    nouveauPost.setUserId(1); // Assure-toi que cet userId existe dans ta table user

                    postCRUD.ajouter(nouveauPost);

                    // 2️⃣ Afficher tous les posts
                    List<post> posts = postCRUD.afficher();
                    for (post p : posts) {
                        System.out.println(p.getIdPost() + " | " + p.getContent() + " | " + p.getType());
                    }


                    // 3️⃣ Modifier un post
                    if (!posts.isEmpty()) {
                        post p = posts.get(0);
                        p.setContent("Contenu modifié !");
                        p.setType("experience");
                        postCRUD.modifier(p);
                    }

                    // 4️⃣ Supprimer un post
                    if (!posts.isEmpty()) {
                        postCRUD.supprimer(posts.get(0).getIdPost());
                    }



                } catch (SQLException e) {
                    e.printStackTrace();
                }






        try {
            // 1️⃣ Créer l'instance CRUD
            CommentCRUD commentCRUD = new CommentCRUD();

            // 2️⃣ Ajouter un commentaire
            comment nouveauComment = new comment(
                    "Super post !",              // content
                    LocalDateTime.now(),         // createdAt
                    0,                           // likeCount
                    1,                           // idPost (doit exister)
                    1                            // userId (doit exister)
            );

            commentCRUD.ajouter(nouveauComment);

            // 3️⃣ Afficher tous les commentaires
            List<comment> comments = commentCRUD.afficher();
            System.out.println("---- Liste des commentaires ----");
            for (comment c : comments) {
                System.out.println(c.getIdComment() + " | " + c.getContent() + " | " + c.getIdPost() + " | " + c.getUserId());
            }

            // 4️⃣ Modifier le premier commentaire
            if (!comments.isEmpty()) {
                comment c = comments.get(0);
                c.setContent("super comment");
                commentCRUD.modifier(c);

            }

            // 5️⃣ Supprimer le premier commentaire
            if (!comments.isEmpty()) {
                commentCRUD.supprimer(comments.get(0).getIdComment());

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}



