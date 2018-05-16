package akshaypalekar.myapplication;

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

    //The server will start at the this port
    static final int ServerPort = 6000;

    //The socket which will be used to communicate with the client
    ServerSocket serverSocket;

    //Variable for the UI elements
    TextView messageWindow;
    EditText messageEdit;
    Button messageSend;

    ClientConnectThread clientConnectThread = null;
    String ClientMessage ="";
    String MessageLog = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Binding UI elements to the variables
        messageWindow = (TextView)findViewById(R.id.tv_MessageWindow);
        messageEdit = (EditText)findViewById(R.id.et_MessageEdit);
        messageSend = (Button)findViewById(R.id.bt_messageSend);

//        findViewById(R.id.et_MessageEdit).setOnClickListener( new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                messageEdit.setText("");
//            }
//        });

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

                //This thread prints 'Connected to Client' in the Message view
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
