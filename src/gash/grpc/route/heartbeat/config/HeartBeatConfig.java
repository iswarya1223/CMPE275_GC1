package gash.grpc.route.heartbeat.config;

import java.io.Serializable;
import java.time.Duration;

public class HeartBeatConfig implements Serializable {
    public final Duration timeoutInterval; // time duration for which if a message is not received
    public final Duration updateFrequency;
    public final Duration failureDetectionFrequency;
    public final Duration cleanupTimeout;
    public final int peersToUpdatePerInterval;

    public HeartBeatConfig(Duration timeoutInterval, Duration updateFrequency,
                           Duration failureDetectionFrequency, Duration cleanupTimeout,
                           int peersToUpdatePerInterval) {
        this.timeoutInterval = timeoutInterval;
        this.updateFrequency = updateFrequency;
        this.failureDetectionFrequency = failureDetectionFrequency;
        this.cleanupTimeout = cleanupTimeout;
        this.peersToUpdatePerInterval = peersToUpdatePerInterval;
    }
}
