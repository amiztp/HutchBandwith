import java.io.IOException;
import java.net.InetAddress;

public class CrossPlatformPing {
    public static void main(String[] args) {
        String host = "www.hutch.lk";

        while (true) {
            try {
                // Resolve host
                InetAddress inet = InetAddress.getByName(host);

                // Try to reach host with a 5-second timeout
                boolean reachable = inet.isReachable(5000);

                if (reachable) {
                    System.out.println("Ping successful to " + host);
                } else {
                    System.out.println("Ping failed to " + host);
                }

                // Wait 10 seconds before next attempt
                Thread.sleep(10000);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
