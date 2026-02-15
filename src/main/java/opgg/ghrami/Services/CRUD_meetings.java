package opgg.ghrami.Services;

import opgg.ghrami.Entites.meetings;
import opgg.ghrami.util.myDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CRUD_meetings implements InterfaceCRUD<meetings> {

    private Connection conn = myDB.getInstance().getConnection();

    @Override
    public void ajouter(meetings m) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO meetings VALUES (?,?,?,?,?,?,?,?)"
            );
            pst.setString(1, m.getMeetingId());
            pst.setString(2, m.getConnectionId());
            pst.setLong(3, m.getOrganizerId());
            pst.setString(4, m.getMeetingType());
            pst.setString(5, m.getLocation());
            pst.setTimestamp(6, m.getScheduledAt());
            pst.setInt(7, m.getDuration());
            pst.setString(8, m.getStatus());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout (meetings) : " + e.getMessage());
        }
    }

    @Override
    public void modifier(meetings m) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE meetings SET connection_id=?, organizer_id=?, meeting_type=?, location=?, scheduled_at=?, duration=?, status=? WHERE meeting_id=?"
            );
            pst.setString(1, m.getConnectionId());
            pst.setLong(2, m.getOrganizerId());
            pst.setString(3, m.getMeetingType());
            pst.setString(4, m.getLocation());
            pst.setTimestamp(5, m.getScheduledAt());
            pst.setInt(6, m.getDuration());
            pst.setString(7, m.getStatus());
            pst.setString(8, m.getMeetingId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification (meetings) : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(String id) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "DELETE FROM meetings WHERE meeting_id=?"
            );
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression (meetings) : " + e.getMessage());
        }
    }

    @Override
    public List<meetings> afficher() {
        List<meetings> list = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM meetings");

            while (rs.next()) {
                list.add(new meetings(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getLong(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getTimestamp(6),
                        rs.getInt(7),
                        rs.getString(8)
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'affichage (meetings) : " + e.getMessage());
        }
        return list;
    }
}
