import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Technician extends Queue{

    private String[] skills;

    private void initTechnicianQueue() throws IOException{
        for(String skill : skills) {
            String key = "*." + skill.toLowerCase();
            String queue = channel.queueDeclare(key, false, false, false, null).getQueue();
            System.out.println("Technician queue: " + queue);
            channel.queueBind(queue, "hospital", key);
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag,
                                           Envelope envelope,
                                           AMQP.BasicProperties properties,
                                           byte[] body)
                        throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println("Received: " + message);
                    String[] splittedMessage = message.split(" ");
                    System.out.print("Handling message");
                    String reply = splittedMessage[1] + " " + splittedMessage[0] + " done";
                    channel.basicPublish("hospital",
                            envelope.getRoutingKey().replace("." + splittedMessage[0], ""),
                            null,
                            reply.getBytes("UTF-8"));
                    System.out.println("\nSent: " + reply);
                }
            };
            channel.basicConsume(queue, true, consumer);
        }
    }

    public void run() throws IOException, TimeoutException {
        System.out.println("Enter two technician skills, can be knee, elbow or hip.");
        skills = reader.readLine().split(" ");
        initChannel();
        initInfoQueue();
        initTechnicianQueue();
        System.out.println("Waiting for tasks...");
    }

    public static void main(String[] argv) throws IOException, TimeoutException{
        System.out.println("TECHNICIAN");
        Technician technician = new Technician();
        technician.run();
    }
}