package gash.grpc.route.services;

public class ServiceRouting {

    public static int route(String path) {
        if (path.equals("/serverA")) {
            return 2346;
        } else if (path.equals("/serverB")) {
            return 2347;
        } else if (path.equals("/serverC")) {
            return 2345;
        }

        return 0;
    }
}
