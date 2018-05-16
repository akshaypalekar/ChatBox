package akshaypalekar.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";
    static final int ServerPort = 6000;//The server will start at the this port
    ServerSocket serverSocket; //The socket which will be used to communicate with the client
    TextView messageWindow;//Variable for the UI elements
    EditText messageEdit;
    Button messageSend;
    ClientConnectThread clientConnectThread = null; //Thread for communicating with client
    String ClientMessage =""; //Client Message
    String MessageLog = ""; //Message Log
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageWindow = (TextView)findViewById(R.id.tv_MessageWindow);//Binding UI elements to the variables
        messageEdit = (EditText)findViewById(R.id.et_MessageEdit);
        messageSend = (Button)findViewById(R.id.bt_messageSend);
        ChatBoxServerThread chatboxServerThread = new ChatBoxServerThread();
        chatboxServerThread.start();
    }
    public void sendMessage(View view){
        if(messageEdit.getText().toString().equals("")){
            Toast.makeText(getApplicationContext(),"Kindly type your message",Toast.LENGTH_SHORT).show();
            return;
        }
        if(serverSocket==null){
            return;
        }
        ClientMessage = messageEdit.getText().toString()+"\n";
        MessageLog +="You: " + ClientMessage;
        messageWindow.setText(MessageLog);
        messageEdit.setText("");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class ChatBoxServerThread extends Thread {
        @Override
        public void run() {
            Socket listener = null;
            try {
                serverSocket = new ServerSocket(ServerPort);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MessageLog = "Waiting for Client to Connect...\n";
                        messageWindow.setText(MessageLog);
                    }
                });
                while(true){
                    listener = serverSocket.accept();
                    Log.d(TAG,"Server listening for client connections");
                    clientConnectThread = new ClientConnectThread(listener);
                    clientConnectThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(listener!=null){
                    try {
                        listener.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private class ClientConnectThread extends Thread {
        Socket socket;
        public ClientConnectThread(Socket listener) {
            this.socket = listener;
        }
        @Override
        public void run() {
            DataInputStream inputStream = null;
            DataOutputStream outputStream = null;
            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("Welcome\n");
                outputStream.flush();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {//This thread prints 'Connected to Client' in the Message view
                        MessageLog += "Connected to Client\n"+"\n";
                        messageWindow.setText(MessageLog);
                    }
                });
                while(true){
                    if(inputStream.available()>0) {
                        final String newMessage = inputStream.readUTF();
                        if (newMessage.equals("Client Disconnected\n")) {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MessageLog += newMessage;
                                    messageWindow.setText(MessageLog);
                                }
                            });
                        } else {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MessageLog += "Client: " + newMessage;
                                    messageWindow.setText(MessageLog);
                                }
                            });
                        }
                    }
                    if(!ClientMessage.equals("")){
                        outputStream.writeUTF(ClientMessage);
                        outputStream.flush();
                        ClientMessage = "";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(inputStream!=null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }if(outputStream!=null){
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}