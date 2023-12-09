package com.example.chatclientgui;

import interfaces.BulletinBoard;
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

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.*;


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
    private String username;

    private int id;
    private int idCopy;
    private String tag;
    private String tagCopy;

    private MessageDigest md;
    private static Registry myRegistry;
    private static BulletinBoard server;
    private ArrayList<String> ownTags = new ArrayList<>();
    private SecretKey key;
    private SecretKey keyCopy;

    //initialize of whole client
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        try
        {
            //KeyGenerator keyGenerator = KeyGenerator.getInstance("AES"); //create key generator for AES
            //keyGenerator.init(256); //make sure 256 key is used
            //key = keyGenerator.generateKey(); //save original key

            Path path = Paths.get("src/main/java/com/example/chatclientgui/keyfile.key");
            byte[] keyBytes = Files.readAllBytes(path);
            key = new SecretKeySpec(keyBytes, "AES");
            keyCopy = new SecretKeySpec(keyBytes, "AES");

            initStyle(); //setup the style of the gui
            bump();
            md = MessageDigest.getInstance("SHA-512");
            myRegistry = LocateRegistry.getRegistry("localhost", 1099);
            server = (BulletinBoard) myRegistry.lookup("ChatService");
            receiveAll(); //receive all messages
            listenForMessage(vboxMessages); //listen for messages
            initButtonAction(vboxMessages); //setup handle for button send
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void bump() throws FileNotFoundException {
        //read id and tag from file
        Scanner scanner = new Scanner(new File("src/main/java/com/example/chatclientgui/initval"));
        id = scanner.nextInt();
        tag = scanner.next();
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
                    try {
                        sendMessage(msgToSend);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
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

    public void sendMessage(String message) throws Exception {
        //create new index and tag
        int newIndex = new Random().nextInt(Integer.MAX_VALUE); //the bounds of the max integer does not matter, since we use modulo to determine the index
        String newTag = UUID.randomUUID().toString(); //create UUID tag (unique random string)

        //hash the current tag
        md.update(tag.getBytes());
        String tagHash = getHexString(md.digest());

        //add message, newIndex and newTag to a string, seperated by unit seperator
        char unitSeperator = 0x1F;
        String u = message + unitSeperator + newIndex + unitSeperator + newTag;
        byte[] ciphertext = encrypt(u, false);

        //add message to bulletin board
        server.add(id, ciphertext, tagHash);

        //update id and tag
        id = newIndex;
        tag = newTag;
        updateKeyWithKDF(false);
    }

    public void listenForMessage(VBox vbox)
    {
        //make thread that checks for messages
        new Thread(() -> {
            while (true) {
                try {
                    //String message = receive();
                    receiveAll();
                    //if (message != null)
                        //System.out.println(message);
                } catch (NoSuchAlgorithmException | RemoteException | InvalidAlgorithmParameterException |
                         NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException |
                         InvalidKeyException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String receive() throws Exception {
        byte[] cipherText = null;
        //get message from bulletin board (optionally null)
        cipherText = server.get(id, tag);
        //String u = server.get(5, "testTag47");
        if (cipherText == null)
            return null;

        String u = decrypt(cipherText, true);
        //split message by unit seperator
        char unitSeperator = 0x1F;
        String[] split = u.split(String.valueOf(unitSeperator));

        //update tag and id and return message
        id = Integer.parseInt(split[1]);
        tag = split[2];

        if (split[0].equals(""))
            return split[0];

        updateKeyWithKDF(true);
        return split[0];
    }

    private String decrypt(byte[] input, boolean copy) throws Exception {
        // Create an instance of the Cipher class
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Extract the initialization vector from the input
        byte[] initializationVector = Arrays.copyOfRange(input, 0, 16);

        // Extract the actual cipher text from the input
        byte[] cipherText = Arrays.copyOfRange(input, 16, input.length);

        // Initialize the cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, keyCopy, new IvParameterSpec(initializationVector));

        // Decrypt the cipher text
        byte[] decryptedText = cipher.doFinal(cipherText);

        // Convert the decrypted byte array back to a string

        return new String(decryptedText);
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

    private byte[] encrypt(String u, boolean copy) throws Exception {
        // Create an instance of the Cipher class
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Initialize the cipher for encryption
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // Encrypt the text
        byte[] cipherText = cipher.doFinal(u.getBytes());

        // Get the initialization vector
        byte[] initializationVector = cipher.getIV();

        // Prepend the IV to the ciphertext
        byte[] output = new byte[initializationVector.length + cipherText.length];
        System.arraycopy(initializationVector, 0, output, 0, initializationVector.length);
        System.arraycopy(cipherText, 0, output, initializationVector.length, cipherText.length);

        return output;
    }

    private static String getHexString(byte[] bytes)
    {
        String hexString = "";
        for (byte b : bytes) {
            hexString += String.format("%02x", b);
        }
        return hexString;
    }

    public void updateKeyWithKDF(boolean copy) throws Exception {
        // Convert the SecretKey to a byte array
        byte[] password;
        if (copy) password = this.keyCopy.getEncoded();
        else password = this.key.getEncoded();

        // Convert the id to a byte array
        byte[] idBytes = ByteBuffer.allocate(4).putInt(this.id).array();

        // Concatenate the key byte array and the id byte array
        byte[] passwordWithId = new byte[password.length + idBytes.length];
        System.arraycopy(password, 0, passwordWithId, 0, password.length);
        System.arraycopy(idBytes, 0, passwordWithId, password.length, idBytes.length);

        // Use the concatenated byte array as the password for the KDF
        KeySpec spec = new PBEKeySpec(Arrays.toString(passwordWithId).toCharArray(), new byte[16], 65536, 256);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.SecretKey derivedKey = factory.generateSecret(spec);
        byte[] derivedKeyEncoded = derivedKey.getEncoded();

        // Clear the password array for security
        Arrays.fill(passwordWithId, (byte) 0);

        // Update the SecretKey with the derived key
        if (copy) this.keyCopy = new javax.crypto.spec.SecretKeySpec(derivedKeyEncoded, "AES");
        else this.key = new javax.crypto.spec.SecretKeySpec(derivedKeyEncoded, "AES");

        // Increment the id
        this.id++;
    }

    private void receiveAll() throws Exception {
        String received;
        while ((received = receive()) != null)
        {
            addMessage(received, serverCheck(received), vboxMessages);
        }
    }
}