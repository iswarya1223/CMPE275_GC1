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
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceB extends RouteServerImpl {
    // server B

    
    static class Control{
        public volatile ConcurrentHashMap<String, LocalDateTime> nodes;

        public Control() {
            this.nodes = new ConcurrentHashMap<>();
        }
    }
    static final Control control = new Control();

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

            String content = new String(request.getPayload().toByteArray());
            if (content.equals("HB")) {
                System.out.println("true HB");
                processHeartBeat(Long.toString(request.getId()));
            }
    }
    @Override
    protected ByteString process(Route msg) {
        String content = new String(msg.getPayload().toByteArray());
        System.out.println("-- got: " + msg.getOrigin() + ", path: " + msg.getPath() + ", with: " + content);

        byte[] raw = "From Server B".getBytes();
        return ByteString.copyFrom(raw);
    }

    private void processHeartBeat(String port) {
        System.out.println("Inside line 81");
         Boolean existingMember = control.nodes.containsKey(port);
        System.out.println("1"+existingMember+"port "+port);
        if (!existingMember) {

            control.nodes.putIfAbsent(port, LocalDateTime.now());

        }
            else
            {

                control.nodes.put(port, LocalDateTime.now());

            }
        for (Map.Entry<String, LocalDateTime> entry : control.nodes.entrySet()) {
            String key = entry.getKey().toString();
            LocalDateTime value = entry.getValue();
            System.out.println("From 99 "+"key, " + key + " value " + value);
        }
    }

    private void detectFailedNodes(){
        System.out.println("line 104");
            String[] keys = new String[control.nodes.size()];
            control.nodes.keySet().toArray(keys);
            System.out.println(control.nodes.size()+"keys"+control.nodes.keySet());
            for (String key : keys) {
                System.out.println("line 108");
                boolean hadFailed = checkIfFailed(key);
                if (hadFailed) {
                    synchronized (control.nodes) {
                        control.nodes.remove(key);
                    }
                }
            }
        for (Map.Entry<String, LocalDateTime> entry : control.nodes.entrySet()) {
            String key = entry.getKey();
            LocalDateTime value = entry.getValue();
            System.out.println("From 118 "+"key, " + key + " value " + value);
        }
    }

    private boolean checkIfFailed(String key) {
        LocalDateTime lastUpdateTime = control.nodes.get(key);
        LocalDateTime failureTime = lastUpdateTime.plus(Duration.ofSeconds(3));
        LocalDateTime now = LocalDateTime.now();
        System.out.println("line 125 For Key "+key+ "lastUpdateTime "+ lastUpdateTime +
                "failureTime "+ failureTime+ "failed "+ now.isAfter(failureTime));
        Boolean failed = now.isAfter(failureTime);
        return failed;
    }
    private void startFailureDetectionThread(){
        new Thread(()->{
            System.out.println("failure thread");
            while(true) {
                detectFailedNodes();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        ).start();
    }

    @Override
    public void start() throws Exception {
        this.svr = ServerBuilder.forPort(RouteServer.getInstance().getServerPort()).addService(new ServiceB()).build();
        System.out.println("-- starting server");
        startFailureDetectionThread();
        this.svr.start();
        Runtime.getRuntime().addShutdownHook(new Thread(ServiceB.this::stop));
       // this.startHeartBeatProcess();
    }

}
