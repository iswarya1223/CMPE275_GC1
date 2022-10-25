import grpc
from concurrent import futures
import time
import route_pb2_grpc as pb2_grpc
import route_pb2 as pb2
import route_client
from jproperties import Properties
import sys
import _thread
import time 
class RouteService(pb2_grpc.RouteServiceServicer):

    def __init__(self,serverlist,NextMessageID, *args, **kwargs):
        pass
        self.server_port = 1435
        self.serverlist=serverlist
        self.NextMessageID=NextMessageID

    def request(self, request, context):


        # get the string from the incoming request
        
        def request_thread( threadName):
            print(request)
            self.NextMessageID+=1
            id=request.id
            origin=  request.origin
            content = request.payload
            LbPortNo= request.lbPortNo
            result = f'-- got message from: "{origin}"  with : "{content}"'
            print(result)
            client1= route_client.RouteClient(LbPortNo)
            result1 = client1.get_url(id=self.NextMessageID,origin=self.serverlist[0],destination=request.destination,
            path=request.path,payload=bytes("Request processed by  processed by python service of CustomQueue 1", 'utf-8'),
            processedBy=bytes("Request processed by  processed by python service of CustomQueue 1", 'utf-8'),
            isFromClient=False,lbPortNo=request.lbPortNo,clientStartTime=request.clientStartTime,clientPort=request.clientPort)

        try:
            _thread.start_new_thread( request_thread, ("Thread-2", ) )
        except:
            print("Error: unable to start thread")
    
def startHeartBeatProcess(serverlist):
    def heartbeat( threadName):
        while True:
            try: 
                client= route_client.RouteClient(5000)
                result1 = client.get_url1(id=serverlist[1],payload=bytes("HB", 'utf-8'))
            except:
                print("unable to connect to load balancer")
            try: 
                time.sleep(3)
            except:
                print("Error: in the Thread")
            
    try:
        _thread.start_new_thread( heartbeat, ("Thread-1", ) )
    except:
        print("Error: unable to start thread")

def serve(serverlist,NextMessageID):
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pb2_grpc.add_RouteServiceServicer_to_server(RouteService(serverlist,NextMessageID), server)
    server.add_insecure_port('[::]:{}'.format(serverlist[1]))
    print("serverstarted")
    startHeartBeatProcess(serverlist)
    server.start()
    server.wait_for_termination()
def reading_config_file():
        configs = Properties()
        serverlist=[]
        try:
            with open(sys.argv[1], 'rb') as config_file:
                configs.load(config_file)
                try:
                    if configs.get("server.id"):
                        id=f'{configs.get("server.id").data}'
                        serverlist.append(int(id))
                except Exception as e:
                    print("The server id is not defined")
                try:
                    if configs.get("server.port"):
                        port=f'{configs.get("server.port").data}'
                        serverlist.append(int(port))
                except Exception as e:
                    print("The server port is not defined")        
            return serverlist
        except Exception as e:
                print("server configuration file is not present")

if __name__ == '__main__':
    try:
        serverlist = reading_config_file()
        NextMessageID=0
        print(serverlist)
        if serverlist is None or serverlist[0] is None or serverlist[1] is None:
            raise Exception
        serve(serverlist,NextMessageID)
    except Exception as e:
                 print("The server properties files are not added as accepted")
