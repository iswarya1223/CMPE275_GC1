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
import java.util.concurrent.ConcurrentHashMap;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class CustomQueue extends RouteServerImpl {
    static class Control {
        public volatile  LinkedBlockingDeque<Route> inBoundQueue;
        public volatile LinkedBlockingDeque<Route> outBoundQueue;
        public volatile  List<Integer> listOfServerIds;
        public volatile List<String> usedServers=new ArrayList<>();

        public volatile ConcurrentHashMap<String, LocalDateTime> nodes;
        public Control() {
            this.inBoundQueue = new LinkedBlockingDeque<>();
            this.outBoundQueue = new LinkedBlockingDeque<>();
            this.listOfServerIds = new ArrayList<>();
            this.nodes = new ConcurrentHashMap<>();
            this.listOfServerIds.add(2001);
        }
    }
    static final Control control = new Control();
    private static final long serverId = 1;
    public CustomQueue() {
    }

    public static void main(String[] args) throws Exception {
        String path = args[0];
        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            CustomQueue customQueue = new CustomQueue();
            System.out.println("serverid"+RouteServer.getInstance().getServerID()+"port"+
                    RouteServer.getInstance().getServerPort());
            customQueue.start();
            customQueue.blockUntilShutdown();
        } catch (IOException var4) {
            var4.printStackTrace();
        }
    }

    public static final class ConsumeMessages extends Thread {
        public boolean _isRunning = true;
        public ConsumeMessages() {}
        public void shutdown() {
            _isRunning = false;
        }
        @Override
        public void run() {
            while (_isRunning) {
                try {
                    if(control.inBoundQueue.size()>0 && control.nodes.size()>0) {
                        int destinationServerPort=0;
                        while(destinationServerPort==0) {
                            for (String key : control.nodes.keySet()) {
                                if (!control.usedServers.contains(key)) {
                                    destinationServerPort = Integer.parseInt(key);
                                    control.usedServers.add((key));
                                    break;
                                }
                            }
                            if (destinationServerPort==0)
                            {
                                control.usedServers.clear();
                            }
                        }
                        System.out.println(destinationServerPort);
                        Route msg = control.inBoundQueue.take();
                        Route.Builder builder = Route.newBuilder(msg);
                        builder.setInboundQueueExitTime(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
                        Route modifiedMsg = builder.build();
                        RouteClient routeClient = new RouteClient(serverId, destinationServerPort);
                        Route r = routeClient.request(modifiedMsg);
                    }else{
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    // ignore - part of the test
                }
            }
        }
    }

    public static final class ConsumeOutBoundMessages extends Thread {
        public boolean _isRunning = true;
        public ConsumeOutBoundMessages() {}
        public void shutdown() {
            _isRunning = false;
        }
        @Override
        public void run() {
            while (_isRunning) {
                try {
                    if(control.outBoundQueue.size()>0 ) {
                        Route msg = control.outBoundQueue.take();
                        Route.Builder builder = Route.newBuilder(msg);
                        builder.setOutboundQueueExitTime(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
                        Route modifiedMsg = builder.build();
                        RouteClient routeClient = new RouteClient(serverId, (int) msg.getClientPort());
                        Route r = routeClient.request(modifiedMsg);
                        r.getClientPort();
                    }else{
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    // ignore - part of the test
                }
            }
        }
    }
    @Override
    public void request(Route request, StreamObserver<Route> responseObserver) {

        Route.Builder builder = Route.newBuilder();
        builder.setId(request.getId());
        builder.setOrigin(RouteServer.getInstance().getServerID());
        builder.setDestination(request.getOrigin());
        builder.setPath(request.getPath());
        builder.setLbPortNo(request.getLbPortNo());
        builder.setClientStartTime(request.getClientStartTime());
        builder.setClientPort(request.getClientPort());
        builder.setIsFromClient(request.getIsFromClient());

        String content = new String(request.getPayload().toByteArray());

        String path = request.getPath();
        long origin = request.getOrigin();
        builder.setPayload(this.process(request));
        Route rtn = null;
        if (content.equals("HB")) {
            processHeartBeat(Long.toString(request.getId()));
            responseObserver.onNext(rtn);
            responseObserver.onCompleted();
        }
        else if(request.getIsFromClient()){
            builder.setInboundQueueEntryTime(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
            rtn = builder.build();
            control.inBoundQueue.add(rtn);
            responseObserver.onNext(rtn);
            responseObserver.onCompleted();
        }
        else {
            builder.setOutboundQueueEntryTime(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
            rtn = builder.build();
            control.outBoundQueue.add(rtn);
            responseObserver.onNext(rtn);
            responseObserver.onCompleted();
        }
    }
    private void processHeartBeat(String port) {
        Boolean existingMember = control.nodes.containsKey(port);
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
        }
    }
    private void detectFailedNodes(){
        String[] keys = new String[control.nodes.size()];
        control.nodes.keySet().toArray(keys);
        for (String key : keys) {
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
        }
    }
    private boolean checkIfFailed(String key) {
        LocalDateTime lastUpdateTime = control.nodes.get(key);
        LocalDateTime failureTime = lastUpdateTime.plus(Duration.ofSeconds(3));
        LocalDateTime now = LocalDateTime.now();
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
    protected ByteString process(Route msg) {
        String content = new String(msg.getPayload().toByteArray());
        System.out.println("-- got from: " + msg.getOrigin() + ", with: " + content);

        byte[] raw = "Custom Queue 1 forwarded message".getBytes();
        return ByteString.copyFrom(raw);
    }

    @Override
    public void start() throws Exception {
        this.svr = ServerBuilder.forPort(RouteServer.getInstance().getServerPort()).addService(new CustomQueue()).build();
        System.out.println("-- starting server-----");
        System.out.println("Listening to the port  " + RouteServer.getInstance().getServerPort());
        //todo : add thread heartbeat
        startFailureDetectionThread();
        //todo : add thread for monitor it will check queue status in 20 sec
        this.svr.start();
        //todo : add thread for take
        ConsumeMessages con = new ConsumeMessages();
        con.start();
        ConsumeOutBoundMessages outBoundMessagesThread = new ConsumeOutBoundMessages();

        outBoundMessagesThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(CustomQueue.this::stop));
    }
}
