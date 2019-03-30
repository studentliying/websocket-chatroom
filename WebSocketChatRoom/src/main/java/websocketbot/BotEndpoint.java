/**
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * You may not modify, use, reproduce, or distribute this software except in
 * compliance with  the terms of the License at:
 * http://java.net/projects/javaeetutorial/pages/BerkeleyLicense
 */
package websocketbot;

import websocketbot.decoders.MessageDecoder;
import websocketbot.encoders.ChatMessageEncoder;
import websocketbot.encoders.InfoMessageEncoder;
import websocketbot.encoders.JoinMessageEncoder;
import websocketbot.encoders.UsersMessageEncoder;
import websocketbot.messages.*;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import javax.annotation.Resource;
//import javax.enterprise.concurrent.ManagedExecutorService;
//import javax.inject.Inject;

/* Websocket endpoint */
@ServerEndpoint(
        value = "/websocketbot",
        decoders = { MessageDecoder.class }, 
        encoders = { JoinMessageEncoder.class, ChatMessageEncoder.class,
                     InfoMessageEncoder.class, UsersMessageEncoder.class }
        )
/* There is a BotEndpoint instance per connetion */
public class BotEndpoint {
    private static final Logger logger = Logger.getLogger("BotEndpoint");
    private static List<Session> all_sessions = new ArrayList();
    /* Bot functionality bean */
//    //@Inject
//    private BotStockBean botstockbean = new BotStockBean();
//    /* Executor service for asynchronous processing */
//    @Resource(name="tomcatThreadPool")
//    private ManagedExecutorService mes;
    
    @OnOpen
    public void openConnection(Session session) {
        System.out.println("连接成功！");
        logger.log(Level.INFO, "Connection opened.");
        all_sessions.add(session);
    }
    
    @OnMessage
    public void message(final Session session, Message msg) {
        logger.log(Level.INFO, "Received: {0}", msg.toString());
        System.out.println(session.toString());
        if (msg instanceof JoinMessage) {
            /* Add the new user and notify everybody */
            JoinMessage jmsg = (JoinMessage) msg;
            session.getUserProperties().put("name", jmsg.getName());
            session.getUserProperties().put("active", true);
            logger.log(Level.INFO, "Received: {0}", jmsg.toString());
            sendAll(session, new InfoMessage(jmsg.getName() + " has joined the chat"));
            //sendAll(session, new ChatMessage("Duke", jmsg.getName(), "Hi there!!"));
            sendAll(session, new UsersMessage(this.getUserList(session)));
            
        } else if (msg instanceof ChatMessage) {
            /* Forward the message to everybody */
            final ChatMessage cmsg = (ChatMessage) msg;
            logger.log(Level.INFO, "Received: {0}", cmsg.toString());
            sendAll(session, cmsg);

        }
    }
    
    @OnClose
    public void closedConnection(Session session) {
        /* Notify everybody */
        session.getUserProperties().put("active", false);
        if (session.getUserProperties().containsKey("name")) {
            String name = session.getUserProperties().get("name").toString();
            sendAll(session, new InfoMessage(name + " has left the chat"));
            sendAll(session, new UsersMessage(this.getUserList(session)));
        }
        logger.log(Level.INFO, "Connection closed.");
    }
    
    @OnError
    public void error(Session session, Throwable t) {
        logger.log(Level.INFO, "Connection error ({0})", t.toString());
    }
    
    /* Forward a message to all connected clients
     * The endpoint figures what encoder to use based on the message type */
    public synchronized void sendAll(Session session, Object msg) {
        try {
            getUserList(session);
            for (Session s : all_sessions) {
                if (s.isOpen()) {
                    System.out.println(s.toString());
                    s.getBasicRemote().sendObject(msg);
                    logger.log(Level.INFO, "Sent: {0}", msg.toString());
                }
            }
        } catch (IOException | EncodeException e) {
            logger.log(Level.INFO, e.toString());
        }   
    }
    
    /* Returns the list of users from the properties of all open sessions */
    public List<String> getUserList(Session session) {
        List<String> users = new ArrayList<>();
        //users.add("Duke");
        for (Session s : all_sessions) {
            if (s.isOpen() && (boolean) s.getUserProperties().get("active"))
            {
                users.add(s.getUserProperties().get("name").toString());
                System.out.println(s.getUserProperties().get("name").toString());
            }

        }
        return users;
    }
}
