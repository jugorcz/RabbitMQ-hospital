import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Doctor extends Queue  {

    private String doctorId;

    void initDoctorQueue() throws IOException {
        String doctorQueue = channel.queueDeclare().getQueue();
        System.out.println("Doctor queue: " + doctorQueue);
        channel.queueBind(doctorQueue, "hospital", doctorId);
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
                System.out.println("Received " + splittedMessage[1] + " results for " + splittedMessage[0]);
            }
        };
        channel.basicConsume(doctorQueue, true, consumer);
    }

    public void run() throws IOException, TimeoutException {
        System.out.println("Enter doctor id: ");
        doctorId = reader.readLine();
        initChannel();
        initInfoQueue();
        initDoctorQueue();
        System.out.println("Waiting for patients....");
        while (true) {
            System.out.println("Enter message: <type> <name>");
            String message = reader.readLine();
            if("exit".equals(message))
                break;
            String[] msg = message.split(" ");
            String key = doctorId + "." + msg[0];
            channel.basicPublish("hospital", key, null, message.getBytes());
            System.out.println("Sent: " + message);
        }
    }

    public static void main(String[] argv) throws IOException, TimeoutException {
        System.out.println("DOCTOR");
        Doctor doctor = new Doctor();
        doctor.run();
    }

}
