import grpc
from concurrent import futures
import time
import route_pb2_grpc as pb2_grpc
import route_pb2 as pb2


class RouteService(pb2_grpc.RouteServiceServicer):

    def __init__(self, *args, **kwargs):
        pass
        self.server_port = 50051

    def request(self, request, context):

        # get the string from the incoming request
        id=request.id
        origin=  self.server_port
        path = request.path
        result = f'reply from received "{path}"message from you'
        result ={'path':result,'origin':origin}
        return pb2.Route(**result)


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pb2_grpc.add_RouteServiceServicer_to_server(RouteService(), server)
    server.add_insecure_port('[::]:50051')
    print("serverstarted")
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    serve()