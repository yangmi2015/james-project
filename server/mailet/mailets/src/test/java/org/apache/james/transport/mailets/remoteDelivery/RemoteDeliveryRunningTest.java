/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.mailets.remoteDelivery;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.transport.mailets.RemoteDelivery;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class RemoteDeliveryRunningTest {

    public static final String QUEUE_NAME = "queueName";
    private RemoteDelivery remoteDelivery;
    private MailQueue mailQueue;
    private CountDownLatch countDownLatch;

    @Before
    public void setUp() throws Exception {
        countDownLatch = new CountDownLatch(1);
        MailQueueFactory mailQueueFactory = mock(MailQueueFactory.class);
        remoteDelivery = new RemoteDelivery(mock(DNSService.class), mock(DomainList.class), mailQueueFactory,
            mock(MetricFactory.class), RemoteDelivery.THREAD_STATE.START_THREADS);

        mailQueue = mock(MailQueue.class);
        when(mailQueueFactory.getQueue(QUEUE_NAME)).thenReturn(mailQueue);
    }

    @Test
    public void remoteDeliveryShouldStart() throws Exception {
        when(mailQueue.deQueue()).thenAnswer(new Answer<MailQueue.MailQueueItem>() {
            @Override
            public MailQueue.MailQueueItem answer(InvocationOnMock invocation) throws Throwable {
                countDownLatch.countDown();
                Thread.sleep(TimeUnit.SECONDS.toMillis(20));
                return null;
            }
        });
        remoteDelivery.init(FakeMailetConfig.builder()
            .setProperty(RemoteDeliveryConfiguration.DELIVERY_THREADS, "1")
            .setProperty(RemoteDeliveryConfiguration.OUTGOING, QUEUE_NAME)
            .setProperty(RemoteDeliveryConfiguration.HELO_NAME, "Hello_name")
            .build());

        countDownLatch.await();
        verify(mailQueue).deQueue();
    }

    @After
    public void tearDown() throws InterruptedException {
        remoteDelivery.destroy();
    }

}
