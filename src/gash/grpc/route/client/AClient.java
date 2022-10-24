package gash.grpc.route.client;

import com.google.protobuf.ByteString;
import gash.grpc.route.server.RouteServer;
import gash.grpc.route.server.RouteServerImpl;
import io.grpc.ServerBuilder;
import route.Route;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AClient extends RouteServerImpl {
    public static void main(String[] args) throws Exception {
        String path = args[0];

        //todo : Available load balancers reading from a file (loadbalancers currently hardcoded)
        List<Integer> availableLoadBalancers=new ArrayList<>();

        //Adding Load Balancers A, B & C
        availableLoadBalancers.add(2000);
        availableLoadBalancers.add(3000);
        availableLoadBalancers.add(4000);

        try {
            Properties conf = getConfiguration(new File(path));
            RouteServer.configure(conf);
            AClient service = new AClient();
            service.start();
            for(int i=0;i<5;i++)
            {
                for(int lbPortNo:availableLoadBalancers)
                {
                                RouteClient routeClient = new RouteClient(1025, lbPortNo);
                                Route msg = getRoute(lbPortNo,"from Client A",conf);
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

    private static Route getRoute(int lbPortNo,String msg,Properties conf) {
        //Generating an input message
        Route.Builder bld = Route.newBuilder();
        bld.setId(Long.parseLong(conf.getProperty("server.id")));
        bld.setOrigin(Long.parseLong(conf.getProperty("server.id")));
        bld.setClientPort(Long.parseLong(conf.getProperty("server.port")));
        bld.setClientStartTime(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
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

