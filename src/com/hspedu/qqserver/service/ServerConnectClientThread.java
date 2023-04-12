package com.hspedu.qqserver.service;


import com.hspedu.qqcommon.Message;
import com.hspedu.qqcommon.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// 该类的对象和某个客户端保持通讯
public class ServerConnectClientThread extends Thread {
    private Socket socket;
    private String userId; // 连接到服务端的用户id

    public ServerConnectClientThread(Socket socket, String userId) {
        this.socket = socket;
        this.userId = userId;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {  // 该线程处于run的状态，可以发送/接收消息

        while (true) {
            try {
                System.out.println("服务端和客户端 " + userId + " 保持通信，读取数据...");
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message message = (Message) ois.readObject();
                if (message.getMsgType().equals(MessageType.MESSAGE_GET_ONLINE_FRIEND)) {
                    System.out.println(message.getSender() + " 要在线用户列表");
                    Message message2 = new Message();
                    message2.setMsgType(MessageType.MESSAGE_RET_ONLINE_FRIEND);
                    message2.setContent(ManageClientThreads.getOnlineUser());
                    message2.setGetter(message2.getSender());

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(message2);

                } else if (message.getMsgType().equals(MessageType.MESSAGE_CLIENT_EXIT)) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    socket.close();
                    ManageClientThreads.removeServerConnectClientThread(userId);
                    System.out.println(userId + " 用户退出，断开与客户端链接...");
                    break;
                } else if (message.getMsgType().equals(MessageType.MESSAGE_COMM_MES)) {

                    // 如果要接收消息的用户不在线，将此消息放入到offline db里面去
                    if (!ManageClientThreads.getHashMap().containsKey(message.getGetter())) {
                        if (!QQServer.getOffLineDb().containsKey(message.getGetter())) {
                            QQServer.getOffLineDb().put(message.getGetter(), new ArrayList<>());
                        }
                        QQServer.getOffLineDb().get(message.getGetter()).add(message);
                    } else {
                        //  根据message获取到getter id，然后拿到getter id对应的线程
                        ServerConnectClientThread serverConnectClientThread = ManageClientThreads.getServerConnectClientThread(message.getGetter());
                        // 得到上面线程对应的socket对象输出流，将message对象转发给指定的客户端
                        ObjectOutputStream oos = new ObjectOutputStream(serverConnectClientThread.getSocket().getOutputStream());
                        oos.writeObject(message); // 转发消息，提示如果客户不在线，可以保存到数据库，实现离线留言
                    }
                } else if (message.getMsgType().equals(MessageType.MESSAGE_TO_ALL_MES)) {
                    for (String userId : ManageClientThreads.getHashMap().keySet()) {
                        if (userId.equals(message.getSender())) continue;
                        //  根据message获取到getter id，然后拿到getter id对应的线程
                        ServerConnectClientThread serverConnectClientThread = ManageClientThreads.getServerConnectClientThread(userId);
                        // 得到上面线程对应的socket对象输出流，将message对象转发给指定的客户端
                        ObjectOutputStream oos = new ObjectOutputStream(serverConnectClientThread.getSocket().getOutputStream());
                        oos.writeObject(message); // 转发消息，提示如果客户不在线，可以保存到数据库，实现离线留言
                    }
                } else if (message.getMsgType().equals(MessageType.MESSAGE_FILE_MES)) { //服务端拿到message之后，只需要转发给另外一个客户端
                    if (!ManageClientThreads.getHashMap().containsKey(message.getGetter())) {
                        if (!QQServer.getOffLineDb().containsKey(message.getGetter())) {
                            QQServer.getOffLineDb().put(message.getGetter(), new ArrayList<>());
                        }
                        QQServer.getOffLineDb().get(message.getGetter()).add(message);
                    } else {
                        ServerConnectClientThread serverConnectClientThread = ManageClientThreads.getServerConnectClientThread(message.getGetter());
                        ObjectOutputStream oos = new ObjectOutputStream(serverConnectClientThread.getSocket().getOutputStream());
                        oos.writeObject(message);
                    }

                }

                // 后面会使用message
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
