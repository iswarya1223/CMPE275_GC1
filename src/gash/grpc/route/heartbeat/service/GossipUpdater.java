package gash.grpc.route.heartbeat.service;

import java.net.InetSocketAddress;

public interface GossipUpdater {
    void update(InetSocketAddress inetSocketAddress);
}
