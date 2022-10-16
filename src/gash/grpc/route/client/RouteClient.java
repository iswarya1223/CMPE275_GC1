package gash.grpc.route.client;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import route.Route;
import route.RouteServiceGrpc;
import route.RouteServiceGrpc.RouteServiceBlockingStub;

/**
 * copyright 2021, gash
 *
 * Gash licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

public class RouteClient {
    ManagedChannel ch;
    RouteServiceBlockingStub stub;
    private long clientID;
    private int port;

    // private queue que obkjectname
    // this queue=queue
    // queue
    public RouteClient(long clientId, int port) {
        this.clientID = clientId;
        this.port = port;
        this.ch = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        this.stub = RouteServiceGrpc.newBlockingStub(ch);
    }

    public long getClientID() {
        return clientID;
    }

    public void setClientID(long clientID) {
        this.clientID = clientID;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private Route constructMessage(int mID, String path, String payload) {
        Route.Builder bld = Route.newBuilder();
        bld.setId(mID);
        bld.setOrigin(clientID);
        bld.setPath(path);

        byte[] hello = payload.getBytes();
        bld.setPayload(ByteString.copyFrom(hello));

        return bld.build();
    }

    private void response(Route reply) {
        // TODO handle the reply/response from the server
        var payload = new String(reply.getPayload().toByteArray());
        System.out.println("reply: " + reply.getId() + ", from: " + reply.getOrigin() + ", payload: " + payload);
    }

    private Route request(Route msg) {
        return stub.request(msg);
    }

    private void shutdown() {
        ch.shutdown();
    }

    public void sendMessage(int mId, String path, String message) {
        Route msg = constructMessage(mId, path, message);
        Route r = request(msg);
        response(r);
        shutdown();
    }

    public static void main(String[] args) {
        RouteClient routeClient = new RouteClient(1314, 2345);
        routeClient.sendMessage(1, "/serverA", "route");
    }
}
