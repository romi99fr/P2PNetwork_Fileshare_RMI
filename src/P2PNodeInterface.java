/* ---------------------------------------------------------------
Práctica 1.
Código fuente: NodeInterface.java
Grau Informàtica
73210823Y - Joel Romia Aribau
53395926T - Pau Francino Urdaniz
--------------------------------------------------------------- */
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public interface P2PNodeInterface {
    void registerNode(String uid, P2PNodeInterface node) throws RemoteException;
    void notifyRegister(String uid) throws RemoteException;

    void uploadFile(File file) throws IOException, NoSuchAlgorithmException;
    byte[] downloadFile(String fileHash) throws IOException;
    FileInformation getFileInfo(String fileHash) throws RemoteException;
    Map<String, FileInformation> listFiles() throws IOException, NoSuchAlgorithmException;
    void editFile(String fileHash, FileInformation fileInformation);
}
