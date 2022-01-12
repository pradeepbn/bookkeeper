package org.apache.bookkeeper.server.http.service;

import lombok.NonNull;
import org.apache.bookkeeper.bookie.Bookie;
import org.apache.bookkeeper.bookie.StateManager;
import org.apache.bookkeeper.http.HttpServer;
import org.apache.bookkeeper.http.service.HttpEndpointService;
import org.apache.bookkeeper.http.service.HttpServiceRequest;
import org.apache.bookkeeper.http.service.HttpServiceResponse;
import org.apache.bookkeeper.meta.LedgerManager;
import org.apache.bookkeeper.meta.LedgerManagerFactory;
import org.apache.bookkeeper.zookeeper.ZooKeeperClient;
import org.apache.zookeeper.ZKUtil;

public class BookieScaleDownService implements HttpEndpointService {
    @NonNull
    private final Bookie bookie;
    private LedgerManagerFactory ledgerManagerFactory;

    public BookieScaleDownService(Bookie bookie, LedgerManagerFactory ledgerManagerFactory) {
        this.bookie = bookie;
        this.ledgerManagerFactory = ledgerManagerFactory;
    }

    @Override
    public HttpServiceResponse handle(HttpServiceRequest request) throws Exception {
        HttpServiceResponse response = new HttpServiceResponse();
        StateManager st = bookie.getStateManager();
        if (request.getMethod() != HttpServer.Method.POST) {
            response.setCode(HttpServer.StatusCode.METHOD_NOT_ALLOWED);
            return response;
        }
        LedgerManager ledgerManger = ledgerManagerFactory.newLedgerManager();
        LedgerManager.LedgerRangeIterator iter = ledgerManger.getLedgerRanges(0);
        int minEnsemble = 0;
        while (iter.hasNext()) {
            LedgerManager.LedgerRange ledgerRange = iter.next();
            for (Long lid : ledgerRange.getLedgers()) {
                if (minEnsemble != 0) {
                    minEnsemble = Math.max(minEnsemble, ledgerManger.readLedgerMetadata(lid).get().getValue().getEnsembleSize());
                } else {
                    minEnsemble = ledgerManger.readLedgerMetadata(lid).get().getValue().getEnsembleSize();
                }
            }
        }
        if (minEnsemble == 1) {
            response.setBody("Cannot scale down the bookie because there is atleast 1 ledger that has maxEnsembleSize = 1");
            response.setCode(HttpServer.StatusCode.OK);
            return response;
        }
        // During scale down first transition to read only mode so that the writes are not going through while draining
        st.transitionToReadOnlyMode();
        // Transition to the draining state
        st.transitionToDrainingMode();
        response.setBody("Successfully hit Scale down service");
        response.setCode(HttpServer.StatusCode.OK);
        return response;
    }
}
