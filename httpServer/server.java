import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
public class server {
    public static void main(String[] args) throws Exception {
        try (ServerSocket serversocket = new ServerSocket(8088)) {
            while (true) {
                // Accept client connection
                Socket client = serversocket.accept();
                Thread clientThread = new Thread(new ClientHandler(client));
                System.out.println(clientThread.getId());
                clientThread.start();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    // Runnable is functional interface. It serves as a descriptor for a task
    // that can be executed concurrently.
    public static class ClientHandler implements Runnable{
        private Socket client;

        public ClientHandler(Socket client){
            this.client = client;
        }
        @Override
        public void run(){
            try{
                handleClient(client);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        public static void handleClient(Socket client)  throws IOException {
            System.out.println("Debug: got new client "+ client.toString());
            BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while(!(line = br.readLine()).isBlank()){
                requestBuilder.append(line + "\r\n");
            }

            String request = requestBuilder.toString();
           // System.out.println(request);
//        Request:
//        GET / HTTP/1.1
//         Host: localhost:8088
//          Connection: keep-alive
//          Cache-Control: max-age=0
//          sec-ch-ua: "Not_A Brand";v="8", "Chromium";v="120", "Google Chrome";v="120"
//          sec-ch-ua-mobile: ?0
//          sec-ch-ua-platform: "Linux"
//          Upgrade-Insecure-Requests: 1
//          User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36
//          Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7
//        Sec-Fetch-Site: none
//        Sec-Fetch-Mode: navigate
//        Sec-Fetch-User: ?1
//        Sec-Fetch-Dest: document
//        Accept-Encoding: gzip, deflate, br, zstd
//        Accept-Language: en-US,en;q=0.9

            // Parsing the request

            String [] requestLines = request.split("\r\n");
            String[] requestLine = requestLines[0].split(" ");
            String method = requestLine[0];
            String path = requestLine[1];
            String version = requestLine[2];
            String host = requestLines[1].split(" ")[1];

            List<String> headers = new ArrayList<>();
            for(int h=2;h< requestLines.length;h++){
                String header = requestLines[h];
                headers.add(header);
            }

//        String accessLog = String.format("Client %s, method %s, path %s, version %s, host %s, headers %s",
//                client.toString(), method, path, version, host, headers.toString());
//        System.out.println(accessLog);

            // To return a file path to client as a response
            Path filePath = getFilePath(path);

            if(Files.exists(filePath)){
                String contentType = guessContentType(filePath);
                sendResponse(client,"200 OK",contentType, Files.readAllBytes(filePath));
            }else{
                byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
                sendResponse(client, "404 Not Found", "text/html", notFoundContent);

            }
        }

        private static String guessContentType(Path filePath) throws IOException{
            return Files.probeContentType(filePath);
        }

        private static Path getFilePath(String path){
            if("/".equals(path)){
                path="/index.html";
            }
            return Paths.get("www",path);
          }

        private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException{
            OutputStream clientOutput = client.getOutputStream();
            clientOutput.write(("HTTP/1.1 \r\n" + status).getBytes());
            clientOutput.write(("ContentType: " + contentType + "\r\n").getBytes());
            clientOutput.write("\r\n".getBytes());
            clientOutput.write(content);
            clientOutput.write("\r\n\r\n".getBytes());
            clientOutput.flush();
            client.close();
        }
    }
}