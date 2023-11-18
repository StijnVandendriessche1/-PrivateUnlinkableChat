module com.example.chatclientgui {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.chatclientgui to javafx.fxml;
    exports com.example.chatclientgui;
}