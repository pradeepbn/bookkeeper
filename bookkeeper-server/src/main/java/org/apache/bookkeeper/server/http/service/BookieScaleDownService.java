package org.apache.bookkeeper.server.http.service;

import lombok.NonNull;
import org.apache.bookkeeper.bookie.Bookie;
import org.apache.bookkeeper.bookie.StateManager;
import org.apache.bookkeeper.http.HttpServer;
import org.apache.bookkeeper.http.service.HttpEndpointService;
import org.apache.bookkeeper.http.service.HttpServiceRequest;
import org.apache.bookkeeper.http.service.HttpServiceResponse;
import org.apache.bookkeeper.zookeeper.ZooKeeperClient;
import org.apache.zookeeper.ZKUtil;

public class BookieScaleDownService implements HttpEndpointService {
    @NonNull
    private final Bookie bookie;

    public BookieScaleDownService(Bookie bookie) { this.bookie = bookie; }

    @Override
    public HttpServiceResponse handle(HttpServiceRequest request) throws Exception {
        HttpServiceResponse response = new HttpServiceResponse();
        StateManager st = bookie.getStateManager();
        if (request.getMethod() != HttpServer.Method.POST) {
            response.setCode(HttpServer.StatusCode.METHOD_NOT_ALLOWED);
            return response;
        }
        // During scale down first transition to read only mode so that the writes are not going through while draining
        st.transitionToReadOnlyMode();
        // Transition to the draining state
        st.transitionToDrainingMode();
//        st.
        System.out.println("hello world from BookieScaleDownService");
//        ZooKeeperCluster zkUtil = new ZKUtil();
//        ZooKeeperClient zkClient = ZooKeeperClient.newBuilder().connectString(zkUtil.get);
        // This should set the bookie to readonly in the zk
        // This should set the bookie to under_replication in the zk
        // This should kick off draining the bookie
        response.setBody("Successfully hit Scale down service");
        response.setCode(HttpServer.StatusCode.OK);
        return response;
    }
}
