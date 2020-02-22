package ru.portfolio.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.SecurityEntityConverter;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.repository.SecurityRepository;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
public class SecurityRestController extends AbstractController<String, Security, SecurityEntity> {
    private final SecurityRepository repository;

    public SecurityRestController(SecurityRepository repository, SecurityEntityConverter converter) {
        super(repository, converter);
        this.repository = repository;
    }

    @GetMapping("/securities")
    @Override
    public List<SecurityEntity> get() {
        return super.get();
    }

    @GetMapping("/securities/{isin}")
    @Override
    public ResponseEntity<SecurityEntity> get(@PathVariable("isin") String isin) {
        return super.get(isin);
    }

    @PostMapping("/securities")
    @Override
    public ResponseEntity<SecurityEntity> post(@Valid @RequestBody Security security) throws URISyntaxException {
        return super.post(security);
    }

    @PutMapping("/securities/{isin}")
    @Override
    public ResponseEntity<SecurityEntity> put(@PathVariable("isin") String isin, @Valid @RequestBody Security security) throws URISyntaxException {
        return super.put(isin, security);
    }

    @DeleteMapping("/securities/{isin}")
    @Override
    public void delete(@PathVariable("isin") String isin) {
        super.delete(isin);
    }

    @Override
    protected Optional<SecurityEntity> getById(String isin) {
        return repository.findByIsin(isin);
    }

    @Override
    protected String getId(Security object) {
        return object.getIsin();
    }

    @Override
    protected Security updateId(String isin, Security object) {
        return object.toBuilder().isin(isin).build();
    }

    @Override
    protected String getLocation() {
        return "/securities";
    }
}
