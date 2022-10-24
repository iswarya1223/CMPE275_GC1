package gash.grpc.route.services;

import com.google.protobuf.ByteString;
import gash.grpc.route.client.RouteClient;
import gash.grpc.route.server.RouteServer;
import gash.grpc.route.server.RouteServerImpl;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import route.Route;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ServiceB1 extends RouteServerImpl {
    public static void main(String[] args) throws Exception {
        String path = args[0];
        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            ServiceB1 service = new ServiceB1();
            service.start();
            service.blockUntilShutdown();
        } catch (IOException var4) {
            var4.printStackTrace();
        }
    }

    @Override
    public void request(Route request, StreamObserver<Route> responseObserver) {
        Route.Builder builder = Route.newBuilder();
        builder.setId(RouteServer.getInstance().getNextMessageID());
        builder.setOrigin(RouteServer.getInstance().getServerID());
        builder.setDestination(request.getOrigin());
        builder.setPath(request.getPath());
        builder.setPayload(this.process(request));
        byte[] raw = "Request processed by  processed by Server B of CustomQueue 1".getBytes();
        builder.setProcessedBy(ByteString.copyFrom(raw));
        builder.setIsFromClient(false);
        builder.setLbPortNo(request.getLbPortNo());
        builder.setClientStartTime(request.getClientStartTime());
        builder.setClientPort(request.getClientPort());
        Route rtn = builder.build();
        RouteClient routeClient = new RouteClient( RouteServer.getInstance().getServerID(), (int) rtn.getLbPortNo());
        Route r = routeClient.request(rtn);
        responseObserver.onNext(rtn);
        responseObserver.onCompleted();
    }

    @Override
    protected ByteString process(Route msg) {
        String content = new String(msg.getPayload().toByteArray());
        System.out.println("-- got message from: " + msg.getOrigin() + ", with : " + content);
        byte[] raw = "Request processed by  processed by Server B of CustomQueue 1".getBytes();
        return ByteString.copyFrom(raw);
    }

    private void startHeartBeatProcess(){
        new Thread(()->{

            Route.Builder builder = Route.newBuilder();
            builder.setId(RouteServer.getInstance().getServerPort());
            byte[] raw = "HB".getBytes();
            builder.setPayload(ByteString.copyFrom(raw));
            Route rtn = builder.build();
            while(true) {
                RouteClient routeClient = new RouteClient(2001, 2000);
                Route r = routeClient.request(rtn);

                //routeClient.sendMessage(RouteServer.getInstance().getServerPort(), "/customQueue", "HB",2000);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    public void start() throws Exception {
        this.svr = ServerBuilder.forPort(RouteServer.getInstance().getServerPort()).addService(new ServiceB1()).build();
        System.out.println("-- starting server -----");
        System.out.println("Listening to the port  " + RouteServer.getInstance().getServerPort());
        this.startHeartBeatProcess();
        this.svr.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ServiceB1.this::stop));
    }

}
