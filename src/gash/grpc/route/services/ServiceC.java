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

public class ServiceC extends RouteServerImpl {
    // server C
    public static void main(String[] args) throws Exception {
        String path = args[0];

        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            ServiceC service = new ServiceC();
            service.start();
            service.blockUntilShutdown();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

    }

    private void startHeartBeatProcess(){
        new Thread(()->{
            while(true) {
                RouteClient routeClient = new RouteClient(1315, 2346);
                routeClient.sendMessage(RouteServer.getInstance().getServerPort(), "/serverB12", "HB");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

    }

    @Override
    public void request(Route request, StreamObserver<Route> responseObserver) {
        Route.Builder builder = Route.newBuilder();
        builder.setId(RouteServer.getInstance().getNextMessageID());
        builder.setOrigin(RouteServer.getInstance().getServerID());
        builder.setDestination(request.getOrigin());
        builder.setPath(request.getPath());
        builder.setPayload(this.process(request));
        Route rtn = builder.build();
        responseObserver.onNext(rtn);
        responseObserver.onCompleted();
    }

    @Override
    protected ByteString process(Route msg) {
        String content = new String(msg.getPayload().toByteArray());
        System.out.println("-- got: " + msg.getOrigin() + ", path: " + msg.getPath() + ", with: " + content);

        byte[] raw = "Hi I am Service C.".getBytes();
        return ByteString.copyFrom(raw);
    }

    @Override
    public void start() throws Exception {
        this.svr = ServerBuilder.forPort(RouteServer.getInstance().getServerPort()).addService(new ServiceC()).build();
        System.out.println("-- starting server");
        this.svr.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ServiceC.this::stop));
        this.startHeartBeatProcess();
    }

}
