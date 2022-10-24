import grpc
import route_pb2_grpc as pb2_grpc
import route_pb2 as pb2
import sys
from concurrent import futures
from jproperties import Properties

class RouteClient(object):
    """
    Client for accessing the gRPC functionality
    """

    def __init__(self,server_port):
        # configure the host and the
        # the port to which the client should connect
        # to.

        # instantiate a communication channel
        self.server_port = server_port
        self.channel = grpc.insecure_channel(
            '{}:{}'.format('localhost', self.server_port))
        # bind the client to the server channel
        self.stub = pb2_grpc.RouteServiceStub(self.channel)
    def get_url(self,id,origin,destination,path,payload,processedBy,isFromClient,lbPortNo,clientStartTime,clientPort):
        """
        Client function to call the rpc for GetServerResponse
        """
        data = pb2.Route(id=id,origin=origin,destination=destination,path=path,payload=payload,
        processedBy=processedBy,isFromClient=isFromClient,lbPortNo=lbPortNo,clientStartTime=clientStartTime,clientPort=clientPort)
        print(f'{data}')
        return self.stub.request(data)
    def get_url1(self,id,payload):
        """
        Client function to call the rpc for GetServerResponse
        """
        data = pb2.Route(id=id,payload=payload)
        return self.stub.request(data)
# reading the client properties file



if __name__ == '__main__':
    RouteClient(2345)
    # print('cmd entry:', sys.argv)
    # client = RouteClient(2345)
    # result = client.get_url(id=1,path="cart service",payload=bytes("I want to see my products", 'utf-8'))
    # print(f'{result}')