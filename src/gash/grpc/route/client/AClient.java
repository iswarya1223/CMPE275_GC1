package gash.grpc.route.client;

import com.google.protobuf.ByteString;
import gash.grpc.route.server.RouteServer;
import gash.grpc.route.server.RouteServerImpl;
import gash.grpc.route.services.A.CustomQueueA;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import route.Route;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

public class AClient extends RouteServerImpl {
    static class Control {
        public volatile long startTime;
        public volatile int numberOfMessages;
        public volatile int counter;
    }
    static final Control control = new Control();
    public static void main(String[] args) throws Exception {
        String path = args[0];

        //todo : Available load balancers reading from a file (loadbalancers currently hardcoded)
        List<Integer> availableLoadBalancers=new ArrayList<>();

        //Adding Load Balancers A, B & C
        availableLoadBalancers.add(2000);
        //availableLoadBalancers.add(3000);
        //availableLoadBalancers.add(4000);
        //availableLoadBalancers.add(5000);

        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            AClient service = new AClient();
            service.start();

            control.counter = 0;

            control.numberOfMessages = 1000;

            control.startTime = System.currentTimeMillis();//this is start time for n number of messages
            for(int i=0;i<control.numberOfMessages;i++)
            {
                for(int lbPortNo:availableLoadBalancers)
                {
                    RouteClient routeClient = new RouteClient(1025, lbPortNo);
                    Route msg = getRoute(lbPortNo,"from Client A",conf, i);
                    Route reply = routeClient.request(msg);
                    var replyPayload = new String(reply.getPayload().toByteArray());
                    System.out.println("reply: " + reply.getId() + ", from: " + reply.getOrigin() + ", payload: " + replyPayload);
                }
            }
            service.blockUntilShutdown();
        } catch (IOException var4) {
            var4.printStackTrace();
        }

    }

    @Override
    public void request(Route request, StreamObserver<Route> responseObserver) {
        route.Route.Builder builder = route.Route.newBuilder();

        // routing/header information
        builder.setId(RouteServer.getInstance().getNextMessageID());
        builder.setOrigin(RouteServer.getInstance().getServerID());
        builder.setDestination(request.getOrigin());
        builder.setPath(request.getPath());

        // do the work and reply
        builder.setPayload(process(request));

        route.Route rtn = builder.build();
        responseObserver.onNext(rtn);
        responseObserver.onCompleted();


        control.counter++;
        long responseReceivedTime = System.currentTimeMillis();

        long startTime = Long.parseLong(request.getClientStartTime());
        //System.out.println("received reply from CQ for messageId : " + request.getId() );
        //System.out.println("processed by load balancer with port no " + request.getLbPortNo());
        //System.out.println("counter value is " + control.counter);
        if(control.counter == control.numberOfMessages)
            System.out.println("Round trip time is : " + ( responseReceivedTime - control.startTime) );
    }

    private static Route getRoute(int lbPortNo, String msg, Properties conf, int msgId) {
        //Generating an input message
        Route.Builder bld = Route.newBuilder();
        bld.setId(msgId);
        bld.setOrigin(Long.parseLong(conf.getProperty("server.id")));
        bld.setClientPort(Long.parseLong(conf.getProperty("server.port")));
        bld.setClientStartTime(String.valueOf(System.currentTimeMillis()));
        bld.setIsFromClient(true);
        bld.setLbPortNo(lbPortNo);
        bld.setPayload(ByteString.copyFrom(msg.getBytes()));
        Route message = bld.build();
        return message;
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

