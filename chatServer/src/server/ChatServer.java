package server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatServer implements interfaces.BulletinBoard
{
    private static final int n = 10; // number of cells
    private static HashMap<String, String>[] cells = new HashMap[n];
    public ChatServer() throws RemoteException
    {
        for (int i = 0; i < n; i++)
        {
            cells[i] = new HashMap<String, String>();
        }
    }
    @Override
    public void add(int i, String v, String t) throws RemoteException
    {
        ChatServer.cells[i % n].put(t,v);
    }

    @Override
    public String get(int i, String t) throws RemoteException
    {
        return ChatServer.cells[i % n].get(t);
    }
}

