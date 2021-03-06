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
package org.apache.james.jmap.model;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.jmap.model.mailbox.Mailbox;
import org.apache.james.jmap.model.mailbox.Role;
import org.apache.james.jmap.model.mailbox.SortOrder;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxCounters;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxPath;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

public class MailboxFactory {
    private final MailboxManager mailboxManager;

    public static class MailboxBuilder {
        private final MailboxFactory mailboxFactory;
        private MailboxSession session;
        private MailboxId id;
        private List<MailboxMetaData> userMailboxesMetadata;

        private MailboxBuilder(MailboxFactory mailboxFactory) {
            this.mailboxFactory = mailboxFactory;
        }

        public MailboxBuilder id(MailboxId id) {
            this.id = id;
            return this;
        }

        public MailboxBuilder session(MailboxSession session) {
            this.session = session;
            return this;
        }

        public MailboxBuilder usingPreloadedMailboxesMetadata(List<MailboxMetaData> userMailboxesMetadata) {
            this.userMailboxesMetadata = userMailboxesMetadata;
            return this;
        }

        public Optional<Mailbox> build() {
            Preconditions.checkNotNull(id);
            Preconditions.checkNotNull(session);

            try {
                MessageManager mailbox = mailboxFactory.mailboxManager.getMailbox(id, session);
                return mailboxFactory.fromMessageManager(mailbox, Optional.ofNullable(userMailboxesMetadata), session);
            } catch (MailboxException e) {
                return Optional.empty();
            }
        }
    }

    @Inject
    public MailboxFactory(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    public MailboxBuilder builder() {
        return new MailboxBuilder(this);
    }

    private Optional<Mailbox> fromMessageManager(MessageManager messageManager, Optional<List<MailboxMetaData>> userMailboxesMetadata,
                                                 MailboxSession mailboxSession) throws MailboxException {
        MailboxPath mailboxPath = messageManager.getMailboxPath();
        Optional<Role> role = Role.from(mailboxPath.getName());
        MailboxCounters mailboxCounters = messageManager.getMailboxCounters(mailboxSession);
        return Optional.ofNullable(Mailbox.builder()
                .id(messageManager.getId())
                .name(getName(mailboxPath, mailboxSession))
                .parentId(getParentIdFromMailboxPath(mailboxPath, userMailboxesMetadata, mailboxSession).orElse(null))
                .role(role)
                .unreadMessages(mailboxCounters.getUnseen())
                .totalMessages(mailboxCounters.getCount())
                .sortOrder(SortOrder.getSortOrder(role))
                .build());
    }

    @VisibleForTesting String getName(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        String name = mailboxPath.getName();
        if (name.contains(String.valueOf(mailboxSession.getPathDelimiter()))) {
            List<String> levels = Splitter.on(mailboxSession.getPathDelimiter()).splitToList(name);
            return levels.get(levels.size() - 1);
        }
        return name;
    }

    @VisibleForTesting Optional<MailboxId> getParentIdFromMailboxPath(MailboxPath mailboxPath, Optional<List<MailboxMetaData>> userMailboxesMetadata,
                                                                      MailboxSession mailboxSession) throws MailboxException {
        List<MailboxPath> levels = mailboxPath.getHierarchyLevels(mailboxSession.getPathDelimiter());
        if (levels.size() <= 1) {
            return Optional.empty();
        }
        MailboxPath parent = levels.get(levels.size() - 2);
        return userMailboxesMetadata.map(list -> retrieveParentFromMetadata(parent, list))
            .orElse(retrieveParentFromBackend(mailboxSession, parent));
    }

    private Optional<MailboxId> retrieveParentFromBackend(MailboxSession mailboxSession, MailboxPath parent) throws MailboxException {
        return Optional.of(
            mailboxManager.getMailbox(parent, mailboxSession)
                .getId());
    }

    private Optional<MailboxId> retrieveParentFromMetadata(MailboxPath parent, List<MailboxMetaData> list) {
        return list.stream()
            .filter(metadata -> metadata.getPath().equals(parent))
            .map(MailboxMetaData::getId)
            .findAny();
    }
}