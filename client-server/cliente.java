import java.io.*;
import java.net.*;
import java.util.Random;
public class cliente{
    public static void main(String[] args){
        Socket server;
        try{
            server = new Socket("127.0.0.1", 2000);//establishes connection to loopback interface at port 2000  
            System.out.println("Sending data to server");
            DataOutputStream dout = new DataOutputStream(server.getOutputStream());  
            Random rd = new Random();
            int num;
            while(true){//client chooses at random if it's going to send a read or process operation to server
                if(rd.nextInt(2) == 1){//read operation
                    num = -1;       
                }else{//process operation
                    num = 2 + rd.nextInt(999999);
                }
                dout.writeInt(num);//send number generated to server
                dout.flush();               
                try{
                    num = 100 + rd.nextInt(200);
                    Thread.sleep(num);//sleep for a random time(between 100 and 299ms)
                }catch(InterruptedException ex){
                    Thread.currentThread().interrupt();
                } 
            }
        }catch(Exception e){
            System.out.println(e);  
        }   
    }
}
