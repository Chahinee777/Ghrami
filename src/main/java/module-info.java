module opgg.ghrami {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires jjwt.api;
    requires java.mail;
    requires java.net.http;
    requires jdk.httpserver;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    
    // iText PDF library
    requires kernel;
    requires layout;
    
    // JUnit 5 for testing
    requires org.junit.jupiter.api;
    requires org.mockito;

    opens opgg.ghrami to javafx.fxml;
    opens opgg.ghrami.view to javafx.fxml;
    opens opgg.ghrami.model to javafx.base, com.fasterxml.jackson.databind;
    opens opgg.ghrami.util;
    opens opgg.ghrami.controller;
    
    exports opgg.ghrami;
    exports opgg.ghrami.view;
    exports opgg.ghrami.model;
    exports opgg.ghrami.controller;
    exports opgg.ghrami.util;
    exports opgg.ghrami.test;
}
