import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P2PNode {
    //Global class vars
    private static int clientPort;
    private static File folderPath;
    private static String hostIp;
    private static int hostPort;
    //Read from keyboard
    private static InputStreamReader ir = new InputStreamReader(System.in);
    private static BufferedReader br = new BufferedReader(ir);;

    public static void main(String args[]) {
        //Treat args
        String uid = UUID.randomUUID().toString();
        if(args.length == 2) {
            clientPort = Integer.parseInt(args[0]);
            folderPath = new File(args[1]);
            if(folderPath.exists() && folderPath.isDirectory()) {
                isolatedTask(clientPort, folderPath, uid);
            }
        }
        else if (args.length == 4 && isValidIPAddress(args[2])) {
            clientPort = Integer.parseInt(args[0]);
            folderPath = new File(args[1]);
            hostIp = args[2];
            hostPort = Integer.parseInt(args[3]);
            if(folderPath.exists() && folderPath.isDirectory()) {
                connectedTask(clientPort, folderPath, hostIp, hostPort, uid);
            }
        }
        else {
            System.err.println("Execute the program as:");
            System.err.println("./P2PNode client_port folder_path [host_ip] [host_port]");
        }
    }

    public static void isolatedTask(int clientPort, File folderPath, String uid) {
        try {
            P2PNodeImplementation node = new P2PNodeImplementation(folderPath, uid, clientPort);
            P2PNodeThread thread = new P2PNodeThread(clientPort, node);
            new Thread(thread).start();
            System.out.println("Starting node at port " + clientPort);
            Thread.sleep(2500);
            System.out.println("Welcome to the P2P network, choose one option:");
            fileshareActions(node);
        }
        catch(InterruptedException | IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void connectedTask(int clientPort, File folderPath, String hostIp, int hostPort, String uid) {
        try {
            Registry registry = LocateRegistry.getRegistry(hostIp, hostPort);
            P2PNodeInterface host = (P2PNodeInterface) registry.lookup("node");
            System.out.println("Connected to the host with IP: " + hostIp + " and port: " + hostPort);
            P2PNodeImplementation node = new P2PNodeImplementation(folderPath, uid, host, clientPort);
            P2PNodeThread thread = new P2PNodeThread(clientPort, node);
            new Thread(thread).start();
            System.out.println("Starting node at port " + clientPort);
            host.registerNode(uid, node);
            Thread.sleep(2500);
            System.out.println("Welcome to the P2P network, choose one option:");
            fileshareActions(node);
        }
        catch(NotBoundException | InterruptedException | IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static boolean isValidIPAddress(String ip) {
        if (ip == null) {
            return false;
        }
        String IPV4_REGEX = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);
        Matcher matcher = IPV4_PATTERN.matcher(ip);
        return matcher.matches();
    }

    public static void fileshareActions(P2PNodeInterface node) throws IOException, NoSuchAlgorithmException {
        while (true) {
            System.out.println( "1 - Upload file\n" +
                                "2 - Download file\n" +
                                "3 - List files\n" +
                                "4 - Search files\n" +
                                "5 - Edit file\n" +
                                "6 - Disconnect\n");
            int userAction = Integer.parseInt(br.readLine());
            switch (userAction) {
                case 1 -> uploadFile(node);
                case 2 -> downloadFile(node);
                case 3 -> listFiles(node);
                case 4 -> searchFiles(node);
                case 5 -> editFile(node);
                case 6 -> disconnect();
            }
        }
    }

    public static void uploadFile(P2PNodeInterface node) throws IOException, NoSuchAlgorithmException {
        System.out.println("Enter the full path of the file you want to upload:");
        String filePath = br.readLine();
        File file = new File(filePath);
        if(file.exists() && file.isFile()) {
            node.uploadFile(file);
            System.out.println("File uploaded!");
        }
        else {
            System.out.println("Invalid file path!");
        }
    }

    public static void downloadFile(P2PNodeInterface node) throws IOException {
        System.out.println("Enter the hash content of the file you want to download:");
        String fileHash = br.readLine();
        String fileName = node.getFileInfo(fileHash).getFile().getName();
        //Download file content
        byte[] fileContent = node.downloadFile(fileHash);
        //Write downloaded file to local path
        File newFile = new File(folderPath.getAbsolutePath() + "\\" + fileName);
        FileOutputStream fos = new FileOutputStream(newFile);
        fos.write(fileContent);
        fos.flush();
        fos.close();
        System.out.println("File downloaded!");
    }

    public static void listFiles(P2PNodeInterface node) throws IOException, NoSuchAlgorithmException {
        System.out.println("Listing available files on the network");
        Map<String, FileInformation> networkFiles = node.listFiles();
        for (Map.Entry<String, FileInformation> entry : networkFiles.entrySet()) {
            System.out.println("*File: " + entry.getValue().getHash());
            System.out.println("File titles: " + entry.getValue().getTitles());
            System.out.println("File keywords: " + entry.getValue().getKeywords());
            System.out.println("File descriptions: " + entry.getValue().getDescriptions());
        }
    }

    public static void searchFiles(P2PNodeInterface node) throws IOException, NoSuchAlgorithmException {
        Map<String, FileInformation> networkFiles = node.listFiles();
        List<String> resultFiles = new ArrayList<>();
        System.out.println("Select the attribute you want to search by:");
        System.out.println("1 - Hash");
        System.out.println("2 - Titles");
        System.out.println("3 - Keywords");
        System.out.println("4 - Descriptions");
        int attribute = Integer.parseInt(br.readLine());
        String value = "";
        switch (attribute) {
            case 1 -> {
                System.out.println("Enter the hash value you want to search for:");
                value = br.readLine();
                for (Map.Entry<String, FileInformation> entry : networkFiles.entrySet()) {
                    String hashResult = entry.getValue().searchByAttribute("hash", value);
                    if (hashResult != null) {
                        resultFiles.add(hashResult);
                    }
                }
            }
            case 2 -> {
                System.out.println("Enter the title value you want to search for:");
                value = br.readLine();
                for (Map.Entry<String, FileInformation> entry : networkFiles.entrySet()) {
                    String hashResult = entry.getValue().searchByAttribute("titles", value);
                    if (hashResult != null) {
                        resultFiles.add(hashResult);
                    }
                }
            }
            case 3 -> {
                System.out.println("Enter the keyword value you want to search for:");
                value = br.readLine();
                for (Map.Entry<String, FileInformation> entry : networkFiles.entrySet()) {
                    String hashResult = entry.getValue().searchByAttribute("keywords", value);
                    if (hashResult != null) {
                        resultFiles.add(hashResult);
                    }
                }
            }
            case 4 -> {
                System.out.println("Enter the description value you want to search for:");
                value = br.readLine();
                for (Map.Entry<String, FileInformation> entry : networkFiles.entrySet()) {
                    String hashResult = entry.getValue().searchByAttribute("descriptions", value);
                    if (hashResult != null) {
                        resultFiles.add(hashResult);
                    }
                }
            }
        }
        System.out.println("List of found files with attribute " + attribute + " and value " + value);
        System.out.println(resultFiles);
    }

    public static void editFile(P2PNodeInterface node) throws IOException {
        System.out.println("Enter the hash content of the file you want to edit:");
        String fileHash = br.readLine();
        FileInformation fileInformation = node.getFileInfo(fileHash);
        System.out.println("Select the attribute you want to add content to:");
        System.out.println("1 - Titles");
        System.out.println("2 - Keywords");
        System.out.println("3 - Descriptions");
        int attribute = Integer.parseInt(br.readLine());
        String value = "";
        switch (attribute) {
            case 1:
                System.out.println("Enter the title you want to add:");
                value = br.readLine();
                fileInformation.addTitle(value);
                node.editFile(fileHash, fileInformation);
                break;
            case 2:
                System.out.println("Enter the keyword you want to add:");
                value = br.readLine();
                fileInformation.addKeyword(value);
                node.editFile(fileHash, fileInformation);
                break;
            case 3:
                System.out.println("Enter the description you want to add:");
                value = br.readLine();
                fileInformation.addDescription(value);
                node.editFile(fileHash, fileInformation);
                break;
        }
        System.out.println("The current content for the file is:");
        System.out.println("File titles: " + fileInformation.getTitles());
        System.out.println("File keywords: " + fileInformation.getKeywords());
        System.out.println("File descriptions: " + fileInformation.getDescriptions());
    }

    public static void disconnect() {
        System.exit(0);
    }
}
