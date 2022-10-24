package gash.grpc.route.client;

import com.google.protobuf.ByteString;
import gash.grpc.route.server.RouteServer;
import gash.grpc.route.server.RouteServerImpl;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import route.Route;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;

public class AClient extends RouteServerImpl {
    // server B
    public static void main(String[] args) throws Exception {
        String path = args[0];

        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            AClient service = new AClient();
            service.start();
            //Adding logic to send messages to load balancer
            //todo : automate the sending messages part for testing
            //port number for load balancer is hardcoded
            //todo : add handling for sending messages to multiple load balancers
            RouteClient routeClient = new RouteClient(1314, 2344);

            //generating input message
            Route.Builder bld = Route.newBuilder();
            bld.setId(Long.parseLong(conf.getProperty("server.id")));
            bld.setOrigin(Long.parseLong(conf.getProperty("server.id")));
            bld.setClientPort(Long.parseLong(conf.getProperty("server.port")));
            bld.setClientStartTime(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
            bld.setIsFromClient(true);
            bld.setLbPortNo(2344);

            //todo : automate hello message
            byte[] hello = "this is message from AClient".getBytes();
            bld.setPayload(ByteString.copyFrom(hello));

            Route msg = bld.build();
            Route reply = routeClient.request(msg);
            //Route reply = routeClient.sendMessage(1, "/customQueue", "route", 2344);
            var replyPayload = new String(reply.getPayload().toByteArray());
            System.out.println("reply: " + reply.getId() + ", from: " + reply.getOrigin() + ", payload: " + replyPayload);

            ////////////////////////////////
            service.blockUntilShutdown();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

    }

    @Override
    public void request(Route request, StreamObserver<Route> responseObserver) {
        //todo : Utkarsh handle the response from load balacer and use it for testing
        Route.Builder builder = Route.newBuilder();
        System.out.println("received reply from CQ");
        System.out.println("processed by " + request.getLbPortNo());

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
        this.svr = ServerBuilder.forPort(RouteServer.getInstance().getServerPort()).addService(new AClient()).build();
        System.out.println("-- starting server -----");
        System.out.println("Listening to the port  " + RouteServer.getInstance().getServerPort());
        this.svr.start();
        Runtime.getRuntime().addShutdownHook(new Thread(AClient.this::stop));
    }

}

