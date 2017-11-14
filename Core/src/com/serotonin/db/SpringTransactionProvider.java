/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: http://www.jooq.org/licenses
 */

package com.serotonin.db;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED;

import org.jooq.Transaction;
import org.jooq.TransactionContext;
import org.jooq.TransactionProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Makes jOOQ use Spring transactions. Calling create.transaction() will create a Spring transaction.
 * 
 * @author Lukas Eder
 */
public class SpringTransactionProvider implements TransactionProvider {

    private final PlatformTransactionManager txMgr;
    
    public SpringTransactionProvider(PlatformTransactionManager txMgr) {
        this.txMgr = txMgr;
    }

    @Override
    public void begin(TransactionContext ctx) {
        // This TransactionProvider behaves like jOOQ's DefaultTransactionProvider,
        // which supports nested transactions using Savepoints
        TransactionStatus tx = txMgr.getTransaction(new DefaultTransactionDefinition(PROPAGATION_NESTED));
        ctx.transaction(new SpringTransaction(tx));
    }

    @Override
    public void commit(TransactionContext ctx) {
        txMgr.commit(((SpringTransaction) ctx.transaction()).tx);
    }

    @Override
    public void rollback(TransactionContext ctx) {
        txMgr.rollback(((SpringTransaction) ctx.transaction()).tx);
    }
    

    private static class SpringTransaction implements Transaction {
        final TransactionStatus tx;
    
        SpringTransaction(TransactionStatus tx) {
            this.tx = tx;
        }
    }
}
