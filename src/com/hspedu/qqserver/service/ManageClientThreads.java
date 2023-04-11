package com.hspedu.qqserver.service;

import java.util.HashMap;
import java.util.Iterator;

//用于管理和客户端通讯的线程
public class ManageClientThreads {
    private static HashMap<String, ServerConnectClientThread> hm = new HashMap<>();

    // 添加线程对象
    public static void addClientThread(String userId, ServerConnectClientThread serverConnectClientThread) {
        hm.put(userId, serverConnectClientThread);
    }

    // 根据UserId 返回线程

    public static ServerConnectClientThread getServerConnectClientThread (String userId) {
        return hm.get(userId);
    }

    public static String getOnlineUser() {
        Iterator<String> iterator = hm.keySet().iterator();
        String onlineUserList = "";
        while (iterator.hasNext()) {
            onlineUserList += iterator.next().toString() + " ";
        }
        return onlineUserList;
    }

    public static void removeServerConnectClientThread(String userId) {
        hm.remove(userId);
    }

}
