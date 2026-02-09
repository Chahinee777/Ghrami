module opgg.ghrami {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires jjwt.api;

    opens opgg.ghrami to javafx.fxml;
    opens opgg.ghrami.view to javafx.fxml;
    opens opgg.ghrami.model to javafx.base;
    
    exports opgg.ghrami;
}
