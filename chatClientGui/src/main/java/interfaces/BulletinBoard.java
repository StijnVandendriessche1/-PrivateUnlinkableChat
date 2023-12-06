package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

public interface BulletinBoard extends Remote
{
    /**
     * @param i the index of the cell that a message should be written to, can be any, since modulo is used to determine the index
     * @param v the value of the message
     * @param t the tag of the message
     */
    void add(int i, byte[] v, String t) throws RemoteException;

    /**
     * @param i the index of the cell that a message should be read from, can be any, since modulo is used to determine the index
     * @param b the tag of the message
     * @return the value of the message
     */
    byte[] get(int i, String b) throws RemoteException, NoSuchAlgorithmException;
}