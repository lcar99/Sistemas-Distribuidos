import java.io.*;
import java.net.*;
import java.util.*;

class Equalizer{

    int servernum;
    int num;
    boolean prime;

    public int getNum(){
        return num;
    }
    public boolean getPrimality(){
        return prime;
    }
    public void setServernum(int servernum){
        this.servernum = servernum;
    }
    public void setNum(int num){
        this.num = num;
    }
    public void setPrime(boolean prime){
        this.prime = prime;
    }

    public void equalize(){
        if(servernum == 1){
            servidor.mt2.equalizefile();
            servidor.mt3.equalizefile();
            //System.out.println("Equalizing servers 2 and 3");
        }
        else if(servernum == 2){
            servidor.mt1.equalizefile();
            servidor.mt3.equalizefile();
            //System.out.println("Equalizing servers 1 and 3");
        }else{
            servidor.mt1.equalizefile();
            servidor.mt2.equalizefile();
            //System.out.println("Equalizing servers 1 and 2");
        }
    }
}

class ServerThread extends Thread{

    int servernum;

    ServerThread(int counter){
        servernum = counter;
    }

    static boolean isPrime(int n){
        // Corner case
        if (n <= 1){
            return false;
        }
        // Check from 2 to n-1
        for(int i = 2; i < n; ++i){
            if(n % i == 0){
                return false;
            }
        }
        return true;
    }

    public void run(int num){
        FileWriter serverfilew;
        Random rd = new Random();
        try{
            while(servidor.lock == true){
                Thread.sleep(rd.nextInt(51) + 100);//if lock is enabled, then wait a random time(50 to 150ms) then check again
            }  
            String path = new String("serverfile" + Integer.toString(servernum) + ".txt");
            try{//check if file exists, if it doesn't, then create it
                serverfilew = new FileWriter(path, true);    
            }catch(Exception e){
                new File(path);
                serverfilew = new FileWriter(path, true);
            }
            File serverfiler = new File(path);

            if(num == -1){//read operation
                System.out.println("------------------");
                System.out.printf("Printando conteudo do servidor %d\n", servernum);
                Scanner filecontent = new Scanner(serverfiler);
                while(filecontent.hasNextLine()){
                    System.out.println(filecontent.nextLine());
                }
                filecontent.close();
                System.out.println("------------------");
            }else{//process operation    
                servidor.lock = true;//enable lock to avoid inconsistencies between server files
                System.out.printf("Processando o valor %d no servidor %d\n", num, servernum);
                if(isPrime(num)){
                    serverfilew.write("O valor " + Integer.toString(num) + " eh primo\n");  
                }else{
                    serverfilew.write("O valor " + Integer.toString(num) + " nao eh primo\n");
                }
                serverfilew.close();

                //equalize server files
                servidor.equalizer.setServernum(servernum);
                servidor.equalizer.setNum(num);
                servidor.equalizer.setPrime(isPrime(num));
                servidor.equalizer.equalize();
                
                servidor.lock = false;//disable lock 
            }    
        }catch(Exception e){
            System.out.println(e);  
        } 
    }

    public void equalizefile(){
        int num = servidor.equalizer.getNum();//get number to write on file
        boolean prime = servidor.equalizer.getPrimality();//get primality of number to write on file
        String path = new String("serverfile" + Integer.toString(servernum) + ".txt");
        FileWriter serverfilew;
        try{
            try{
                serverfilew = new FileWriter(path, true);    
            }catch (Exception e){
                new File(path);
                serverfilew = new FileWriter(path, true);
            }
            //write to file
            if(prime){
                serverfilew.write("O valor " + Integer.toString(num) + " eh primo\n");  
            }else{
                serverfilew.write("O valor " + Integer.toString(num) + " nao eh primo\n");
            }
            serverfilew.close();
        }catch (Exception e){
            System.out.println(e); 
        }
    }
}
class ClientThread extends Thread{

    int clientnum;
    Socket client;
    Balancer balancer;

    ClientThread(int counter, Socket socket, Balancer blc){
        clientnum = counter;
        client = socket;
        balancer = blc;
    }

    public void run(){
        try{  
            DataInputStream dis = new DataInputStream(client.getInputStream());
            while(true){
                int num = dis.readInt();
                balancer.addToBuffer(num);
                balancer.process();
            } 
        }catch(Exception e){
            System.out.println(e);  
        } 
    }
}

class Balancer{

    int num;
    ArrayList<Integer> request_buffer = new ArrayList<>();

    public void startServers(){
        servidor.mt1.start();
        servidor.mt2.start();
        servidor.mt3.start();
    }
    
    public void addToBuffer(int num){
        request_buffer.add(num);
    }

    public void process(){
        num = request_buffer.get(0);
        request_buffer.remove(0);
        Random rd = new Random();
        int server_number = rd.nextInt(3);//choose randomly the server to process the request
        if(server_number == 0){
            servidor.mt1.run(num);
        }
        else if(server_number == 1){
            servidor.mt2.run(num);
        }else{
            servidor.mt3.run(num);
        }    
    }

    
}

public class servidor{

    public static boolean lock;//variable used to stop servers from writing to unequalized files
    public static ServerThread  mt1 = new ServerThread(1);//creates a thread responsible for processing the operations
    public static ServerThread  mt2 = new ServerThread(2);
    public static ServerThread  mt3 = new ServerThread(3);
    public static Equalizer equalizer = new Equalizer();//helper class to store info on unequalized lines and call server equalization
    public static void main(String[] args){
        ServerSocket ss;
        lock = false;
        try{
            ss = new ServerSocket(2000);//create socket for client connection
            int clientnum = 0;
            System.out.println("Listening...");
            Socket client = null;
            client = ss.accept();//accepts the connection request from a client
            Balancer balancer = new Balancer();
            ClientThread ct1 = new ClientThread(clientnum++, client, balancer);//creates a thread that handles incoming data from
            ct1.run();//runs said thread                                       //a specific client
            System.out.printf("Client number %d connected\n", clientnum);
            client = ss.accept();
            ClientThread ct2 = new ClientThread(clientnum++, client, balancer);
            ct2.run();
            client = ss.accept();
            ClientThread ct3 = new ClientThread(clientnum++, client, balancer);
            ct3.run();
            System.out.printf("Client number %d connected\n", clientnum);
        }catch(Exception e){
            System.out.println(e);
        }
    }
}
