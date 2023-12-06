package server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main
{
    public static void main(String[] args) throws AlreadyBoundException, RemoteException {
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.bind("ChatService", new src.main.java.server.BulletinBoardImpl());
        System.out.println("system is ready");
    }
}
