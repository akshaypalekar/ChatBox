package akshaypalekar.chatboxclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";
    static final int ClientPort = 5000;
    static final String ClientIp = "10.0.2.2";
    TextView messageWindow;
    EditText messageEdit;
    Button messageSend, connectToServer, disconnectFromServer;
    String serverMessage = "";
    String MessageLog = "";
    ServerConnectThread serverConnectThread;
    Socket socket = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectToServer = (Button) findViewById(R.id.bt_ConnectToServer);
        messageWindow = (TextView) findViewById(R.id.tv_MessageWindow);
        messageEdit = (EditText) findViewById(R.id.et_MessageEdit);
        messageSend = (Button) findViewById(R.id.bt_SendMessage);
        disconnectFromServer = (Button) findViewById(R.id.bt_Disconnect);
        disconnectFromServer.setEnabled(false);
    }
    public void connect(View view) {
        serverConnectThread = new ServerConnectThread(ClientIp, ClientPort);
        serverConnectThread.start();
        connectToServer.setEnabled(false);
        disconnectFromServer.setEnabled(true);
    }
    public void disconnect(View view) {
        if(serverConnectThread==null){
            return;
        }
        serverConnectThread.disconnect();
    }
    public void sendMessage(View view) {
        if (messageEdit.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "Kindly type you message", Toast.LENGTH_SHORT).show();
            return;
        }
        serverMessage = messageEdit.getText().toString() + "\n";
        MessageLog +="You: " + serverMessage;
        messageWindow.setText(MessageLog);
        messageEdit.setText("");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class ServerConnectThread extends Thread {
        String ip;
        int port;
        boolean flag = false;
        ServerConnectThread(String ClientIp, int ClientPort) {
            this.ip = ClientIp;
            this.port = ClientPort;
        }
        @Override
        public void run() {
            DataOutputStream outputStream = null;
            DataInputStream inputStream = null;
            try {
                socket = new Socket(ip, port);
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("Hello Server\n");
                outputStream.flush();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MessageLog = "Connected to Server\n"+"\n";
                        messageWindow.setText(MessageLog);
                    }
                });
                while (!flag) {
                    if (inputStream.available() > 0) {
                        final String newMessage = inputStream.readUTF();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MessageLog += "Server: "+ newMessage;
                                messageWindow.setText(MessageLog);
                            }
                        });
                    }
                    if (!serverMessage.equals("")) {
                        outputStream.writeUTF(serverMessage);
                        outputStream.flush();
                        serverMessage = "";
                    }
                }
                while (flag){
                    outputStream.writeUTF("Client Disconnected\n");
                    outputStream.flush();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        private void disconnect(){
            flag = true;
            MessageLog += "Disconnected From Server\n"+"\n";
            messageWindow.setText(MessageLog);
            connectToServer.setEnabled(true);
            disconnectFromServer.setEnabled(false);
        }
    }
}