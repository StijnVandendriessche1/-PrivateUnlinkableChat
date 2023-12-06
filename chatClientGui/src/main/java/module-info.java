module com.example.chatclientgui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.rmi;


    opens com.example.chatclientgui to javafx.fxml;
    exports com.example.chatclientgui;
}