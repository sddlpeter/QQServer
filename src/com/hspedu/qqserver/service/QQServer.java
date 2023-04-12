package com.hspedu.qqserver.service;

import com.hspedu.qqcommon.Message;
import com.hspedu.qqcommon.MessageType;
import com.hspedu.qqcommon.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class QQServer {
    private ServerSocket ss = null;

    // 创建一个集合，存储多个用户登录信息, 可以使用ConcurrentHashMap来替换，能保证高并发
    private static HashMap<String, User> validUsers = new HashMap<>();
    private static ConcurrentHashMap<String, List<Message>> offLineDb = new ConcurrentHashMap<>();

    // 在静态代码块初始化validUsers
    static {
        validUsers.put("100", new User("100", "123456"));
        validUsers.put("200", new User("200", "123456"));
        validUsers.put("300", new User("300", "123456"));
        validUsers.put("至尊宝", new User("至尊宝", "123456"));
        validUsers.put("紫霞仙子", new User("紫霞仙子", "123456"));
        validUsers.put("菩提老祖", new User("菩提老祖", "123456"));
    }

    public static void main(String[] args) {
        new QQServer();
    }

    public static ConcurrentHashMap<String, List<Message>> getOffLineDb() {
        return offLineDb;
    }

    private boolean checkUser(String userId, String password) {
        User user = validUsers.get(userId);
        if (user == null) {
            return false;
        }
        if (!user.getPassword().equals(password)) {
            return false;
        }
        return true;
    }

    public QQServer() {
        try {
            System.out.println("服务端在9999端口监听...");
            ss = new ServerSocket(9999);
            new Thread(new SendNewsToAllService()).start();

            while (true) { // 当和某个客户端建连接后，会继续监听，因此循环
                Socket socket = ss.accept();  // 服务端在这里监听，如果有客户端链接，就继续往下执行，否则就阻塞在这里...
                
                // 得到socket关联对象输入流
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                // 得到socket关联的对象输出流
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

                User u = (User) ois.readObject(); // 读取客户端发送的User对象

                // 创建message对象
                Message message = new Message();


                // 验证
                if (checkUser(u.getUserId(), u.getPassword())) { // 登录成功
                    message.setMsgType(MessageType.MESSAGE_LOGIN_SUCCESS);
                    // 将message对象回复客户端
                    oos.writeObject(message);

                    // 创建一个线程，和客户端保持通讯，该线程需要持有socket对象
                    ServerConnectClientThread serverConnectClientThread = new ServerConnectClientThread(socket, u.getUserId());
                    serverConnectClientThread.start();

                    // 把该线程对象放入到集合中
                    ManageClientThreads.addClientThread(u.getUserId(), serverConnectClientThread);

                    // 用户上线，检查是否有离线消息库存，如果有则发送离线消息给这个刚登陆的用户
                    if (offLineDb.containsKey(u.getUserId())) {
                        ServerConnectClientThread scct = ManageClientThreads.getServerConnectClientThread(u.getUserId());
                        ObjectOutputStream ooos = new ObjectOutputStream(scct.getSocket().getOutputStream());

                        for (Message msg : offLineDb.get(u.getUserId())) {
                            ooos.writeObject(msg);
                            System.out.println("发消息给 " + u.getUserId());
                        }
                    }


                } else { //登录失败
                    System.out.println("用户 id=" + u.getUserId() + " pwd=" + u.getPassword() + " 验证失败");
                    message.setMsgType(MessageType.MESSAGE_LOGIN_FAIL);
                    oos.writeObject(message);

                    socket.close();
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
