package executable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleTestServer {
    public static void main(String[] args) {
        final int port = 9000;
        System.out.println("SimpleTestServer listening on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            try (Socket client = serverSocket.accept();
                 DataInputStream in = new DataInputStream(client.getInputStream());
                 DataOutputStream out = new DataOutputStream(client.getOutputStream())) {

                System.out.println("Client connected: " + client.getRemoteSocketAddress());
                // Lee primer mensaje (espera "Doctor")
                String first = in.readUTF();
                System.out.println("Received first UTF: " + first);
                out.writeUTF("ACK");
                out.writeUTF("Hello from test server");
                out.flush();

                // Bucle simple de diagnóstico
                while (true) {
                    String cmd;
                    try {
                        cmd = in.readUTF();
                    } catch (IOException e) {
                        System.out.println("Client disconnected.");
                        break;
                    }
                    System.out.println("CMD: " + cmd);
                    if ("END".equalsIgnoreCase(cmd)) {
                        out.writeUTF("ACK");
                        out.writeUTF("Goodbye");
                        out.flush();
                        break;
                    } else if ("START".equalsIgnoreCase(cmd)) {
                        out.writeUTF("ACK");
                        out.writeUTF("Starting recording");
                    } else {
                        out.writeUTF("ECHO");
                        out.writeUTF("Received: " + cmd);
                    }
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server stopped.");
    }
}

