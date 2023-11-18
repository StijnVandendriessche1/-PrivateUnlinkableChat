package com.example.chatclientgui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class HelloController implements Initializable
{
    //GUI declaration
    @FXML
    private Label lblTitle;

    @FXML
    private Button buttonSend;

    @FXML
    private TextField tfMessage;

    @FXML
    private VBox vboxMessages;

    @FXML
    ScrollPane spMain;

    //Socket declaration
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    //initialize of whole client
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        this.username = getUsername(); //get username from TextInputDialog
        this.lblTitle.setText("SocketChat - " + this.username);
        initConnection(); //create connection with server
        initStyle(); //setup the style of the gui
        listenForMessage(vboxMessages); //listen for messages
        initButtonAction(vboxMessages); //setup handle for button send
    }

    private void initButtonAction(VBox vBox)
    {
        buttonSend.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String msgToSend = tfMessage.getText();
                if(!msgToSend.isEmpty())
                {
                    addMessage(msgToSend, MessageType.OUT, vBox);
                    sendMessage(msgToSend);
                    tfMessage.clear();
                }
            }
        });
    }

    private void addMessage(String message, MessageType messageType, VBox vBox)
    {
        if(messageType == MessageType.SERVER)
        {
            message = message.substring(7);
        }
        HBox hBox = new HBox();
        hBox.setPadding(new Insets(5,5,5,5));
        Text text = new Text(message);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setPadding(new Insets(5,10,5,10));
        if(messageType == MessageType.IN)
        {
            hBox.setAlignment(Pos.CENTER_LEFT);
            textFlow.setStyle("-fx-color: rgb(233,233,235);"
                    + "-fx-background-color: rgb(200,200,200);"
                    + "-fx-background-radius: 20px;");
        }
        else if(messageType == MessageType.OUT)
        {
            text.setFill(Color.color(0.934,0.945,0.996));
            hBox.setAlignment(Pos.CENTER_RIGHT);
            textFlow.setStyle("-fx-color: rgb(239,242,255);"
                    + "-fx-background-color: rgb(15,125,242);"
                    + "-fx-background-radius: 20px;");
        }
        else
        {
            hBox.setAlignment(Pos.CENTER);
        }
        hBox.getChildren().add(textFlow);

        Platform.runLater(new Runnable() {
            @Override
            public void run()
            {
                vBox.getChildren().add(hBox);
            }
        });
    }

    public String getUsername()
    {
        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("SocketChat username");
        textInputDialog.setHeaderText("Enter your username: ");
        textInputDialog.setContentText("Username: ");
        Optional<String> res;
        do
        {
            res = textInputDialog.showAndWait();
        }
        while (textInputDialog.getEditor().getText() == null || textInputDialog.getEditor().getText().length() == 0);
        return textInputDialog.getEditor().getText();
    }

    public void initStyle()
    {
        buttonSend.setStyle("-fx-background-color: linear-gradient(#f2f2f2, #d6d6d6), " +
                "linear-gradient(#fcfcfc 0%, #d9d9d9 20%, #d6d6d6 100%), " +
                "linear-gradient(#dddddd 0%, #f6f6f6 50%);" +
                "-fx-background-radius: 8,7,6;" +
                "-fx-background-insets: 0,1,2;" +
                "-fx-text-fill: black;" +
                "-fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 );");

        tfMessage.setStyle("-fx-accent: -fx-primary-color;" +
                "-fx-background-color: white;" +
                "-fx-background-insets: 0, 0 0 1 0;" +
                "-fx-background-radius: 0;");

        vboxMessages.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                spMain.setVvalue((Double) newValue);
            }
        });
    }

    //Server Connection part
    public void initConnection()
    {
        try
        {
            Socket socket = new Socket("localhost", 1234);
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            sendMessage(this.username);
        }
        catch (IOException e)
        {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void sendMessage(String message)
    {
        try
        {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
        catch (IOException e)
        {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void listenForMessage(VBox vbox)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (socket.isConnected())
                {
                    try
                    {
                        String messageIn;
                        messageIn = bufferedReader.readLine();
                        addMessage(messageIn, serverCheck(messageIn), vbox);
                    }catch (IOException e)
                    {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                        break;
                    }
                }
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter)
    {
        //addMessage("closing connection", MessageType.SERVER, vboxMessages);
        System.out.println("connection lost, shutting down");
        try
        {
            if(bufferedReader != null)
            {
                bufferedReader.close();
            }
            if(bufferedWriter != null)
            {
                bufferedWriter.close();
            }
            if(socket != null)
            {
                socket.close();
            }
            Platform.exit();
            System.exit(0);
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public MessageType serverCheck(String message)
    {
        String s = "";
        if(message.length() > 7)
        {
            s = message.substring(0,7);
        }
        if(s.equals("SERVER:"))
        {
            return MessageType.SERVER;
        }
        else
        {
            return MessageType.IN;
        }
    }
}