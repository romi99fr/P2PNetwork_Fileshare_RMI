/* ---------------------------------------------------------------
Práctica 1.
Código fuente: P2PNodeImplementation.java
Grau Informàtica
73210823Y - Joel Romia Aribau
53395926T - Pau Francino Urdaniz
--------------------------------------------------------------- */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class P2PNodeImplementation extends UnicastRemoteObject implements P2PNodeInterface {
    private File folderPath;
    private String uid;
    private String clientIp;
    private int clientPort;
    private P2PNodeInterface parentNode;
    private Map<String, P2PNodeInterface> networkNodes = new HashMap<>();
    private Map<String, FileInformation> networkFiles = new HashMap<>();

    protected P2PNodeImplementation(File folderPath, String uid, int clientPort) throws IOException, NoSuchAlgorithmException {
        this.folderPath = folderPath;
        this.uid = uid;
        this.clientIp = InetAddress.getLocalHost().getHostAddress();
        this.clientPort = clientPort;
        this.uploadLocalFilesToNetwork();
    }

    protected P2PNodeImplementation(File folderPath, String uid, P2PNodeInterface parentNode, int clientPort) throws IOException, NoSuchAlgorithmException {
        this.folderPath = folderPath;
        this.uid = uid;
        this.parentNode = parentNode;
        this.clientIp = InetAddress.getLocalHost().getHostAddress();
        this.clientPort = clientPort;
        this.uploadLocalFilesToNetwork();
    }

    //INTERFACE METHODS
    @Override
    public void registerNode(String uid, P2PNodeInterface node) throws RemoteException {
        synchronized (this) {
            System.out.println("Registering node with ID: " + uid);
            this.networkNodes.put(uid, node);
            this.notify();
        }
    }

    @Override
    public void notifyRegister(String uid) throws RemoteException {
        synchronized (this) {
            System.out.println("Node with ID " + uid + " has been registered in the network.");
            this.notify();
        }
    }

    @Override
    public void uploadFile(File file) throws IOException, NoSuchAlgorithmException {
        FileInformation fileInformation = new FileInformation(file, this.clientIp, this.clientPort);
        MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
        String fileHash = checksum(shaDigest, file);
        this.networkFiles.put(fileHash, fileInformation);
        Path localPath = this.folderPath.toPath().resolve(file.getName());
        Files.copy(file.toPath(), localPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public byte[] downloadFile(String fileHash) throws IOException {
        File toDownload = networkFiles.get(fileHash).getFile();
        return Files.readAllBytes(toDownload.toPath());
    }

    @Override
    public FileInformation getFileInfo(String fileHash) throws RemoteException {
        return networkFiles.get(fileHash);
    }

    @Override
    public Map<String, FileInformation> listFiles() throws IOException, NoSuchAlgorithmException {
        this.uploadLocalFilesToNetwork();
        return this.networkFiles;
    }

    @Override
    public void editFile(String fileHash, FileInformation fileInformation) {
        this.networkFiles.replace(fileHash, fileInformation);
    }

    private void uploadLocalFilesToNetwork() throws NoSuchAlgorithmException, IOException {
        for (File file : Objects.requireNonNull(folderPath.listFiles())) {
            MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
            String fileHash = checksum(shaDigest, file);
            if(!networkFiles.containsKey(fileHash)) {
                FileInformation fileInformation = new FileInformation(file, this.clientIp, this.clientPort);
                networkFiles.put(fileHash, fileInformation);
            }
        }
    }

    private String checksum(MessageDigest digest, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1)
        {
            digest.update(byteArray, 0, bytesCount);
        };

        fis.close();

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer
                    .toString((bytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }

        return sb.toString();
    }
}
