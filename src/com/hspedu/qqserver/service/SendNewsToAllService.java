package com.hspedu.qqserver.service;

import com.hspedu.qqcommon.Message;
import com.hspedu.qqcommon.MessageType;
import com.hspedu.utils.Utility;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class SendNewsToAllService implements Runnable {

    @Override
    public void run() {
        while (true) {
            System.out.println("请输入服务器要推送的新闻/消息[输入exit表示退出]：");
            String news = Utility.readString(100);
            if ("exit".equals(news)) break;
            Message message = new Message();
            message.setSender("服务器");
            message.setMsgType(MessageType.MESSAGE_TO_ALL_MES);
            message.setContent(news);
            message.setSentTime(new Date().toString());
            System.out.println("服务器发消息给所有人说：" + news);

            HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHashMap();
            Iterator<String> iterator = hm.keySet().iterator();
            while (iterator.hasNext()) {
                String onLineUserId = iterator.next();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(hm.get(onLineUserId).getSocket().getOutputStream());
                    oos.writeObject(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
