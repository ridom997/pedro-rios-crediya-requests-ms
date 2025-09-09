package co.com.pedrorido.r2dbc;

import co.com.pedrorido.model.status.Status;
import co.com.pedrorido.model.status.gateways.StatusRepository;
import co.com.pedrorido.r2dbc.entity.StatusEntity;
import co.com.pedrorido.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.log4j.Log4j2;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Log4j2
@Repository
public class StatusReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Status,
        StatusEntity,
        Long,
        StatusReactiveRepository
        > implements StatusRepository {
    public StatusReactiveRepositoryAdapter(StatusReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Status.class));
    }


}
