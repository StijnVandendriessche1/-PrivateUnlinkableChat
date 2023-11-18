package server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

public class ChatServer implements interfaces.BulletinBoard
{
    int n = 10; // number of cells
    ArrayList<ArrayList<Map<String, String>>> messages = new ArrayList<ArrayList<Map<String, String>>>(n);
    @Override
    public void add(int i, String v, String t) throws RemoteException
    {
        
    }

    @Override
    public String get(int i, String t) throws RemoteException {
        return null;
    }
}
