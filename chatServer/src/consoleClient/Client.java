package consoleClient;

import interfaces.BulletinBoard;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
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
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;

public class Client {
    private int id;
    private String tag;

    private MessageDigest md;
    private static Registry myRegistry;
    private static BulletinBoard server;
    private String nextTag;
    private SecretKey originalKey;
    private SecretKey key;

    private ArrayList<SecretKey> keys;

    public static void main(String[] args) throws Exception {
        //setup client
        Client client = new Client();

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES"); //create key generator for AES
        keyGenerator.init(256); //make sure 256 key is used
        client.originalKey = keyGenerator.generateKey(); //save original key

        client.initialize(); //simulate touch by reading values from file
        //todo add salt to hash as expansion?
        client.md = MessageDigest.getInstance("SHA-512");
        myRegistry = LocateRegistry.getRegistry("localhost", 1099);
        server = (BulletinBoard) myRegistry.lookup("ChatService");

        //client.run();

        //random tests
        //
        //send 25 random messages (hello world1-25)
        client.send("Hallo dit is een test");
        client.send("Dit is een test voor de kdf");

        //reset id and tag to initial values
        client.initialize();

        System.out.println(client.receive()); //receive 1 message
        System.out.println(client.receive()); //receive 1 message

        //receive 25 messages
        new Thread(new Runnable() {
            @Override
            public void run() {
            }
        }).start();
        System.out.println(client.receive());

    }

    private void send(String message) throws Exception {
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
        keys.add(key);
        updateKeyWithKDF();
    }

    private byte[] encrypt(String u) throws Exception {
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

    private String decrypt(byte[] input) throws Exception {
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

    public void updateKeyWithKDF() throws Exception {
        // Convert the SecretKey to a byte array
        byte[] password = this.key.getEncoded();

        // Convert the id to a byte array
        byte[] idBytes = ByteBuffer.allocate(4).putInt(this.id).array();

        // Concatenate the key byte array and the id byte array
        byte[] passwordWithId = new byte[password.length + idBytes.length];
        System.arraycopy(password, 0, passwordWithId, 0, password.length);
        System.arraycopy(idBytes, 0, passwordWithId, password.length, idBytes.length);

        // Use the concatenated byte array as the password for the KDF
        KeySpec spec = new PBEKeySpec(Arrays.toString(passwordWithId).toCharArray(), new byte[16], 65536, 256);

        //PBKDF2WithHmacSHA256 research ppt
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.SecretKey derivedKey = factory.generateSecret(spec);
        byte[] derivedKeyEncoded = derivedKey.getEncoded();

        // Clear the password array for security
        Arrays.fill(passwordWithId, (byte) 0);

        // Update the SecretKey with the derived key
        this.key = new javax.crypto.spec.SecretKeySpec(derivedKeyEncoded, "AES");

        // Increment the id
        this.id++;
    }

    private String receive() throws Exception {
        byte[] cipherText = null;
        //get message from bulletin board (optionally null)
        cipherText = server.get(id, tag);
        //String u = server.get(5, "testTag47");
        if (cipherText == null)
            return null;

        //todo decryption
        String u = decrypt(cipherText);
        //split message by unit seperator
        char unitSeperator = 0x1F;
        String[] split = u.split(String.valueOf(unitSeperator));

        //update tag and id and return message
        id = Integer.parseInt(split[1]);
        tag = split[2];

        updateKeyWithKDF();

        return split[0];
    }

    private ArrayList<String> receiveAll() throws Exception {
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
    public static void getNextMessage() {

    }

    public void initialize() throws FileNotFoundException, NoSuchAlgorithmException {
        //todo read id and tag from file
        this.id = 9771448;
        this.tag = "be58ba9a-4a97-41c8-9b5c-2fb11b14122b"; // uuid
        this.key = originalKey;
        this.keys = new ArrayList<>();
    }

    //TODO make this not overload the server
    //check for messages every 100ms
    private void checkForMessages() {
        //make thread that checks for messages
        new Thread(() -> {
            while (true) {
                try {
                    String message = receive();
                    if (message != null)
                        System.out.println(message);
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

    private void run() throws Exception {
        //make loop that waits for userinput and sends it
        checkForMessages();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            send(scanner.nextLine());
        }
    }

    private static String getHexString(byte[] bytes) {
        String hexString = "";
        for (byte b : bytes) {
            hexString += String.format("%02x", b);
        }
        return hexString;
    }
}
