package src.main.java.consoleClient;

import interfaces.BulletinBoard;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

public class Client
{
    private int id;
    private String tag;

    private MessageDigest md;
    private static Registry myRegistry;
    private static BulletinBoard server;
    private String nextTag;
    private SecretKey key;

    public static void main(String[] args) throws MalformedURLException, NotBoundException, RemoteException, NoSuchAlgorithmException, FileNotFoundException, InterruptedException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        //setup client
        Client client = new Client();
        client.initialize(); //simulate touch by reading values from file
        //todo add salt to hash as expansion?
        client.md = MessageDigest.getInstance("SHA-512");
        myRegistry = LocateRegistry.getRegistry("localhost", 1099);
        server = (BulletinBoard) myRegistry.lookup("ChatService");

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES"); //create key generator for AES
        keyGenerator.init(256); //make sure 256 key is used
        client.key = keyGenerator.generateKey(); //create secret

        //client.run();

        //random tests
        //
        //send 25 random messages (hello world1-25)
        client.send("Hallo dit is een test");

        //reset id and tag to initial values
        client.initialize();

        System.out.println(client.receive()); //receive 1 message

        //receive 25 messages
        new Thread(new Runnable() {
            @Override
            public void run() {}
        }).start();
        System.out.println(client.receive());

    }

    private void send(String message) throws NoSuchAlgorithmException, RemoteException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        //create new index and tag
        int newIndex = new Random().nextInt(Integer.MAX_VALUE); //the bounds of the max integer does not matter, since we use modulo to determine the index
        String newTag = UUID.randomUUID().toString(); //create UUID tag (unique random string)

        //hash the current tag
        md.update(tag.getBytes());
        String tagHash = getHexString(md.digest());

        //add message, newIndex and newTag to a string, seperated by unit seperator
        char unitSeperator = 0x1F;
        String u = message + unitSeperator + newIndex + unitSeperator + newTag;
        byte[] ciphertext = encrypt(u);

        //add message to bulletin board
        server.add(id, ciphertext, tagHash);

        //update id and tag
        id = newIndex;
        tag = newTag;
    }

    private byte[] encrypt(String u) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
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

    private String decrypt(byte[] input) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        // Create an instance of the Cipher class
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Extract the initialization vector from the input
        byte[] initializationVector = Arrays.copyOfRange(input, 0, 16);

        // Extract the actual cipher text from the input
        byte[] cipherText = Arrays.copyOfRange(input, 16, input.length);

        // Initialize the cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(initializationVector));

        // Decrypt the cipher text
        byte[] decryptedText = cipher.doFinal(cipherText);

        // Convert the decrypted byte array back to a string
        return new String(decryptedText);
    }

    private String receive() throws NoSuchAlgorithmException, RemoteException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] cipherText = null;
        //get message from bulletin board (optionally null)
        cipherText = server.get(id, tag);
        //String u = server.get(5, "testTag47");
        if(cipherText == null)
            return null;

        //todo decryption
        String u = decrypt(cipherText);
        //split message by unit seperator
        char unitSeperator = 0x1F;
        String[] split = u.split(String.valueOf(unitSeperator));

        //update tag and id and return message
        id = Integer.parseInt(split[1]);
        tag = split[2];
        return split[0];
    }

    private ArrayList<String> receiveAll() throws NoSuchAlgorithmException, RemoteException, InterruptedException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        ArrayList<String> out = new ArrayList<>();
        String received;
        while ((received = receive()) != null) {
            out.add(received);
        }
        return out;
    }

    /**
     * todo implement
     */
    public static void getNextMessage(){

    }

    public void initialize() throws FileNotFoundException, NoSuchAlgorithmException {
        //todo read id and tag from file
        this.id = 9771448;
        this.tag = "be58ba9a-4a97-41c8-9b5c-2fb11b14122b"; // uuid
    }

    //TODO make this not overload the server
    //check for messages every 100ms
    private void checkForMessages()
    {
        //make thread that checks for messages
        new Thread(() -> {
            while (true)
            {
                try {
                    String message = receive();
                    if(message != null)
                        System.out.println(message);
                } catch (NoSuchAlgorithmException | RemoteException | InvalidAlgorithmParameterException |
                         NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void run() throws NoSuchAlgorithmException, RemoteException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        //make loop that waits for userinput and sends it
        checkForMessages();
        Scanner scanner = new Scanner(System.in);
        while (true)
        {
            send(scanner.nextLine());
        }
    }

    private static String getHexString(byte[] bytes)
    {
        String hexString = "";
        for (byte b : bytes) {
            hexString += String.format("%02x", b);
        }
        return hexString;
    }
}
