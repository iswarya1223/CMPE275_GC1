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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

public class CustomQueue extends RouteServerImpl {

    static class Control {
        public volatile  LinkedBlockingDeque<Route> inBoundQueue;
        public volatile LinkedBlockingDeque<Route> outBoundQueue;
        public volatile  List<Integer> listOfServerIds;

        public Control() {
            this.inBoundQueue = new LinkedBlockingDeque<>();
            this.outBoundQueue = new LinkedBlockingDeque<>();
            this.listOfServerIds = new ArrayList<>();
            this.listOfServerIds.add(2346);
            //this.listOfServerIds.add(2347);
        }
    }

    static final Control control = new Control();

    private static final long serverId = 1;


    public CustomQueue() {
    }
    public boolean putMessage(Route route){
        try{
            control.inBoundQueue.add(route);
            return true;
        }catch (Exception e){
            return false;
        }

    }

    public Route takeMessage(){
        try {
            return control.inBoundQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
            System.out.println("inside run method for consume messages");
            System.out.println("initial value for is running is " + _isRunning);
            while (_isRunning) {
                try {
                    if(control.inBoundQueue.size()>0 && control.listOfServerIds.size()>0) {
                        System.out.println("inside consume message if condition");
                        int destinationServerPort = control.listOfServerIds.get((int) ((Math.random() * (control.listOfServerIds.size() - 0)) + 0)) ;
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
            System.out.println("inside run method for ConsumeOutBoundMessages ");
            System.out.println("initial value for ConsumeOutBoundMessages is running is " + _isRunning);
            while (_isRunning) {
                try {
                    if(control.outBoundQueue.size()>0 ) {
                        System.out.println("inside ConsumeOutBoundMessages if condition");
                        int destinationServerPort = control.listOfServerIds.get((int) ((Math.random() * (control.listOfServerIds.size() - 0)) + 0)) ;
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



    // server a
    // private queue que obkjectname
    // this queue=queue
    // queue
    public static void main(String[] args) throws Exception {
        String path = args[0];

        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            CustomQueue customQueue = new CustomQueue();
            customQueue.start();
            customQueue.blockUntilShutdown();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

    }

    @Override
    public void request(Route request, StreamObserver<Route> responseObserver) {

        //Write code here for put method
        //taking the input and creating a route object and storing it in the queue
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
//here make sure the process request is handled by server not by load balancer
        builder.setPayload(this.process(request));
        Route rtn = null;
        //Adding message to Custom Queue here
        if(request.getIsFromClient()){
            builder.setInboundQueueEntryTime(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
            rtn = builder.build();
            control.inBoundQueue.add(rtn);
            responseObserver.onNext(rtn);
            responseObserver.onCompleted();
        }else {
            builder.setOutboundQueueEntryTime(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));

            rtn = builder.build();
            control.outBoundQueue.add(rtn);
            responseObserver.onNext(rtn);
            responseObserver.onCompleted();
        }
        //putMessage(rtn);

//        responseObserver.onNext(rtn);
//        responseObserver.onCompleted();
//send ack to client
//        if (content.equals("route")) {
//            Route r = routeMessageToDestinationServer(origin, path, content + "forwarded from Server A");
//        }
    }

    private Route routeMessageToDestinationServer(long origin, String path, String content) {
        int routePort = ServiceRouting.route(path);
        RouteClient routeClient = new RouteClient(serverId, routePort);
        Route responseObj = routeClient.sendMessage(1, "/serverB", content, 2344);
        return responseObj;
    }

    private void sendServiceB_Ack() {
        RouteClient routeClient = new RouteClient(1315, 2346);
        routeClient.sendMessage(1, "/serverB", "message from server A " + routeClient.getClientID(), 2344);
    }

    @Override
    protected ByteString process(Route msg) {
        String content = new String(msg.getPayload().toByteArray());
        System.out.println("-- got: From" + msg.getOrigin() + ", path: " + msg.getPath() + ", with: " + content);

        byte[] raw = "Hi I am Custom Queue".getBytes();
        return ByteString.copyFrom(raw);
    }

    @Override
    public void start() throws Exception {
        this.svr = ServerBuilder.forPort(RouteServer.getInstance().getServerPort()).addService(new CustomQueue()).build();
        System.out.println("-- starting server-----");
        System.out.println("Listening to the port  " + RouteServer.getInstance().getServerPort());
        //todo : add thread heartbeat


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
