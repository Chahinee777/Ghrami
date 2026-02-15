package opgg.ghrami;

import opgg.ghrami.Entites.connections;
import opgg.ghrami.Services.CRUD_connections;
import opgg.ghrami.Services.InterfaceCRUD;
import opgg.ghrami.util.myDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {


        Connection conn = myDB.getInstance().getConnection();
        try {
            if (conn != null && !conn.isClosed()) {
                System.out.println("OK");
            } else {
                System.out.println("échouée");
                return;
            }
        } catch (SQLException e) {
            System.err.println(" Erreur : " + e.getMessage());
            return;
        }




        InterfaceCRUD<connections> crud = new CRUD_connections();

        connections c1 = new connections("C001",1L,2L,"professional","Java","SQL","pending");
        crud.ajouter(c1);







        List<connections> list = crud.afficher();
        for (connections c : list) {
            System.out.println(c);
        }



        connections cUpdate = new connections("C001",1L,2L,"professional","Java","Spring Boot", "accepted");

        crud.modifier(cUpdate);







        crud.supprimer("C001");
        System.out.println("🗑️ Connection supprimée (Test)");


    }
}
