import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

public class Queue {
    protected Channel channel;
    protected BufferedReader reader;
    protected Consumer infoConsumer;

    public Queue() {
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    void initChannel() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        channel = connection.createChannel();
        channel.basicQos(1);
        channel.exchangeDeclare("hospital", BuiltinExchangeType.TOPIC);  // Dopasowanie do wzorca
        channel.exchangeDeclare("info", BuiltinExchangeType.FANOUT);     // Każdy kto jest zapisany do danego
                                                                            // Exchang dostaje wiadomości
        infoConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("[INFO] Received: " + message);
            }
        };
    }


    void initInfoQueue() throws IOException {
        String infoQueue = channel.queueDeclare().getQueue();
        System.out.println("Info queue: " + infoQueue);
        channel.queueBind(infoQueue, "info", "");
        channel.basicConsume(infoQueue, true, infoConsumer);
    }
}
