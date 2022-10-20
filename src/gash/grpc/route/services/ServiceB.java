package gash.grpc.route.services;

import gash.grpc.route.client.RouteClient;
import com.google.protobuf.ByteString;
import gash.grpc.route.heartbeat.service.HeartBeatService;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import route.Route;
import gash.grpc.route.server.RouteServer;
import gash.grpc.route.server.RouteServerImpl;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class ServiceB extends RouteServerImpl {
    // server B
    private final HeartBeatService heartBeatService;

    public ServiceB() {
        heartBeatService = new HeartBeatService(
                new InetSocketAddress(Long.toString(RouteServer.getInstance().getServerID())
                        ,RouteServer.getInstance().getServerPort()));;
    }
    private void startHeartBeatProcess(){
        this.heartBeatService.start();
    }
    public static void main(String[] args) throws Exception {
        String path = args[0];

        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            ServiceB service = new ServiceB();
            System.out.println("serverid"+RouteServer.getInstance().getServerID()+"port"+
                    RouteServer.getInstance().getServerPort());
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
        Route rtn = builder.build();
        responseObserver.onNext(rtn);
        responseObserver.onCompleted();
    }

    @Override
    protected ByteString process(Route msg) {
        String content = new String(msg.getPayload().toByteArray());
        System.out.println("-- got: " + msg.getOrigin() + ", path: " + msg.getPath() + ", with: " + content);

        byte[] raw = "Hi I am Service B.".getBytes();
        return ByteString.copyFrom(raw);
    }

    @Override
    public void start() throws Exception {
        this.svr = ServerBuilder.forPort(RouteServer.getInstance().getServerPort()).addService(new ServiceB()).build();
        System.out.println("-- starting server");
        this.svr.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ServiceB.this::stop));
        this.startHeartBeatProcess();
    }

}
