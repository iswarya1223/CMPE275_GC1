package gash.grpc.route.client;

import gash.grpc.route.heartbeat.service.HeartBeatService;
import java.net.InetSocketAddress;

public class ClientA extends RouteClient{
    private final HeartBeatService heartBeatService;

    public ClientA(long clientId, int portToSend,int clientPort) {
        super(clientId,portToSend);
        heartBeatService = new HeartBeatService(
                new InetSocketAddress(Long.toString(clientId), clientPort));
    }
    private void startHeartBeatProcess(){
        HeartBeatService clientNode = heartBeatService;
        clientNode.start();
    }
    public static void main(String[] args){
        ClientA routeClient = new ClientA(1314, 2344,1111);
        routeClient.startHeartBeatProcess();
        routeClient.sendMessage(1, "/serverA", "route");
    }
}
