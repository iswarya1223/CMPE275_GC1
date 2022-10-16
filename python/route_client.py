import grpc
import route_pb2_grpc as pb2_grpc
import route_pb2 as pb2
import sys
from jproperties import Properties

class RouteClient(object):
    """
    Client for accessing the gRPC functionality
    """

    def __init__(self,host,client_port,server_port,neigbors):
        # configure the host and the
        # the port to which the client should connect
        # to.
        self.host = host
        self.server_port = server_port
        self.client_port = client_port
        self.neigbors=neigbors

        # instantiate a communication channel
        if server_port in neigbors:
            self.channel = grpc.insecure_channel(
            '{}:{}'.format(self.host, self.server_port))
        else:
            self.channel = grpc.insecure_channel(
            '{}:{}'.format(self.host, 2348))
        # bind the client to the server channel
        self.stub = pb2_grpc.RouteServiceStub(self.channel)
    def get_url(self,id,path,payload):
        """
        Client function to call the rpc for GetServerResponse
        """
        data = pb2.Route(id=id,path=path,payload=payload,origin=self.client_port)
        print(f'{data}')
        return self.stub.request(data)
# reading the client properties file
def reading_config_file():
        configs = Properties()
        clientlist=[]
        try:
            with open(sys.argv[1], 'rb') as config_file:
                configs.load(config_file)
                try:
                    if configs.get("host"):
                        host=f'{configs.get("host").data}'
                        clientlist.append(host)
                except Exception as e:
                    print("The client host is not defined")
                try:
                    if configs.get("port"):
                        port=f'{configs.get("port").data}'
                        clientlist.append(int(port))
                except Exception as e:
                    print("The client port is not defined")
                try:
                    if configs.get("serverport"):
                        serverport=f'{configs.get("serverport").data}'
                        clientlist.append(int(serverport))
                except Exception as e:
                    print("The serverport port is not defined")
                try:
                    if configs.get("neighbors"):
                        neighbors=f'{configs.get("neighbors").data}'
                        clientlist.append(list(map(int,neighbors.split(','))))
                except Exception as e:
                    print("The neighbors are not defined")            
            return clientlist
        except Exception as e:
                print("client configuration file is not present")

    


if __name__ == '__main__':
    # print('cmd entry:', sys.argv)
    try:
        clientlist = reading_config_file()
        if clientlist is None or clientlist[0] is None or clientlist[1] is None or clientlist[2] is None or clientlist[3] is None:
            raise Exception
        try:
            i=0
            while i <= 10:
         
                if i % 2 == 0:
                    client = RouteClient(clientlist[0],clientlist[1],clientlist[2],clientlist[3])
                    result = client.get_url(id=i,path="cart service",payload=bytes("I want to see my products", 'utf-8'))
                    print(f'{result}')
                else:
                    client = RouteClient(clientlist[0],clientlist[1],clientlist[2],clientlist[3])
                    result = client.get_url(id=i,path="login service",payload=bytes("I want to login", 'utf-8'))
                    print(f'{result}')
                i+=1
        except Exception as e:
                 print("The port is Down unable to connect")
    except Exception as e:
                 print("The client properties files are not added as accepted")