package consolClient;

import interfaces.BulletinBoard;
import server.BulletinBoardImpl;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class Client
{
    private int id;
    private String tag;

    private MessageDigest md;
    private static Registry myRegistry;
    private static BulletinBoard server;

    private void send(String message) throws NoSuchAlgorithmException, RemoteException {
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

        //update id and tag
        id = newIndex;
        tag = newTag;
    }

    private String receive() throws NoSuchAlgorithmException, RemoteException
    {
        //get message from bulletin board (optionally null)
        String u = server.get(id, tag);
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
    public static void main(String[] args) throws MalformedURLException, NotBoundException, RemoteException, NoSuchAlgorithmException
    {
        //setup client
        Client client = new Client();
        client.md = MessageDigest.getInstance("SHA-512");
        myRegistry = LocateRegistry.getRegistry("localhost", 1099);
        server = (BulletinBoard) myRegistry.lookup("ChatService");

        //create initial id and tag
        client.id = 5;
        client.tag = "testTag47";

        //send 25 random messages (hello world1-25)
        for (int i = 0; i < 25; i++)
            client.send("Hello World" + (i + 1));

        //reset id and tag to initial values
        client.id = 5;
        client.tag = "testTag47";

        //receive 25 messages
        for (int i = 0; i < 25; i++)
            System.out.println(client.receive());
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
