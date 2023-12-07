package src.main.java.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class BulletinBoardImpl extends UnicastRemoteObject implements interfaces.BulletinBoard
{
    private static final int n = 10; // number of cells
    private static HashMap<String, byte[]>[] cells = new HashMap[n]; //cell array, each cell is a hashmap with tag (hashed) as key and value as value

    public BulletinBoardImpl() throws RemoteException
    {
        for(int i = 0; i < n; i++)
            cells[i] = new HashMap<>();
    }

    @Override
    public void add(int i, byte[] v, String t) throws RemoteException
    {
        BulletinBoardImpl.cells[i % n].put(t, v);
        System.out.println("Message added to cell " + i + " with tag " + t);
    }

    @Override
    public byte[] get(int i, String b) throws RemoteException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(b.getBytes());
        String t = getHexString(md.digest());
        //System.out.println("Message retrieved from cell " + i + " with tag " + t);
        return BulletinBoardImpl.cells[i % n].get(t);
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
