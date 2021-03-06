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

import java.io.Serializable;

import org.apache.mailet.Mail;

public class DeliveryRetriesHelper {

    public static final String DELIVERY_RETRY_COUNT = "delivery_retry_count";

    public static int retrieveRetries(Mail mail) {
        try {
            Serializable value = mail.getAttribute(DELIVERY_RETRY_COUNT);
            if (value != null) {
                return (Integer) value;
            }
            return 0;
        } catch (ClassCastException e) {
            return 0;
        }
    }

    public static void initRetries(Mail mail) {
        mail.setAttribute(DELIVERY_RETRY_COUNT, 0);
    }

    public static void incrementRetries(Mail mail) {
        mail.setAttribute(DELIVERY_RETRY_COUNT, retrieveRetries(mail) + 1);
    }

}
