module org.group13.chessgame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens org.group13.chessgame to javafx.fxml;
    exports org.group13.chessgame;
}