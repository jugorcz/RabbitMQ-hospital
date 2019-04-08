import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class Doctor implements AutoCloseable {

    private Connection connection;
    private Channel channel;
    private String requestQueueName = "rpc_queue";

    public Doctor() throws IOException, TimeoutException {
        //We establish a connection and channel.
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    public static void main(String[] argv) {
        try (Doctor fibonacciRpc = new Doctor()) {
            for (int i = 0; i < 32; i++) {
                String i_str = Integer.toString(i);
                System.out.println(" [x] Requesting fib(" + i_str + ")");
                String response = fibonacciRpc.call(i_str);
                System.out.println(" [.] Got '" + response + "'");
            }
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Our call method makes the actual RPC request.
    public String call(String message) throws IOException, InterruptedException {
        // Here, we first generate a unique correlationId number and save it -
        // our consumer callback will use this value to match the appropriate response.
        final String corrId = UUID.randomUUID().toString();

        // Then, we create a dedicated exclusive queue for the reply and subscribe to it.
        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        //Next, we publish the request message, with two properties: replyTo and correlationId.
        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        // Since our consumer delivery handling is happening in a separate thread, we're going
        // to need something to suspend the main thread before the response arrives.
        // Usage of BlockingQueue is one possible solutions to do so. Here we are creating
        // ArrayBlockingQueue with capacity set to 1 as we need to wait for only one response.
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        // The consumer is doing a very simple job, for every consumed response message it checks
        // if the correlationId is the one we're looking for. If so, it puts the response to BlockingQueue.
        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        channel.basicCancel(ctag);
        return result;
    }

    public void close() throws IOException {
        connection.close();
    }
}
