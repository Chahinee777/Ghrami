package opgg.ghrami.Services;

import opgg.ghrami.Entites.meetings_participants;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CRUD_meetings_participants implements InterfaceCRUD<meetings_participants> {

    private Connection conn = DatabaseConnection.getInstance().getConnection();

    @Override
    public void ajouter(meetings_participants p) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO meetings_participants VALUES (?,?,?,?)"
            );
            pst.setString(1, p.getParticipantId());
            pst.setString(2, p.getMeetingId());
            pst.setLong(3, p.getUserId());
            pst.setBoolean(4, p.isActive());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur" + e.getMessage());
        }
    }

    @Override
    public void modifier(meetings_participants p) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE meetings_participants SET meeting_id=?, user_id=?, is_active=? WHERE participant_id=?"
            );
            pst.setString(1, p.getMeetingId());
            pst.setLong(2, p.getUserId());
            pst.setBoolean(3, p.isActive());
            pst.setString(4, p.getParticipantId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur" + e.getMessage());
        }
    }

    @Override
    public void supprimer(String id) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                    "DELETE FROM meetings_participants WHERE participant_id=?"
            );
            pst.setString(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur " + e.getMessage());
        }
    }

    @Override
    public List<meetings_participants> afficher() {
        List<meetings_participants> list = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM meetings_participants");

            while (rs.next()) {
                list.add(new meetings_participants(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getLong(3),
                        rs.getBoolean(4)
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur " + e.getMessage());
        }
        return list;
    }
}
