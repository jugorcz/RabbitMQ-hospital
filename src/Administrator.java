import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Administrator extends Queue{

    private void initQueue() throws IOException{
        String adminQueue = channel.queueDeclare().getQueue();
        System.out.println("Administrator queue: " + adminQueue);
        channel.queueBind(adminQueue, "hospital", "#");
        channel.basicConsume(adminQueue, true, infoConsumer);
        System.out.println("Waiting for messages... ");
    }

    public void run() throws IOException, TimeoutException {
        initChannel();
        initQueue();

        while (true) {
            System.out.println("Enter message: ");
            String message = reader.readLine();
            if("exit".equals(message))
                break;
            channel.basicPublish("info", "", null, message.getBytes());
            System.out.println("Sent: " + message);
        }
    }

    public static void main(String[] argv) throws IOException, TimeoutException {
        System.out.println("ADMINISTRATOR");
        Administrator administrator = new Administrator();
        administrator.run();
    }
}
