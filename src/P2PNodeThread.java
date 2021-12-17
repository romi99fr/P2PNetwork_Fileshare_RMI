/* ---------------------------------------------------------------
Práctica 1.
Código fuente: NodeThread.java
Grau Informàtica
73210823Y - Joel Romia Aribau
53395926T - Pau Francino Urdaniz
--------------------------------------------------------------- */
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class P2PNodeThread implements Runnable {
    private int clientPort;
    private P2PNodeImplementation node;

    public P2PNodeThread(int clientPort, P2PNodeImplementation node) {
        this.clientPort = clientPort;
        this.node = node;
    }

    @Override
    public void run() {
        try {
            Registry registry = startRegistry(this.clientPort);
            registry.rebind("node", this.node);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private Registry startRegistry(int clientPort) throws RemoteException {
        try {
            //Check if registry is already created
            Registry registry = LocateRegistry.getRegistry(clientPort);
            registry.list();
            return registry;
        }
        catch(RemoteException re) {
            //Create registry if not already
            return LocateRegistry.createRegistry(clientPort);
        }
    }
}
