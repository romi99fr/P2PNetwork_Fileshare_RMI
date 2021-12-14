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
    Map<String, FileInformation> listFiles() throws RemoteException;
    Map<String, FileInformation> searchFiles() throws RemoteException;
    void editFile(String fileHash, FileInformation fileInformation);
}
