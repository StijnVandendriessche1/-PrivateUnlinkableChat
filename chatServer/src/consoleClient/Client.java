package consoleClient;

import interfaces.BulletinBoard;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Client
{
    private int id;
    private String tag;

    private MessageDigest md;
    private static Registry myRegistry;
    private static BulletinBoard server;
    private ArrayList<String> ownTags = new ArrayList<>();

    public static void main(String[] args) throws MalformedURLException, NotBoundException, RemoteException, NoSuchAlgorithmException, FileNotFoundException {
        //setup client
        Client client = new Client();
        client.initialize(); //simulate touch by reading values from file
        client.md = MessageDigest.getInstance("SHA-512");
        myRegistry = LocateRegistry.getRegistry("localhost", 1099);
        server = (BulletinBoard) myRegistry.lookup("ChatService");
        client.run();

        //random tests
        /*
        //send 25 random messages (hello world1-25)
        for (int i = 0; i < 25; i++)
            client.send("Hello World" + (i + 1));

        //reset id and tag to initial values
        client.initialize();

        //receive 25 messages
        System.out.println(client.receiveAll());
         */
    }

    private void send(String message) throws NoSuchAlgorithmException, RemoteException
    {
        //create new index and tag
        int newIndex = new Random().nextInt(Integer.MAX_VALUE); //the bounds of the max integer does not matter, since we use modulo to determine the index
        String newTag = UUID.randomUUID().toString(); //create UUID tag (unique random string)

        //hash the current tag
        md.update(tag.getBytes());
        String tagHash = getHexString(md.digest());

        //add message, newIndex and newTag to a string, seperated by unit seperator
        char unitSeperator = 0x1F;
        String u = message + unitSeperator + newIndex + unitSeperator + newTag;

        //add message to bulletin board
        server.add(id, u, tagHash);
        ownTags.add(tag);

        //update id and tag
        id = newIndex;
        tag = newTag;
    }

    private String receive() throws NoSuchAlgorithmException, RemoteException, InterruptedException {
        String u = null;
        //get message from bulletin board (optionally null)
        if(!ownTags.contains(tag))
            u = server.get(id, tag);
        //String u = server.get(5, "testTag47");
        if(u == null)
            return null;

        //split message by unit seperator
        char unitSeperator = 0x1F;
        String[] split = u.split(String.valueOf(unitSeperator));

        //update tag and id and return message
        id = Integer.parseInt(split[1]);
        tag = split[2];
        return split[0];
    }

    private ArrayList<String> receiveAll() throws NoSuchAlgorithmException, RemoteException, InterruptedException {
        ArrayList<String> out = new ArrayList<>();
        String received;
        while ((received = receive()) != null) {
            out.add(received);
        }
        return out;
    }

    public void initialize() throws FileNotFoundException {
        //read id and tag from file
        Scanner scanner = new Scanner(new File("src/consoleClient/initval"));
        id = scanner.nextInt();
        tag = scanner.next();
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
                } catch (NoSuchAlgorithmException | RemoteException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void run() throws NoSuchAlgorithmException, RemoteException {
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
