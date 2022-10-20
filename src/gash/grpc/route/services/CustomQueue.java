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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

public class CustomQueue extends RouteServerImpl {
    private static final long serverId = 1;
    private LinkedBlockingDeque<Route> messageList;
    private List<Integer> listOfServerIds;

    public CustomQueue() {
        this.messageList = new LinkedBlockingDeque<>();
        this.listOfServerIds = new ArrayList<>();
        this.listOfServerIds.add(2246);
    }
    public boolean putMessage(Route route){
        try{
            this.messageList.add(route);
            return true;
        }catch (Exception e){
            return false;
        }

    }

    public Route takeMessage(){
        try {
            return this.messageList.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static final class ConsumeMessages extends Thread {

        public boolean _isRunning = true;
        public LinkedBlockingDeque<Route> messageList;

        public List<Integer> listOfServerIds;

        public ConsumeMessages(LinkedBlockingDeque<Route> messageList, List<Integer> listOfServerIds) {
            this.messageList = messageList;
            this.listOfServerIds = listOfServerIds;
        }

        public void shutdown() {
            _isRunning = false;
        }

        @Override
        public void run() {
            while (_isRunning) {
                try {
                    if(messageList.size()>0 && this.listOfServerIds.size()>0) {
                        int destinationServerPort = (int) ((Math.random() * (listOfServerIds.size() - 0)) + 0);
                        Route msg = messageList.take();

                        RouteClient routeClient = new RouteClient(serverId, destinationServerPort);
                        routeClient.sendMessage(1, "/serverB", "Message is sent via customQueue");

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

        String content = new String(request.getPayload().toByteArray());
        String path = request.getPath();
        long origin = request.getOrigin();

        builder.setPayload(this.process(request));
        Route rtn = builder.build();
        //Adding message to Custom Queue here
        putMessage(rtn);

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
        System.out.println("-- starting server-----");
        System.out.println("Listening to the port  " + RouteServer.getInstance().getServerPort());
        //todo : add thread heartbeat


        //todo : add thread for monitor it will check queue status in 20 sec
        this.svr.start();
        //todo : add thread for take
        ConsumeMessages con = new ConsumeMessages(this.messageList, this.listOfServerIds);
        con.start();
        Runtime.getRuntime().addShutdownHook(new Thread(CustomQueue.this::stop));
    }
}
