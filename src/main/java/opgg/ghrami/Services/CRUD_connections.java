package opgg.ghrami.Services;

import opgg.ghrami.Entites.connections;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CRUD_connections implements InterfaceCRUD<connections> {

    private Connection conn = DatabaseConnection.getInstance().getConnection();

    @Override
    public void ajouter(connections c) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO connections VALUES (?,?,?,?,?,?,?)"
            );
            pst.setString(1, c.getConnectionId());
            pst.setLong(2, c.getInitiatorId());
            pst.setLong(3, c.getReceiverId());
            pst.setString(4, c.getConnectionType());
            pst.setString(5, c.getReceiverSkill());
            pst.setString(6, c.getInitiatorSkill());
            pst.setString(7, c.getStatus());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @Override
    public void modifier(connections c) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE connections SET initiator_id=?, receiver_id=?, connection_type=?, receiver_skill=?, initiator_skill=?, status=? WHERE connection_id=?"
            );
            pst.setLong(1, c.getInitiatorId());
            pst.setLong(2, c.getReceiverId());
            pst.setString(3, c.getConnectionType());
            pst.setString(4, c.getReceiverSkill());
            pst.setString(5, c.getInitiatorSkill());
            pst.setString(6, c.getStatus());
            pst.setString(7, c.getConnectionId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(String id) {
        try {
            PreparedStatement pst = conn.prepareStatement("DELETE FROM connections WHERE connection_id=?");
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    @Override
    public List<connections> afficher() {
        List<connections> list = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM connections");

            while (rs.next()) {
                list.add(new connections(
                        rs.getString(1),
                        rs.getLong(2),
                        rs.getLong(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7)
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'affichage : " + e.getMessage());
        }
        return list;
    }
}
