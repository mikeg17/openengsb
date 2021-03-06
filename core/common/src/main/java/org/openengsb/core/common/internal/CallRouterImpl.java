/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.core.common.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openengsb.core.common.communication.CallRouter;
import org.openengsb.core.common.communication.MethodCall;
import org.openengsb.core.common.communication.MethodReturn;
import org.openengsb.core.common.communication.OutgoingPort;
import org.openengsb.core.common.util.OsgiServiceNotAvailableException;
import org.openengsb.core.common.util.OsgiServiceUtils;

public class CallRouterImpl implements CallRouter {

    private final Log log = LogFactory.getLog(CallRouterImpl.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void call(String portId, final String destination, final MethodCall call) {
        final OutgoingPort port;
        port = getPort(portId);
        Runnable callHandler = new Runnable() {
            @Override
            public void run() {
                port.send(destination, call);
            }
        };
        executor.execute(callHandler);
    }

    @Override
    public MethodReturn callSync(String portId, final String destination, final MethodCall call) {
        OutgoingPort port;
        port = getPort(portId);
        return port.sendSync(destination, call);
    }

    private OutgoingPort getPort(String portId) throws OsgiServiceNotAvailableException {
        final OutgoingPort port = OsgiServiceUtils.getServiceWithId(OutgoingPort.class, portId);
        return port;
    }

    public void stop() {
        executor.shutdownNow();
    }

}
