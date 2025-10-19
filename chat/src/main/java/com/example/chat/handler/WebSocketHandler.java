package com.example.chat.handler;

import com.example.chat.model.Message;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 웹 소켓 연결
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("[afterConnectionEstablished] " + session.getId());
        var sessionId = session.getId();
        sessions.put(sessionId, session);

        Message message = Message.builder().sender(sessionId).receiver("all").build();
        message.newConnect();

        sessions.values().forEach(s -> {
            try{
                if(!s.getId().equals(sessionId)) {
                    s.sendMessage(new TextMessage(message.toString()));
                }
            } catch (Exception e) {
                // TODO: throw
            }
        });
    }

    // 양방향 데이터 통신
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Message message = getObject(textMessage.getPayload());
        message.setSender(session.getId());

        WebSocketSession receiver = sessions.get(message.getReceiver());    // 메시지를 받을 타겟을 찾는다
        if(receiver != null && receiver.isOpen()) {     // 타겟이 존재하고 연결된 상태라면 메세지를 전송한다
            receiver.sendMessage(new TextMessage(message.toString()));
        }
    }

    private Message getObject(String textMessage){
        Gson gson = new Gson();
        Message message = gson.fromJson(textMessage, Message.class);
        return message;
    }

    // 연결 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        var sessionId = session.getId();

        sessions.remove(sessionId);

        final Message message = new Message();
    }
}
