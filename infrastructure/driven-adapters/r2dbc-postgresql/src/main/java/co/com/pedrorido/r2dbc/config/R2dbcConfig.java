package co.com.pedrorido.r2dbc.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Log4j2
@Configuration
public class R2dbcConfig {
    @Bean
    public R2dbcTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        log.info("Creating R2dbcTransactionManager");
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public TransactionalOperator transactionalOperator(R2dbcTransactionManager tm) {
        log.info("Creating TransactionalOperator");
        //Con esto, cualquier Publisher envuelto con el TransactionalOperator se ejecuta dentro de una transacción.
        return TransactionalOperator.create(tm);
    }
}
