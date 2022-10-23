package gash.grpc.route.client;


public class ClientA extends RouteClient{

    public ClientA(long clientId, int portToSend,int clientPort) {
        super(clientId,portToSend);

    }

    public static void main(String[] args){
        ClientA routeClient = new ClientA(1314, 2344,1111);
        System.out.println("Doing other task as well");
        routeClient.sendMessage(1, "/serverA", "route");
    }
}
