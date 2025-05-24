module org.group13.chessgame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires javafx.media;

    opens org.group13.chessgame to javafx.fxml, javafx.graphics;
    opens org.group13.chessgame.controller to javafx.fxml;
    exports org.group13.chessgame;
}