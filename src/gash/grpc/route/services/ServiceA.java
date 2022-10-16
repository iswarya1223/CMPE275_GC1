package gash.grpc.route.services;

import gash.grpc.route.client.RouteClient;
import com.google.protobuf.ByteString;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import route.Route;
import gash.grpc.route.server.RouteServer;
import gash.grpc.route.server.RouteServerImpl;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ServiceA extends RouteServerImpl {
    private static final long serverId = 1;

    // server a
    // private queue que obkjectname
    // this queue=queue
    // queue
    public static void main(String[] args) throws Exception {
        String path = args[0];

        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            ServiceA service = new ServiceA();
            service.start();
            service.blockUntilShutdown();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

    }

    @Override
    public void request(Route request, StreamObserver<Route> responseObserver) {
        Route.Builder builder = Route.newBuilder();
        builder.setId(request.getId());
        builder.setOrigin(RouteServer.getInstance().getServerID());
        builder.setDestination(request.getOrigin());
        builder.setPath(request.getPath());

        String content = new String(request.getPayload().toByteArray());
        String path = request.getPath();
        long origin = request.getOrigin();

        builder.setPayload(this.process(request));
        Route rtn = builder.build();
        responseObserver.onNext(rtn);
        responseObserver.onCompleted();

        if (content.equals("route")) {
            routeMessageToDestinationServer(origin, path, content + "forwarded from Server A");
        }
    }

    private void routeMessageToDestinationServer(long origin, String path, String content) {
        int routePort = ServiceRouting.route(path);
        RouteClient routeClient = new RouteClient(serverId, routePort);
        routeClient.sendMessage(1, "/serverB", content);
    }

    private void sendServiceB_Ack() {
        RouteClient routeClient = new RouteClient(1315, 2346);
        routeClient.sendMessage(1, "/serverB", "message from server A " + routeClient.getClientID());
    }

    @Override
    protected ByteString process(Route msg) {
        String content = new String(msg.getPayload().toByteArray());
        System.out.println("-- got: From" + msg.getOrigin() + ", path: " + msg.getPath() + ", with: " + content);

        byte[] raw = "Hi I am Service A".getBytes();
        return ByteString.copyFrom(raw);
    }

    @Override
    public void start() throws Exception {
        this.svr = ServerBuilder.forPort(RouteServer.getInstance().getServerPort()).addService(new ServiceA()).build();
        System.out.println("-- starting server");
        this.svr.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ServiceA.this::stop));
    }

}
